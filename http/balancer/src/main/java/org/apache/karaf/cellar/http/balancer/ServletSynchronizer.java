/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.http.balancer;

import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.features.BootFinished;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.*;

public class ServletSynchronizer implements Synchronizer {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServletSynchronizer.class);

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private ConfigurationAdmin configurationAdmin;
    private ProxyServletRegistry proxyRegistry;
    private BundleContext bundleContext;
    private EventProducer eventProducer;

    public void init(BundleContext bundleContext) {
        // wait the end of Karaf boot process
        ServiceTracker tracker = new ServiceTracker(bundleContext, BootFinished.class, null);
        try {
            tracker.waitForService(120000);
        } catch (Exception e) {
            LOGGER.warn("Can't start BootFinished service tracker", e);
        }
        if (groupManager == null)
            return;
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                sync(group);
            }
        }
    }

    @Override
    public void sync(Group group) {
        String policy = getSyncPolicy(group);
        if (policy == null) {
            LOGGER.warn("CELLAR HTTP BALANCER: sync policy is not defined for cluster group {}", group.getName());
        } else if (policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR HTTP BALANCER: sync policy set as 'cluster' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR HTTP BALANCER: updating node from the cluster (pull first)");
            pull(group);
            LOGGER.debug("CELLAR HTTP BALANCER: updating cluster from the local node (push after)");
            push(group);
        } else if (policy.equalsIgnoreCase("node")) {
            LOGGER.debug("CELLAR HTTP BALANCER: sync policy set as 'node' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR HTTP BALANCER: updating cluster from the local node (push first)");
            push(group);
            LOGGER.debug("CELLAR HTTP BALANCER: updating node from the cluster (pull after)");
            pull(group);
        } else if (policy.equalsIgnoreCase("clusterOnly")) {
            LOGGER.debug("CELLAR HTTP BALANCER: sync policy set as 'clusterOnly' for cluster group " + group.getName());
            LOGGER.debug("CELLAR HTTP BALANCER: updating node from the cluster (pull only)");
            pull(group);
        } else if (policy.equalsIgnoreCase("nodeOnly")) {
            LOGGER.debug("CELLAR HTTP BALANCER: sync policy set as 'nodeOnly' for cluster group " + group.getName());
            LOGGER.debug("CELLAR HTTP BALANCER: updating cluster from the local node (push only)");
            push(group);
        } else {
            LOGGER.debug("CELLAR HTTP BALANCER: sync policy set as 'disabled' for cluster group " + group.getName());
            LOGGER.debug("CELLAR HTTP BALANCER: no sync");
        }
    }

    @Override
    public void pull(Group group) {
        Map<String, List<String>> clusterServlets = clusterManager.getMap(Constants.BALANCER_MAP + Configurations.SEPARATOR + group.getName());
        for (String alias : clusterServlets.keySet()) {
            try {
                // add a proxy servlet only if the alias is not present locally
                Collection<ServiceReference<Servlet>> references = bundleContext.getServiceReferences(Servlet.class, "(alias=" + alias + ")");
                if (references.isEmpty()) {
                    LOGGER.debug("CELLAR HTTP BALANCER: create proxy servlet for {}", alias);
                    CellarBalancerProxyServlet proxyServlet = new CellarBalancerProxyServlet();
                    proxyServlet.setLocations(clusterServlets.get(alias));
                    proxyServlet.init();
                    Hashtable<String, String> properties = new Hashtable<String, String>();
                    properties.put("alias", alias);
                    properties.put("cellar.http.balancer.proxy", "true");
                    ServiceRegistration registration = bundleContext.registerService(Servlet.class, proxyServlet, properties);
                    proxyRegistry.register(alias, registration);
                }
            } catch (Exception e) {
                LOGGER.warn("CELLAR HTTP BALANCER: can't create proxy servlet for {}", alias, e);
            }
        }
    }

    @Override
    public void push(Group group) {

        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR HTTP BALANCER: cluster event producer is OFF");
            return;
        }

        Map<String, List<String>> clusterServlets = clusterManager.getMap(Constants.BALANCER_MAP + Configurations.SEPARATOR + group.getName());
        BalancedServletUtil util = new BalancedServletUtil();
        util.setClusterManager(clusterManager);
        util.setConfigurationAdmin(configurationAdmin);
        try {
            Collection<ServiceReference<Servlet>> references = bundleContext.getServiceReferences(Servlet.class, null);
            for (ServiceReference<Servlet> reference : references) {
                if (reference.getProperty("alias") != null) {
                    String alias = (String) reference.getProperty("alias");
                    String location = util.constructLocation(alias);
                    Servlet servlet = bundleContext.getService(reference);
                    if (servlet != null) {
                        if (!(servlet instanceof CellarBalancerProxyServlet)) {
                            // update the cluster servlets
                            List<String> locations = clusterServlets.get(alias);
                            if (locations == null) {
                                locations = new ArrayList<String>();
                            }

                            if (!locations.contains(location)) {
                                LOGGER.debug("CELLAR HTTP BALANCER: adding location {} to servlet {} on cluster", location, alias);
                                locations.add(location);
                                clusterServlets.put(alias, locations);
                                // send cluster event
                                ClusterBalancerEvent event = new ClusterBalancerEvent(alias, ClusterBalancerEvent.ADDING, locations);
                                event.setSourceGroup(group);
                                event.setSourceNode(clusterManager.getNode());
                                event.setLocal(clusterManager.getNode());
                                eventProducer.produce(event);
                            } else {
                                LOGGER.debug("CELLAR HTTP BALANCER: location {} already defined for servlet {} on cluster", location, alias);
                            }
                        }
                    }
                } else {
                    LOGGER.warn("CELLAR HTTP BALANCER: alias property is not defined");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR HTTP BALANCER: can't push servlet on cluster", e);
        }
    }

    /**
     * Get the balanced servlet sync policy for the given cluster group.
     *
     * @param group the cluster group.
     * @return the current features sync policy for the given cluster group.
     */
    @Override
    public String getSyncPolicy(Group group) {
        String groupName = group.getName();
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                return properties.get(propertyKey).toString();
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HTTP BALANCER: error while retrieving the sync policy", e);
        }

        return null;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setProxyRegistry(ProxyServletRegistry proxyRegistry) {
        this.proxyRegistry = proxyRegistry;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
