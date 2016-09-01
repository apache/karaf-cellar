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
package org.apache.karaf.cellar.dosgi.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.command.CommandStore;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.dosgi.*;
import org.apache.karaf.cellar.dosgi.management.ServiceMBean;
import org.apache.karaf.cellar.dosgi.management.internal.ServiceMBeanImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(ListenerHook.class),
                @ProvideService(EventHandler.class),
                @ProvideService(ServiceMBean.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(EventTransportFactory.class),
                @RequireService(CommandStore.class),
                @RequireService(ConfigurationAdmin.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ImportServiceListener importServiceListener;
    private ExportServiceListener exportServiceListener;
    private RemovedNodeServiceTracker removedNodeServiceTracker;
    private ServiceRegistration mbeanRegistration;

    @Override
    public void doStart() throws Exception {

        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null)
            return;
        EventTransportFactory eventTransportFactory = getTrackedService(EventTransportFactory.class);
        if (eventTransportFactory == null)
            return;
        CommandStore commandStore = getTrackedService(CommandStore.class);
        if (commandStore == null)
            return;
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null)
            return;

        LOGGER.debug("CELLAR DOSGI: init remote service call handler");
        RemoteServiceCallHandler remoteServiceCallHandler = new RemoteServiceCallHandler();
        remoteServiceCallHandler.setEventTransportFactory(eventTransportFactory);
        remoteServiceCallHandler.setClusterManager(clusterManager);
        remoteServiceCallHandler.setBundleContext(bundleContext);
        remoteServiceCallHandler.setConfigurationAdmin(configurationAdmin);
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(EventHandler.class, remoteServiceCallHandler, props);

        LOGGER.debug("CELLAR DOSGI: init remote service result handler");
        RemoteServiceResultHandler remoteServiceResultHandler = new RemoteServiceResultHandler();
        remoteServiceResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, remoteServiceResultHandler);

        LOGGER.debug("CELLAR DOSGI: init import service listener");
        importServiceListener = new ImportServiceListener();
        importServiceListener.setClusterManager(clusterManager);
        importServiceListener.setEventTransportFactory(eventTransportFactory);
        importServiceListener.setCommandStore(commandStore);
        importServiceListener.setBundleContext(bundleContext);
        importServiceListener.init();
        register(ListenerHook.class, importServiceListener);

        LOGGER.debug("CELLAR DOSGI: init export service listener");
        exportServiceListener = new ExportServiceListener();
        exportServiceListener.setClusterManager(clusterManager);
        exportServiceListener.setEventTransportFactory(eventTransportFactory);
        exportServiceListener.setBundleContext(bundleContext);
        exportServiceListener.init();

        LOGGER.debug("CELLAR DOSGI: start removed nodes service tracker");
        removedNodeServiceTracker = new RemovedNodeServiceTracker();
        removedNodeServiceTracker.setClusterManager(clusterManager);
        removedNodeServiceTracker.init();

        LOGGER.debug("CELLAR DOSGI: register MBean");
        ServiceMBeanImpl mbean = new ServiceMBeanImpl();
        mbean.setClusterManager(clusterManager);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=service,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = bundleContext.registerService(getInterfaceNames(mbean), mbean, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }

        if (removedNodeServiceTracker != null) {
            removedNodeServiceTracker.destroy();
            removedNodeServiceTracker = null;
        }

        if (exportServiceListener != null) {
            exportServiceListener.destroy();
            exportServiceListener = null;
        }
        if (importServiceListener != null) {
            importServiceListener.destroy();
            importServiceListener = null;
        }
    }

}
