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
package org.apache.karaf.cellar.config.internal.osgi;

import org.apache.karaf.cellar.config.ConfigurationEventHandler;
import org.apache.karaf.cellar.config.ConfigurationSynchronizer;
import org.apache.karaf.cellar.config.LocalConfigurationListener;
import org.apache.karaf.cellar.config.management.CellarConfigMBean;
import org.apache.karaf.cellar.config.management.internal.CellarConfigMBeanImpl;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Hashtable;

@Services(
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(EventProducer.class)
        },
        provides = {
                @ProvideService(ConfigurationListener.class),
                @ProvideService(Synchronizer.class),
                @ProvideService(EventHandler.class),
                @ProvideService(CellarConfigMBean.class)
        }
)
@Managed("org.apache.karaf.shell.config")
public class Activator extends BaseActivator implements ManagedService {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private LocalConfigurationListener localConfigurationListener;
    private ConfigurationSynchronizer configurationSynchronizer;
    private ConfigurationEventHandler configurationEventHandler;
    private ServiceRegistration cellarConfigMBeanRegistration;

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

        File storage = new File(getString("storage", System.getProperty("karaf.etc")));

        LOGGER.debug("CELLAR CONFIG: init event handler");
        configurationEventHandler = new ConfigurationEventHandler();
        configurationEventHandler.setConfigurationAdmin(configurationAdmin);
        configurationEventHandler.setGroupManager(groupManager);
        configurationEventHandler.setClusterManager(clusterManager);
        configurationEventHandler.setStorage(storage);
        configurationEventHandler.init();
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(EventHandler.class, configurationEventHandler, props);

        LOGGER.debug("CELLAR CONFIG: init local listener");
        localConfigurationListener = new LocalConfigurationListener();
        localConfigurationListener.setClusterManager(clusterManager);
        localConfigurationListener.setGroupManager(groupManager);
        localConfigurationListener.setConfigurationAdmin(configurationAdmin);
        localConfigurationListener.setEventProducer(eventProducer);
        localConfigurationListener.setStorage(storage);
        localConfigurationListener.init();
        register(ConfigurationListener.class, localConfigurationListener);

        LOGGER.debug("CELLAR CONFIG: init synchronizer");
        configurationSynchronizer = new ConfigurationSynchronizer();
        configurationSynchronizer.setConfigurationAdmin(configurationAdmin);
        configurationSynchronizer.setGroupManager(groupManager);
        configurationSynchronizer.setClusterManager(clusterManager);
        configurationSynchronizer.setEventProducer(eventProducer);
        configurationSynchronizer.setStorage(storage);
        configurationSynchronizer.init(bundleContext);
        props = new Hashtable();
        props.put("resource", "config");
        register(Synchronizer.class, configurationSynchronizer, props);

        LOGGER.debug("CELLAR CONFIG: register MBean");
        CellarConfigMBeanImpl cellarConfigMBean = new CellarConfigMBeanImpl();
        cellarConfigMBean.setClusterManager(clusterManager);
        cellarConfigMBean.setGroupManager(groupManager);
        cellarConfigMBean.setConfigurationAdmin(configurationAdmin);
        cellarConfigMBean.setEventProducer(eventProducer);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=config,name=" + System.getProperty("karaf.name"));
        cellarConfigMBeanRegistration = bundleContext.registerService(getInterfaceNames(cellarConfigMBean), cellarConfigMBean, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (cellarConfigMBeanRegistration != null) {
            cellarConfigMBeanRegistration.unregister();
            cellarConfigMBeanRegistration = null;
        }
        if (configurationSynchronizer != null) {
            configurationSynchronizer.destroy();
            configurationSynchronizer = null;
        }
        if (localConfigurationListener != null) {
            localConfigurationListener.destroy();
            localConfigurationListener = null;
        }
        if (configurationEventHandler != null) {
            configurationEventHandler.destroy();
            configurationEventHandler = null;
        }
    }

}
