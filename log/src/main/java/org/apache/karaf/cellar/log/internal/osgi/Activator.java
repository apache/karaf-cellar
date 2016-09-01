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
package org.apache.karaf.cellar.log.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.command.CommandStore;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.log.*;
import org.apache.karaf.cellar.log.management.CellarLogMBean;
import org.apache.karaf.cellar.log.management.internal.CellarLogMBeanImpl;
import org.apache.karaf.log.core.LogService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(PaxAppender.class),
                @ProvideService(EventHandler.class),
                @ProvideService(CellarLogMBean.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(EventProducer.class),
                @RequireService(CommandStore.class),
                @RequireService(ExecutionContext.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(LogService.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ServiceRegistration mbeanRegistration;

    @Override
    public void doStart() throws Exception {
        LOGGER.debug("CELLAR LOG: retrieve cluster manager service");
        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null) {
            return;
        }

        LOGGER.debug("CELLAR LOG: retrieve producer service");
        EventProducer producer = getTrackedService(EventProducer.class);
        if (producer == null) {
            return;
        }

        LOGGER.debug("CELLAR LOG: retrieve command store service");
        CommandStore commandStore = getTrackedService(CommandStore.class);
        if (commandStore == null) {
            return;
        }

        LOGGER.debug("CELLAR LOG: retrieve execution context service");
        ExecutionContext executionContext = getTrackedService(ExecutionContext.class);
        if (executionContext == null) {
            return;
        }

        LOGGER.debug("CELLAR LOG: retrieve configuration admin service");
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }

        LOGGER.debug("CELLAR LOG: retrieve log service");
        LogService logService = getTrackedService(LogService.class);
        if (logService == null) {
            return;
        }

        LOGGER.debug("CELLAR LOG: init PaxAppender");
        LogAppender paxAppender = new LogAppender();
        paxAppender.setClusterManager(clusterManager);
        Hashtable props = new Hashtable();
        props.put("org.ops4j.pax.logging.appender.name", "CellarLogAppender");
        register(PaxAppender.class, paxAppender, props);

        LOGGER.debug("CELLAR LOG: register get log command handler");
        GetLogCommandHandler getLogCommandHandler = new GetLogCommandHandler();
        getLogCommandHandler.setProducer(producer);
        getLogCommandHandler.setConfigurationAdmin(configurationAdmin);
        getLogCommandHandler.setLogService(logService);
        register(EventHandler.class, getLogCommandHandler);

        LOGGER.debug("CELLAR LOG: register get log result handler");
        GetLogResultHandler getLogResultHandler = new GetLogResultHandler();
        getLogResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, getLogResultHandler);

        LOGGER.debug("CELLAR LOG: register set log command handler");
        SetLogCommandHandler setLogCommandHandler = new SetLogCommandHandler();
        setLogCommandHandler.setProducer(producer);
        setLogCommandHandler.setConfigurationAdmin(configurationAdmin);
        setLogCommandHandler.setLogService(logService);
        register(EventHandler.class, setLogCommandHandler);

        LOGGER.debug("CELLAR LOG: register set log result handler");
        SetLogResultHandler setLogResultHandler = new SetLogResultHandler();
        setLogResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, setLogResultHandler);

        LOGGER.debug("CELLAR LOG: register MBean");
        CellarLogMBeanImpl mbean = new CellarLogMBeanImpl();
        mbean.setClusterManager(clusterManager);
        mbean.setExecutionContext(executionContext);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=log,name=" + System.getProperty("karaf.name"));
        mbeanRegistration = bundleContext.registerService(getInterfaceNames(mbean), mbean, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
    }

}
