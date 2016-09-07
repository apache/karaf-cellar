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
package org.apache.karaf.cellar.obr.internal.osgi;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.obr.ObrBundleEventHandler;
import org.apache.karaf.cellar.obr.ObrUrlEventHandler;
import org.apache.karaf.cellar.obr.ObrUrlSynchronizer;
import org.apache.karaf.cellar.obr.management.CellarOBRMBean;
import org.apache.karaf.cellar.obr.management.internal.CellarOBRMBeanImpl;
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
                @ProvideService(CellarOBRMBean.class)
        },
        requires = {
                @RequireService(RepositoryAdmin.class),
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(EventProducer.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ObrUrlEventHandler urlEventHandler;
    private ObrBundleEventHandler bundleEventHandler;
    private ObrUrlSynchronizer urlSynchronizer;
    private ServiceRegistration mbeanRegistration;

    @Override
    public void doStart() throws Exception {

        RepositoryAdmin repositoryAdmin = getTrackedService(RepositoryAdmin.class);
        if (repositoryAdmin == null)
            return;
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

        LOGGER.debug("CELLAR OBR: init URL event handler");
        urlEventHandler = new ObrUrlEventHandler();
        urlEventHandler.setClusterManager(clusterManager);
        urlEventHandler.setGroupManager(groupManager);
        urlEventHandler.setConfigurationAdmin(configurationAdmin);
        urlEventHandler.setObrService(repositoryAdmin);
        urlEventHandler.init(bundleContext);
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(EventHandler.class, urlEventHandler, props);

        LOGGER.debug("CELLAR OBR: init bundle event handler");
        bundleEventHandler = new ObrBundleEventHandler();
        bundleEventHandler.setObrService(repositoryAdmin);
        bundleEventHandler.setClusterManager(clusterManager);
        bundleEventHandler.setGroupManager(groupManager);
        bundleEventHandler.setConfigurationAdmin(configurationAdmin);
        bundleEventHandler.init(bundleContext);
        register(EventHandler.class, bundleEventHandler, props);

        LOGGER.debug("CELLAR OBR: init URL synchronizer");
        urlSynchronizer = new ObrUrlSynchronizer();
        urlSynchronizer.setObrService(repositoryAdmin);
        urlSynchronizer.setClusterManager(clusterManager);
        urlSynchronizer.setGroupManager(groupManager);
        urlSynchronizer.setEventProducer(eventProducer);
        urlSynchronizer.setConfigurationAdmin(configurationAdmin);
        urlSynchronizer.init(bundleContext);
        props = new Hashtable();
        props.put("resource", "obr.urls");
        register(Synchronizer.class, urlSynchronizer, props);

        LOGGER.debug("CELLAR OBR: register MBean");
        CellarOBRMBeanImpl mbean = new CellarOBRMBeanImpl();
        mbean.setClusterManager(clusterManager);
        mbean.setGroupManager(groupManager);
        mbean.setConfigurationAdmin(configurationAdmin);
        mbean.setEventProducer(eventProducer);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=obr,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = bundleContext.registerService(getInterfaceNames(mbean), mbean, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
        if (urlSynchronizer != null) {
            urlSynchronizer.destroy();
            urlEventHandler = null;
        }
        if (bundleEventHandler != null) {
            bundleEventHandler.destroy();
            bundleEventHandler = null;
        }
        if (urlEventHandler != null) {
            urlEventHandler.destroy();
            urlEventHandler = null;
        }
    }

}
