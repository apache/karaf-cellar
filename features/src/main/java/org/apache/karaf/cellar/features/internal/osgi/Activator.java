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
package org.apache.karaf.cellar.features.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.features.FeaturesEventHandler;
import org.apache.karaf.cellar.features.FeaturesSynchronizer;
import org.apache.karaf.cellar.features.LocalFeaturesListener;
import org.apache.karaf.cellar.features.RepositoryEventHandler;
import org.apache.karaf.cellar.features.management.CellarFeaturesMBean;
import org.apache.karaf.cellar.features.management.internal.CellarFeaturesMBeanImpl;
import org.apache.karaf.features.FeaturesListener;
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
                @ProvideService(FeaturesListener.class),
                @ProvideService(Synchronizer.class),
                @ProvideService(EventHandler.class),
                @ProvideService(CellarFeaturesMBean.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class),
                @RequireService(EventProducer.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(FeaturesService.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private LocalFeaturesListener localFeaturesListener;
    private FeaturesSynchronizer featuresSynchronizer;
    private FeaturesEventHandler featuresEventHandler;
    private RepositoryEventHandler repositoryEventHandler;
    private ServiceRegistration mbeanRegistration;

    @Override
    public void doStart() throws Exception {

        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null)
            return;
        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null)
            return;
        GroupManager groupManager = getTrackedService(GroupManager.class);
        if (groupManager == null)
            return;
        EventProducer eventProducer = getTrackedService(EventProducer.class);
        if (eventProducer == null)
            return;
        FeaturesService featuresService = getTrackedService(FeaturesService.class);
        if (featuresService == null)
            return;

        LOGGER.debug("CELLAR FEATURE: init repository event handler");
        repositoryEventHandler = new RepositoryEventHandler();
        repositoryEventHandler.setConfigurationAdmin(configurationAdmin);
        repositoryEventHandler.setFeaturesService(featuresService);
        repositoryEventHandler.setClusterManager(clusterManager);
        repositoryEventHandler.setGroupManager(groupManager);
        repositoryEventHandler.init(bundleContext);
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(new Class[]{ EventHandler.class }, repositoryEventHandler, props);

        LOGGER.debug("CELLAR FEATURE: init features event handler");
        featuresEventHandler = new FeaturesEventHandler();
        featuresEventHandler.setFeaturesService(featuresService);
        featuresEventHandler.setClusterManager(clusterManager);
        featuresEventHandler.setGroupManager(groupManager);
        featuresEventHandler.setConfigurationAdmin(configurationAdmin);
        featuresEventHandler.init(bundleContext);
        register(new Class[]{ EventHandler.class }, featuresEventHandler, props);

        LOGGER.debug("CELLAR FEATURE: init local features listener");
        localFeaturesListener = new LocalFeaturesListener();
        localFeaturesListener.setClusterManager(clusterManager);
        localFeaturesListener.setGroupManager(groupManager);
        localFeaturesListener.setEventProducer(eventProducer);
        localFeaturesListener.setConfigurationAdmin(configurationAdmin);
        localFeaturesListener.setFeaturesService(featuresService);
        localFeaturesListener.init(bundleContext);
        register(FeaturesListener.class, localFeaturesListener);

        LOGGER.debug("CELLAR FEATURE: init features synchronizer");
        featuresSynchronizer = new FeaturesSynchronizer();
        featuresSynchronizer.setClusterManager(clusterManager);
        featuresSynchronizer.setGroupManager(groupManager);
        featuresSynchronizer.setEventProducer(eventProducer);
        featuresSynchronizer.setConfigurationAdmin(configurationAdmin);
        featuresSynchronizer.setFeaturesService(featuresService);
        featuresSynchronizer.init(bundleContext);
        props = new Hashtable();
        props.put("resource", "feature");
        register(Synchronizer.class, featuresSynchronizer, props);

        LOGGER.debug("CELLAR FEATURE: register MBean");
        CellarFeaturesMBeanImpl mbean = new CellarFeaturesMBeanImpl();
        mbean.setClusterManager(clusterManager);
        mbean.setGroupManager(groupManager);
        mbean.setConfigurationAdmin(configurationAdmin);
        mbean.setFeaturesService(featuresService);
        mbean.setEventProducer(eventProducer);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=feature,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = bundleContext.registerService(getInterfaceNames(mbean), mbean, props);

    }

    @Override
    public void doStop() {
        super.doStop();

        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
        if (featuresSynchronizer != null) {
            featuresSynchronizer.destroy();
            featuresSynchronizer = null;
        }
        if (localFeaturesListener != null) {
            localFeaturesListener.destroy();
            localFeaturesListener = null;
        }
        if (featuresEventHandler != null) {
            featuresEventHandler.destroy();
            featuresEventHandler = null;
        }
        if (repositoryEventHandler != null) {
            repositoryEventHandler.destroy();
            repositoryEventHandler = null;
        }
    }

}
