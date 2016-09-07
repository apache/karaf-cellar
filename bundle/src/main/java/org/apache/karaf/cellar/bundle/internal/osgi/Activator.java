/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.bundle.internal.osgi;

import org.apache.karaf.cellar.bundle.BundleEventHandler;
import org.apache.karaf.cellar.bundle.BundleSynchronizer;
import org.apache.karaf.cellar.bundle.LocalBundleListener;
import org.apache.karaf.cellar.bundle.management.CellarBundleMBean;
import org.apache.karaf.cellar.bundle.management.internal.CellarBundleMBeanImpl;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(EventHandler.class),
                @ProvideService(Synchronizer.class),
                @ProvideService(CellarBundleMBean.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(EventProducer.class),
                @RequireService(FeaturesService.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private LocalBundleListener localBundleListener;
    private BundleSynchronizer synchronizer;
    private BundleEventHandler eventHandler;
    private ServiceRegistration mbeanRegistration;

    @Override
    public void doStart() throws Exception {

        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null)
            return;
        GroupManager groupManager = getTrackedService(GroupManager.class);
        if (groupManager == null)
            return;
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null)
            return;
        EventProducer eventProducer = getTrackedService(EventProducer.class);
        if (eventProducer == null)
            return;
        FeaturesService featuresService = getTrackedService(FeaturesService.class);
        if (featuresService == null)
            return;

        LOGGER.debug("CELLAR BUNDLE: init even handler");
        eventHandler = new BundleEventHandler();
        eventHandler.setConfigurationAdmin(configurationAdmin);
        eventHandler.setClusterManager(clusterManager);
        eventHandler.setGroupManager(groupManager);
        eventHandler.setBundleContext(bundleContext);
        eventHandler.setFeaturesService(featuresService);
        eventHandler.init();
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(EventHandler.class, eventHandler, props);

        LOGGER.debug("CELLAR BUNDLE: init local listener");
        localBundleListener = new LocalBundleListener();
        localBundleListener.setClusterManager(clusterManager);
        localBundleListener.setGroupManager(groupManager);
        localBundleListener.setConfigurationAdmin(configurationAdmin);
        localBundleListener.setEventProducer(eventProducer);
        localBundleListener.setFeaturesService(featuresService);
        localBundleListener.setBundleContext(bundleContext);
        localBundleListener.init();

        LOGGER.debug("CELLAR BUNDLE: init synchronizer");
        synchronizer = new BundleSynchronizer();
        synchronizer.setConfigurationAdmin(configurationAdmin);
        synchronizer.setGroupManager(groupManager);
        synchronizer.setClusterManager(clusterManager);
        synchronizer.setBundleContext(bundleContext);
        synchronizer.setEventProducer(eventProducer);
        synchronizer.init(bundleContext);
        props = new Hashtable();
        props.put("resource", "bundle");
        register(Synchronizer.class, synchronizer, props);

        LOGGER.debug("CELLAR BUNDLE: register MBean");
        CellarBundleMBeanImpl mbean = new CellarBundleMBeanImpl();
        mbean.setClusterManager(clusterManager);
        mbean.setConfigurationAdmin(configurationAdmin);
        mbean.setGroupManager(groupManager);
        mbean.setEventProducer(eventProducer);
        mbean.setBundleContext(bundleContext);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=bundle,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = bundleContext.registerService(getInterfaceNames(mbean), mbean, props);

    }

    @Override
    public void doStop() {
        super.doStop();

        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
        if (synchronizer != null) {
            synchronizer.destroy();
            synchronizer = null;
        }
        if (localBundleListener != null) {
            localBundleListener.destroy();
            localBundleListener = null;
        }
        if (eventHandler != null) {
            eventHandler.destroy();
            eventHandler = null;
        }
    }

}
