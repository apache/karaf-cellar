/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.http.balancer;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Hashtable;

public class BalancerEventHandler implements EventHandler<ClusterBalancerEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BalancerEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.http.balancer.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private ConfigurationAdmin configurationAdmin;
    private BundleContext bundleContext;
    private ProxyServletRegistry proxyRegistry;

    @Override
    public void handle(ClusterBalancerEvent event) {
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR HTTP BALANCER: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        if (groupManager == null) {
            //in rare cases for example right after installation this happens!
            LOGGER.error("CELLAR HTTP BALANCER: retrieved event {} while groupManager is not available yet!", event);
            return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR HTTP BALANCER: node is not part of the event cluster group {}", event.getSourceGroup().getName());
            return;
        }

        // check if it's not a "local" event
        if (event.getLocal() != null && event.getLocal().getId().equalsIgnoreCase(clusterManager.getNode().getId())) {
            LOGGER.trace("CELLAR HTTP BALANCER: cluster event is local (coming from local synchronizer or listener)");
            return;
        }

        String alias = event.getAlias();
        if (event.getType() == ClusterBalancerEvent.ADDING) {
            LOGGER.debug("CELLAR HTTP BALANCER: creating proxy servlet for {}", alias);
            CellarBalancerProxyServlet cellarBalancerProxyServlet = new CellarBalancerProxyServlet();
            cellarBalancerProxyServlet.setLocations(event.getLocations());
            try {
                cellarBalancerProxyServlet.init();
                Hashtable<String, String> properties = new Hashtable<String, String>();
                properties.put("alias", alias);
                properties.put("cellar.http.balancer.proxy", "true");
                ServiceRegistration registration = bundleContext.registerService(Servlet.class, cellarBalancerProxyServlet, properties);
                proxyRegistry.register(alias, registration);
            } catch (Exception e) {
                LOGGER.error("CELLAR HTTP BALANCER: can't start proxy servlet", e);
            }
        } else if (event.getType() == ClusterBalancerEvent.REMOVING) {
            if (proxyRegistry.contain(alias)) {
                LOGGER.debug("CELLAR HTTP BALANCER: removing proxy servlet for {}", alias);
                proxyRegistry.unregister(alias);
            }
        }
    }

    @Override
    public Class<ClusterBalancerEvent> getType() {
        return ClusterBalancerEvent.class;
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE, null);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.HANDLER + "." + this.getClass().getName()));
                if (status) {
                    eventSwitch.turnOn();
                } else {
                    eventSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return eventSwitch;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setProxyRegistry(ProxyServletRegistry proxyRegistry) {
        this.proxyRegistry = proxyRegistry;
    }

}
