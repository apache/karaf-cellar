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
package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.command.ClusteredExecutionContext;
import org.apache.karaf.cellar.core.command.CommandStore;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.ListenerHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listener for the service import.
 */
public class ImportServiceListener implements ListenerHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportServiceListener.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Map<ListenerInfo, String> listenerRegistrations = Collections.synchronizedMap(new LinkedHashMap());
    private final Set<ListenerInfo> pendingListeners = Collections.synchronizedSet(new LinkedHashSet());
    private final Map<String, ServiceRegistration> serviceRegistrations = new HashMap();
    private final Map<String, ExecutionContext> executionContexts = new HashMap();
    private final Map<String, EventConsumer> consumers = new HashMap();
    private Map<String, EndpointDescription> remoteEndpoints;
    private EventTransportFactory eventTransportFactory;
    private ClusterManager clusterManager;
    private BundleContext bundleContext;
    private CommandStore commandStore;

    public void init() {
        remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
        scheduledExecutorService.scheduleWithFixedDelay(new RemoteServiceTracker(this), 5, 5, TimeUnit.SECONDS);
    }

    public void destroy() {
        scheduledExecutorService.shutdown();
        synchronized (pendingListeners) {
            for (ServiceRegistration serviceRegistration : serviceRegistrations.values()) {
                LOGGER.trace("CELLAR DOSGI: DESTROY removing registration {}", serviceRegistration.getReference().getPropertyKeys());
                serviceRegistration.unregister();
            }
            for (EventConsumer eventConsumer : consumers.values()) {
                eventConsumer.stop();
            }
            listenerRegistrations.clear();
            serviceRegistrations.clear();
            executionContexts.clear();
            pendingListeners.clear();
            consumers.clear();
        }
    }

    @Override
    public void added(Collection listeners) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // create a clone of remote endpoint descriptions to avoid concurrency concerns while iterating it
            final Set<EndpointDescription> endpointDescriptions = new HashSet(remoteEndpoints.values());
            for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listeners) {
                if (listenerInfo.getFilter() == null) {
                    LOGGER.trace("CELLAR DOSGI: skip adding listener with no filter for bundle {}", listenerInfo.getBundleContext().getBundle().getBundleId());
                } else if (listenerInfo.getBundleContext() == bundleContext) {
                    LOGGER.trace("CELLAR DOSGI: skip adding listener {} with same bundle context for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                } else if (listenerInfo.isRemoved()) {
                    // could be removed by bundles restarting
                    LOGGER.trace("CELLAR DOSGI: skip adding already removed listener {} for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                } else {
                    // make sure we only import remote services
                    if (!checkListener(listenerInfo, endpointDescriptions)) {
                        LOGGER.trace("CELLAR DOSGI: adding pending listener {} for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                        if (!pendingListeners.add(listenerInfo)) {
                            LOGGER.warn("CELLAR DOSGI: pending listener {} for bundle {} already added!", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void removed(Collection listeners) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // create a clone of remote endpoint descriptions to avoid concurrency concerns while iterating it
            final Set<EndpointDescription> endpointDescriptions = new HashSet(remoteEndpoints.values());
            for (ListenerInfo listenerInfo : (Collection<ListenerInfo>) listeners) {
                if (listenerInfo.getFilter() == null) {
                    LOGGER.trace("CELLAR DOSGI: skip removing listener with no filter for bundle {}", listenerInfo.getBundleContext().getBundle().getBundleId());
                } else if (listenerInfo.getBundleContext() == bundleContext) {
                    LOGGER.trace("CELLAR DOSGI: skip removing listener {} with same bundle context for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                } else if (!listenerRegistrations.containsKey(listenerInfo)) {
                    LOGGER.trace("CELLAR DOSGI: skip removing unregistered listener {} for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                } else {
                    // make sure we only match remote services for this listener
                    String filter = "(&" + listenerInfo.getFilter() + "(!(" + Constants.ENDPOINT_FRAMEWORK_UUID + "=" + clusterManager.getNode().getId() + ")))";
                    // iterate through known services and un-import them if needed
                    Set<EndpointDescription> filteredEndpointEndpointDescriptions = new LinkedHashSet();
                    for (EndpointDescription endpointDescription : endpointDescriptions) {
                        if (endpointDescription.matches(filter)) {
                            filteredEndpointEndpointDescriptions.add(endpointDescription);
                        }
                    }
                    synchronized (listenerRegistrations) {
                        for (EndpointDescription filteredEndpointEndpointDescription : filteredEndpointEndpointDescriptions) {
                            Iterator<Map.Entry<ListenerInfo, String>> iterator = listenerRegistrations.entrySet().iterator();
                            while (iterator.hasNext()) {
                                Map.Entry<ListenerInfo, String> entry = iterator.next();
                                if (entry.getKey().equals(listenerInfo) && entry.getValue().equals(filteredEndpointEndpointDescription.getId())) {
                                    LOGGER.trace("CELLAR DOSGI: removing registered listener {} for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                                    iterator.remove();
                                }
                            }
                            // un-import service from registry if last listener for this filter is removed
                            if (!listenerRegistrations.containsValue(filteredEndpointEndpointDescription.getId())) {
                                unImportService(filteredEndpointEndpointDescription.getId());
                            }
                        }
                    }
                    LOGGER.trace("CELLAR DOSGI: removing pending listener {} for bundle {}", listenerInfo.getFilter(), listenerInfo.getBundleContext().getBundle().getBundleId());
                    pendingListeners.remove(listenerInfo);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Check if there is a match for the current {@link ListenerInfo} for importing if not already available.
     *
     * @param listenerInfo         the listener info.
     * @param endpointDescriptions local copy of remote endpoint descriptions.
     */
    private boolean checkListener(ListenerInfo listenerInfo, Set<EndpointDescription> endpointDescriptions) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // iterate through known services and import them if needed
            Set<EndpointDescription> matchedEndpointDescriptions = new LinkedHashSet();
            for (EndpointDescription endpointDescription : endpointDescriptions) {
                // match endpoint on OSGi filter and if not this node is already registered and if service is not already local
                if (endpointDescription.matches(listenerInfo.getFilter()) && !endpointDescription.getNodes().contains(clusterManager.getNode().getId())) {
                    ServiceReference[] serviceReferences = bundleContext.getServiceReferences(endpointDescription.getServiceClass(), listenerInfo.getFilter());
                    if (serviceReferences == null) {
                        matchedEndpointDescriptions.add(endpointDescription);
                        LOGGER.trace("CELLAR DOSGI: remote endpoint {} available for local listener {}", endpointDescription.getId(), listenerInfo.getFilter());
                    }
                }
            }
            for (EndpointDescription matchedRemoteEndpointDescription : matchedEndpointDescriptions) {
                importService(matchedRemoteEndpointDescription, listenerInfo);
            }
            return matchedEndpointDescriptions.size() > 0;
        } catch (Exception e) {
            LOGGER.error("CELLAR DOSGI: listener {} check failed due to {}", listenerInfo.getFilter(), e.getMessage());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return false;
    }

    /**
     * Import a remote service to the service registry.
     *
     * @param endpointDescription the endpoint to import.
     * @param listenerInfo        the associated listener info.
     */
    private void importService(EndpointDescription endpointDescription, ListenerInfo listenerInfo) {
        LOGGER.debug("CELLAR DOSGI: importing remote endpoint {} with filter {}", endpointDescription.getId(), listenerInfo.getFilter());

        ExecutionContext executionContext = executionContexts.get(endpointDescription.getId());
        if (executionContext == null) {
            LOGGER.trace("CELLAR DOSGI: creating new request producer");
            Producer requestProducer = eventTransportFactory.getEventProducer(Constants.INTERFACE_PREFIX + Constants.SEPARATOR + endpointDescription.getId(), Boolean.FALSE);
            executionContext = new ClusteredExecutionContext(requestProducer, commandStore);
            executionContexts.put(endpointDescription.getId(), executionContext);
        }

        EventConsumer resultConsumer = consumers.get(endpointDescription.getId());
        if (resultConsumer == null) {
            LOGGER.trace("CELLAR DOSGI: creating new result consumer");
            resultConsumer = eventTransportFactory.getEventConsumer(Constants.RESULT_PREFIX + Constants.SEPARATOR + clusterManager.getNode().getId() + endpointDescription.getId(), Boolean.FALSE);
            consumers.put(endpointDescription.getId(), resultConsumer);
        } else if (!resultConsumer.isConsuming()) {
            resultConsumer.start();
        }

        LOGGER.trace("CELLAR DOSGI: adding service registration for {}", endpointDescription.getServiceClass());
        RemoteServiceFactory remoteServiceFactory = new RemoteServiceFactory(clusterManager, executionContext, endpointDescription);
        ServiceRegistration serviceRegistration = listenerInfo.getBundleContext().registerService(endpointDescription.getServiceClass(), remoteServiceFactory, new Hashtable(endpointDescription.getProperties()));
        serviceRegistrations.put(endpointDescription.getId(), serviceRegistration);
        listenerRegistrations.put(listenerInfo, endpointDescription.getId());
    }

    /**
     * Un-register an imported service.
     *
     * @param endpointId the endpoint to un-register.
     */
    private void unImportService(String endpointId) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            LOGGER.debug("CELLAR DOSGI: un-importing remote service with endpoint {}", endpointId);

            ServiceRegistration registration = serviceRegistrations.remove(endpointId);
            if (registration != null) registration.unregister();

            EventConsumer consumer = consumers.get(endpointId);
            if (consumer != null) {
                consumer.stop();
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public CommandStore getCommandStore() {
        return commandStore;
    }

    public void setCommandStore(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    private static class RemoteServiceTracker implements Runnable {

        private static final Logger LOGGER = LoggerFactory.getLogger(RemoteServiceTracker.class);

        private final ImportServiceListener importServiceListener;

        public RemoteServiceTracker(ImportServiceListener importServiceListener) {
            this.importServiceListener = importServiceListener;
        }

        @Override
        public void run() {
            try {
                // create a clone of remote endpoints to avoid concurrency concerns while iterating it
                final Set<Map.Entry<String, EndpointDescription>> threadLocalRemoteEndpointEntries = new HashSet(importServiceListener.remoteEndpoints.entrySet());
                final Set<EndpointDescription> threadLocalRemoteEndpointValues = new HashSet(threadLocalRemoteEndpointEntries.size());
                final Set<String> threadLocalRemoteEndpointKeys = new HashSet(threadLocalRemoteEndpointEntries.size());
                for (Map.Entry<String, EndpointDescription> localRemoteEndpointEntry : threadLocalRemoteEndpointEntries) {
                    threadLocalRemoteEndpointValues.add(localRemoteEndpointEntry.getValue());
                    threadLocalRemoteEndpointKeys.add(localRemoteEndpointEntry.getKey());
                }
                LOGGER.trace("CELLAR DOSGI: running the remote service tracker task having {} endpoint(s), {} pending listener(s), {} registered listener(s) and {} service registration(s)",
                        threadLocalRemoteEndpointEntries.size(), importServiceListener.pendingListeners.size(), importServiceListener.listenerRegistrations.size(), importServiceListener.serviceRegistrations.size());
                synchronized (importServiceListener.pendingListeners) {
                    Iterator<ListenerInfo> iterator = importServiceListener.pendingListeners.iterator();
                    while (iterator.hasNext()) {
                        ListenerInfo listenerInfo = iterator.next();
                        if (importServiceListener.checkListener(listenerInfo, threadLocalRemoteEndpointValues)) {
                            iterator.remove();
                        }
                    }
                }
                synchronized (importServiceListener.listenerRegistrations) {
                    Iterator<Map.Entry<ListenerInfo, String>> listenerRegistrationIterator = importServiceListener.listenerRegistrations.entrySet().iterator();
                    while (listenerRegistrationIterator.hasNext()) {
                        Map.Entry<ListenerInfo, String> listenerRegistration = listenerRegistrationIterator.next();
                        if (!threadLocalRemoteEndpointKeys.contains(listenerRegistration.getValue())) {
                            LOGGER.trace("CELLAR DOSGI: registered remote endpoint {} unavailable", listenerRegistration.getValue());
                            importServiceListener.unImportService(listenerRegistration.getValue());
                            listenerRegistrationIterator.remove();
                            importServiceListener.pendingListeners.add(listenerRegistration.getKey());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("CELLAR DOSGI: {} / {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

}