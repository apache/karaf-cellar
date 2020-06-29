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
package org.apache.karaf.cellar.hazelcast.internal.osgi;

import com.hazelcast.core.HazelcastInstance;
import org.apache.aries.proxy.ProxyManager;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.command.BasicCommandStore;
import org.apache.karaf.cellar.core.command.ClusteredExecutionContext;
import org.apache.karaf.cellar.core.command.CommandStore;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.control.*;
import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.apache.karaf.cellar.core.discovery.DiscoveryTask;
import org.apache.karaf.cellar.core.event.*;
import org.apache.karaf.cellar.core.management.CellarGroupMBean;
import org.apache.karaf.cellar.core.management.CellarMBean;
import org.apache.karaf.cellar.core.management.CellarNodeMBean;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.apache.karaf.cellar.hazelcast.*;
import org.apache.karaf.cellar.hazelcast.factory.HazelcastConfigurationManager;
import org.apache.karaf.cellar.hazelcast.factory.HazelcastServiceFactory;
import org.apache.karaf.cellar.hazelcast.management.internal.CellarGroupMBeanImpl;
import org.apache.karaf.cellar.hazelcast.management.internal.CellarMBeanImpl;
import org.apache.karaf.cellar.hazelcast.management.internal.CellarNodeMBeanImpl;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Services(
        provides = {
                @ProvideService(HazelcastInstance.class),
                @ProvideService(ClusterManager.class),
                @ProvideService(GroupManager.class),
                @ProvideService(EventTransportFactory.class),
                @ProvideService(EventProducer.class),
                @ProvideService(ExecutionContext.class),
                @ProvideService(EventHandler.class),
                @ProvideService(CommandStore.class),
                @ProvideService(CellarMBean.class),
                @ProvideService(CellarNodeMBean.class),
                @ProvideService(CellarGroupMBean.class)
        },
        requires = {
                @RequireService(ConfigurationAdmin.class),
                @RequireService(ProxyManager.class),
                @RequireService(EventHandlerRegistry.class)
        }
)
@Managed("org.apache.karaf.cellar.discovery")
public class Activator extends BaseActivator implements ManagedService {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private CombinedClassLoader combinedClassLoader;
    private HazelcastServiceFactory hazelcastServiceFactory;
    private List<DiscoveryService> discoveryServices = new ArrayList<DiscoveryService>();
    private List<Synchronizer> synchronizers = new ArrayList<Synchronizer>();
    private HazelcastInstance hazelcastInstance;
    private HazelcastGroupManager groupManager;
    private DiscoveryTask discoveryTask;
    private CellarExtender extender;
    private TopicProducer producer;
    private TopicConsumer consumer;
    private ServiceTracker<DiscoveryService, DiscoveryService> discoveryServiceTracker;
    private ServiceTracker<Synchronizer, Synchronizer> synchronizerServiceTracker;

    private volatile ServiceRegistration coreMBeanRegistration;
    private volatile ServiceRegistration nodeMBeanRegistration;
    private volatile ServiceRegistration groupMBeanRegistration;

    private HashMap updatedConfig;

    private EventHandlerRegistryDispatcher dispatcher;

    @Override
    public void doStart() throws Exception {

        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null)
            return;
        EventHandlerRegistry eventHandlerRegistry = getTrackedService(EventHandlerRegistry.class);
        if (eventHandlerRegistry == null)
            return;
        ProxyManager proxyManager = getTrackedService(ProxyManager.class);
        if (proxyManager == null)
            return;

        LOGGER.debug("CELLAR HAZELCAST: init combined class loader");
        combinedClassLoader = new CombinedClassLoader();
        combinedClassLoader.init();

        LOGGER.debug("CELLAR HAZELCAST: start the discovery service tracker");
        discoveryServiceTracker = new ServiceTracker<DiscoveryService, DiscoveryService>(bundleContext, DiscoveryService.class, new ServiceTrackerCustomizer<DiscoveryService, DiscoveryService>() {
            @Override
            public DiscoveryService addingService(ServiceReference<DiscoveryService> serviceReference) {
                DiscoveryService service = bundleContext.getService(serviceReference);
                discoveryServices.add(service);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<DiscoveryService> serviceReference, DiscoveryService discoveryService) {
                // nothing to do
            }

            @Override
            public void removedService(ServiceReference<DiscoveryService> serviceReference, DiscoveryService discoveryService) {
                discoveryServices.remove(discoveryService);
                bundleContext.ungetService(serviceReference);
            }
        });
        discoveryServiceTracker.open();

        LOGGER.debug("CELLAR HAZELCAST: init Cellar extender");
        extender = new CellarExtender();
        extender.setCombinedClassLoader(combinedClassLoader);
        extender.setBundleContext(bundleContext);
        extender.init();

        LOGGER.debug("CELLAR HAZELCAST: init dispatcher");
        dispatcher = new EventHandlerRegistryDispatcher();
        dispatcher.setHandlerRegistry(eventHandlerRegistry);
        dispatcher.init();

        LOGGER.debug("CELLAR HAZELCAST: create Hazelcast configuration manager");
        HazelcastConfigurationManager hazelcastConfigurationManager = new HazelcastConfigurationManager();
        hazelcastConfigurationManager.setDiscoveryServices(discoveryServices);

        LOGGER.debug("CELLAR HAZELCAST: init Hazelcast service factory");
        hazelcastServiceFactory = new HazelcastServiceFactory();
        hazelcastServiceFactory.setCombinedClassLoader(combinedClassLoader);
        hazelcastServiceFactory.setConfigurationManager(hazelcastConfigurationManager);
        hazelcastServiceFactory.setBundleContext(bundleContext);
        hazelcastServiceFactory.init();
        if (updatedConfig != null) {
            // we have outstanding configuration update: do it now
            updated(updatedConfig);
            updatedConfig = null;
        }

        LOGGER.debug("CELLAR HAZELCAST: register Hazelcast instance");
        hazelcastInstance = hazelcastServiceFactory.getInstance();
        register(HazelcastInstance.class, hazelcastInstance);

        LOGGER.debug("CELLAR HAZELCAST: init discovery task");
        discoveryTask = new DiscoveryTask();
        discoveryTask.setDiscoveryServices(discoveryServices);
        discoveryTask.setConfigurationAdmin(configurationAdmin);
        discoveryTask.init();

        LOGGER.debug("CELLAR HAZELCAST: register Hazelcast cluster manager");
        HazelcastClusterManager clusterManager = new HazelcastClusterManager();
        clusterManager.setInstance(hazelcastInstance);
        clusterManager.setConfigurationAdmin(configurationAdmin);
        clusterManager.setCombinedClassLoader(combinedClassLoader);
        register(ClusterManager.class, clusterManager);

        LOGGER.debug("CELLAR HAZELCAST: create Hazelcast event transport factory");
        HazelcastEventTransportFactory eventTransportFactory = new HazelcastEventTransportFactory();
        eventTransportFactory.setCombinedClassLoader(combinedClassLoader);
        eventTransportFactory.setConfigurationAdmin(configurationAdmin);
        eventTransportFactory.setInstance(hazelcastInstance);
        eventTransportFactory.setDispatcher(dispatcher);
        register(EventTransportFactory.class, eventTransportFactory);

        LOGGER.debug("CELLAR HAZELCAST: init Hazelcast group manager");
        groupManager = new HazelcastGroupManager();
        groupManager.setInstance(hazelcastInstance);
        groupManager.setCombinedClassLoader(combinedClassLoader);
        groupManager.setBundleContext(bundleContext);
        groupManager.setConfigurationAdmin(configurationAdmin);
        groupManager.setEventTransportFactory(eventTransportFactory);
        groupManager.init();
        register(new Class[]{GroupManager.class, SynchronousConfigurationListener.class}, groupManager);

        LOGGER.debug("CELLAR HAZELCAST: create Cellar membership listener");
        CellarMembershipListener membershipListener = new CellarMembershipListener(hazelcastInstance);
        membershipListener.setSynchronizers(synchronizers);
        membershipListener.setGroupManager(groupManager);

        Node node = clusterManager.getNode();

        LOGGER.debug("CELLAR HAZELCAST: init topic consumer");
        consumer = new TopicConsumer();
        consumer.setInstance(hazelcastInstance);
        consumer.setDispatcher(dispatcher);
        consumer.setNode(node);
        consumer.setConfigurationAdmin(configurationAdmin);
        consumer.init();

        LOGGER.debug("CELLAR HAZELCAST: init topic producer");
        producer = new TopicProducer();
        producer.setInstance(hazelcastInstance);
        producer.setNode(node);
        producer.setConfigurationAdmin(configurationAdmin);
        producer.init();
        register(EventProducer.class, producer);

        LOGGER.debug("CELLAR HAZELCAST: register basic command store");
        CommandStore commandStore = new BasicCommandStore();
        register(CommandStore.class, commandStore);

        LOGGER.debug("CELLAR HAZELCAST: register clustered execution context");
        ClusteredExecutionContext executionContext = new ClusteredExecutionContext();
        executionContext.setProducer(producer);
        executionContext.setCommandStore(commandStore);
        register(ExecutionContext.class, executionContext);

        LOGGER.debug("CELLAR HAZELCAST: register producer switch command handler");
        ProducerSwitchCommandHandler producerSwitchCommandHandler = new ProducerSwitchCommandHandler();
        producerSwitchCommandHandler.setProducer(producer);
        producerSwitchCommandHandler.setConfigurationAdmin(configurationAdmin);
        register(EventHandler.class, producerSwitchCommandHandler);

        LOGGER.debug("CELLAR HAZELCAST: register producer switch result handler");
        ProducerSwitchResultHandler producerSwitchResultHandler = new ProducerSwitchResultHandler();
        producerSwitchResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, producerSwitchResultHandler);

        LOGGER.debug("CELLAR HAZELCAST: register consumer switch command handler");
        ConsumerSwitchCommandHandler consumerSwitchCommandHandler = new ConsumerSwitchCommandHandler();
        consumerSwitchCommandHandler.setProducer(producer);
        consumerSwitchCommandHandler.setConsumer(consumer);
        consumerSwitchCommandHandler.setConfigurationAdmin(configurationAdmin);
        register(EventHandler.class, consumerSwitchCommandHandler);

        LOGGER.debug("CELLAR HAZELCAST; register consumer switch result handler");
        ConsumerSwitchResultHandler consumerSwitchResultHandler = new ConsumerSwitchResultHandler();
        consumerSwitchResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, consumerSwitchResultHandler);

        LOGGER.debug("CELLAR HAZELCAST: register manage handlers command handler");
        ManageHandlersCommandHandler manageHandlersCommandHandler = new ManageHandlersCommandHandler();
        manageHandlersCommandHandler.setConfigurationAdmin(configurationAdmin);
        manageHandlersCommandHandler.setProducer(producer);
        manageHandlersCommandHandler.setProxyManager(proxyManager);
        register(EventHandler.class, manageHandlersCommandHandler);

        LOGGER.debug("CELLAR HAZELCAST: register manage handlers result handler");
        ManageHandlersResultHandler manageHandlersResultHandler = new ManageHandlersResultHandler();
        manageHandlersResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, manageHandlersResultHandler);

        LOGGER.debug("CELLAR HAZELCAST: register manage group command handler");
        ManageGroupCommandHandler manageGroupCommandHandler = new ManageGroupCommandHandler();
        manageGroupCommandHandler.setProducer(producer);
        manageGroupCommandHandler.setClusterManager(clusterManager);
        manageGroupCommandHandler.setGroupManager(groupManager);
        register(EventHandler.class, manageGroupCommandHandler);

        LOGGER.debug("CELLAR HAZELCAST: register manage group result handler");
        ManageGroupResultHandler manageGroupResultHandler = new ManageGroupResultHandler();
        manageGroupResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, manageGroupResultHandler);

        LOGGER.debug("CELLAR HAZELCAST: register shutdown command handler");
        ShutdownCommandHandler shutdownCommandHandler = new ShutdownCommandHandler();
        shutdownCommandHandler.setBundleContext(bundleContext);
        shutdownCommandHandler.setProducer(producer);
        shutdownCommandHandler.setClusterManager(clusterManager);
        shutdownCommandHandler.setGroupManager(groupManager);
        register(EventHandler.class, shutdownCommandHandler);

        LOGGER.debug("CELLAR HAZELCAST: register shutdown command result handler");
        ShutdownResultHandler shutdownResultHandler = new ShutdownResultHandler();
        shutdownResultHandler.setCommandStore(commandStore);
        register(EventHandler.class, shutdownCommandHandler);

        LOGGER.debug("CELLAR HAZELCAST: start the synchronizer service tracker");
        synchronizerServiceTracker = new ServiceTracker<Synchronizer, Synchronizer>(bundleContext, Synchronizer.class, new ServiceTrackerCustomizer<Synchronizer, Synchronizer>() {
            @Override
            public Synchronizer addingService(ServiceReference<Synchronizer> serviceReference) {
                Synchronizer service = bundleContext.getService(serviceReference);
                synchronizers.add(service);
                return service;
            }

            @Override
            public void modifiedService(ServiceReference<Synchronizer> serviceReference, Synchronizer synchronizer) {
                // nothing to do
            }

            @Override
            public void removedService(ServiceReference<Synchronizer> serviceReference, Synchronizer synchronizer) {
                synchronizers.remove(synchronizer);
                bundleContext.ungetService(serviceReference);
            }
        });
        synchronizerServiceTracker.open();

        LOGGER.debug("CELLAR HAZELCAST: register Cellar Core MBean");
        CellarMBeanImpl cellarMBean = new CellarMBeanImpl();
        cellarMBean.setBundleContext(bundleContext);
        cellarMBean.setClusterManager(clusterManager);
        cellarMBean.setGroupManager(groupManager);
        cellarMBean.setExecutionContext(executionContext);
        Hashtable props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=core,name=" + System.getProperty("karaf.name"));
        coreMBeanRegistration = bundleContext.registerService(getInterfaceNames(cellarMBean), cellarMBean, props);

        LOGGER.debug("CELLAR HAZELCAST: register Cellar Node MBean");
        CellarNodeMBeanImpl cellarNodeMBean = new CellarNodeMBeanImpl();
        cellarNodeMBean.setClusterManager(clusterManager);
        cellarNodeMBean.setExecutionContext(executionContext);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=node,name=" + System.getProperty("karaf.name"));
        nodeMBeanRegistration = bundleContext.registerService(getInterfaceNames(cellarNodeMBean), cellarNodeMBean, props);

        LOGGER.debug("CELLAR HAZELCAST: register Cellar Group MBean");
        CellarGroupMBeanImpl cellarGroupMBean = new CellarGroupMBeanImpl();
        cellarGroupMBean.setClusterManager(clusterManager);
        cellarGroupMBean.setExecutionContext(executionContext);
        cellarGroupMBean.setGroupManager(groupManager);
        props = new Hashtable();
        props.put("jmx.objectname", "org.apache.karaf.cellar:type=group,name=" + System.getProperty("karaf.name"));
        groupMBeanRegistration = bundleContext.registerService(getInterfaceNames(cellarGroupMBean), cellarGroupMBean, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (groupMBeanRegistration != null) {
            groupMBeanRegistration.unregister();
            groupMBeanRegistration = null;
        }
        if (nodeMBeanRegistration != null) {
            nodeMBeanRegistration.unregister();
            nodeMBeanRegistration = null;
        }
        if (coreMBeanRegistration != null) {
            coreMBeanRegistration.unregister();
            coreMBeanRegistration = null;
        }
        if (synchronizerServiceTracker != null) {
            synchronizerServiceTracker.close();
            synchronizerServiceTracker = null;
        }
        if (groupManager != null) {
            try {
                groupManager.destroy();
            } catch (Exception e) {
                LOGGER.trace("Error occured destroying the group manager", e);
            }
            groupManager = null;
        }
        if (hazelcastServiceFactory != null) {
            hazelcastServiceFactory.destroy();
            hazelcastServiceFactory = null;
        }
        if (hazelcastInstance != null) {
            hazelcastInstance.shutdown();
            hazelcastInstance = null;
        }
        if (discoveryTask != null) {
            discoveryTask.destroy();
            discoveryTask = null;
        }
        if (producer != null) {
            producer.destroy();
            producer = null;
        }
        if (consumer != null) {
            consumer.destroy();
            consumer = null;
        }
        if (extender != null) {
            extender.destroy();
            extender = null;
        }
        if (discoveryServiceTracker != null) {
            discoveryServiceTracker.close();
            discoveryServiceTracker = null;
        }
        if (combinedClassLoader != null) {
            combinedClassLoader.destroy();
            combinedClassLoader = null;
        }
        if (dispatcher != null) {
            dispatcher.destroy();
            dispatcher = null;
        }
    }

    @Override
    public void updated(Dictionary config) {
        if (config == null) {
            return;
        }
        HashMap map = new HashMap();
        for (Enumeration keys = config.keys(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            map.put(key, config.get(key));
        }
        if (hazelcastServiceFactory != null) {
            updated(map);
        } else {
            // postpone configuration update
            updatedConfig = map;
        }
    }

    private void updated(HashMap config) {
        try {
            hazelcastServiceFactory.update(config);
        } catch (Exception e) {
            LOGGER.error("Can't update Hazelcast service factory", e);
        }
    }
}
