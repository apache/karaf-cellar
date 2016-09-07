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
package org.apache.karaf.cellar.http.balancer.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.http.balancer.BalancerEventHandler;
import org.apache.karaf.cellar.http.balancer.LocalServletListener;
import org.apache.karaf.cellar.http.balancer.ProxyServletRegistry;
import org.apache.karaf.cellar.http.balancer.ServletSynchronizer;
import org.apache.karaf.cellar.http.balancer.management.CellarHttpMBean;
import org.apache.karaf.cellar.http.balancer.management.internal.CellarHttpMBeanImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.ops4j.pax.web.service.spi.ServletListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(ServletListener.class),
                @ProvideService(EventHandler.class),
                @ProvideService(Synchronizer.class),
                @ProvideService(CellarHttpMBean.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(EventProducer.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ProxyServletRegistry proxyRegistry;

    private ServiceRegistration mbeanRegistration;

    @Override
    public void doStart() throws Exception {
        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null) {
            return;
        }
        GroupManager groupManager = getTrackedService(GroupManager.class);
        if (groupManager == null) {
            return;
        }
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }
        EventProducer eventProducer = getTrackedService(EventProducer.class);
        if (eventProducer == null) {
            return;
        }

        LOGGER.debug("CELLAR HTTP BALANCER: starting proxy registry");
        proxyRegistry = new ProxyServletRegistry();
        proxyRegistry.init();

        LOGGER.debug("CELLAR HTTP BALANCER: starting balancer event handler");
        BalancerEventHandler balancerEventHandler = new BalancerEventHandler();
        balancerEventHandler.setClusterManager(clusterManager);
        balancerEventHandler.setBundleContext(bundleContext);
        balancerEventHandler.setConfigurationAdmin(configurationAdmin);
        balancerEventHandler.setGroupManager(groupManager);
        balancerEventHandler.setProxyRegistry(proxyRegistry);
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(EventHandler.class, balancerEventHandler, props);

        LOGGER.debug("CELLAR HTTP BALANCER: starting servlet synchronizer");
        ServletSynchronizer synchronizer = new ServletSynchronizer();
        synchronizer.setEventProducer(eventProducer);
        synchronizer.setClusterManager(clusterManager);
        synchronizer.setConfigurationAdmin(configurationAdmin);
        synchronizer.setProxyRegistry(proxyRegistry);
        synchronizer.setGroupManager(groupManager);
        synchronizer.setBundleContext(bundleContext);
        synchronizer.init(bundleContext);
        props = new Hashtable();
        props.put("resource", "balanced.servlet");
        register(Synchronizer.class, synchronizer, props);

        LOGGER.debug("CELLAR HTTP BALANCER: starting local servlet listener");
        LocalServletListener servletListener = new LocalServletListener();
        servletListener.setClusterManager(clusterManager);
        servletListener.setGroupManager(groupManager);
        servletListener.setConfigurationAdmin(configurationAdmin);
        servletListener.setEventProducer(eventProducer);
        register(ServletListener.class, servletListener);

        LOGGER.debug("CELLAR HTTP BALANCER: register MBean");
        CellarHttpMBeanImpl mbean = new CellarHttpMBeanImpl();
        mbean.setClusterManager(clusterManager);
        mbean.setGroupManager(groupManager);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=http,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = bundleContext.registerService(getInterfaceNames(mbean), mbean, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (proxyRegistry != null) {
            proxyRegistry.destroy();
            proxyRegistry = null;
        }

        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
    }

}
