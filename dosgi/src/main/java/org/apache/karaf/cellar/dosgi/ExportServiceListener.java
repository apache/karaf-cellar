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

package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author: iocanel
 */
public class ExportServiceListener implements ServiceListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ExportServiceListener.class);

    private ClusterManager clusterManager;
    private EventTransportFactory eventTransportFactory;

    private BundleContext bundleContext;
    private Map<String,EndpointDescription> remoteEndpoints;

    private final Map<String,EventConsumer> consumers = new HashMap<String,EventConsumer>();

    private String nodeId;

    public void init() {
        nodeId = clusterManager.getNode().getId();
        remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
        bundleContext.addServiceListener(this);

        //Lookup for already exported services.
        ServiceReference[] references = null;
        try {
            String filter = "(" + Constants.EXPORTED_INTERFACES + "=" + Constants.ALL_INTERFACES+")";
            references = bundleContext.getServiceReferences(null, filter);

        if (references != null) {
            for (ServiceReference reference : references) {
                exportService(reference);
            }
        }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Error exporting existing remote services.",e);
        }
    }

    public void destroy() {
        bundleContext.removeServiceListener(this);
            for(Map.Entry<String,EventConsumer> consumerEntry:consumers.entrySet()) {
                EventConsumer consumer = consumerEntry.getValue();
                consumer.stop();
            }
            consumers.clear();
    }


    @Override
    public void serviceChanged(ServiceEvent event) {
            if (event != null) {
                switch (event.getType()) {
                    case ServiceEvent.REGISTERED:
                        exportService(event.getServiceReference());
                        break;
                    case ServiceEvent.UNREGISTERING:
                        unExportService(event.getServiceReference());
                        break;
                    case ServiceEvent.MODIFIED:
                    case ServiceEvent.MODIFIED_ENDMATCH:
                    default:
                        break;
                }
            }

    }

    /**
     * Registers {@link EventConsumer}s for consuming remote service calls.
     *
     * @param serviceReference The reference of the service to be exported.
     */
    public void exportService(ServiceReference serviceReference) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            String exportedServices = (String) serviceReference.getProperty(Constants.EXPORTED_INTERFACES);
            if (exportedServices != null && exportedServices.length() > 0) {
                LOGGER.info("CELLAR DOSGI EVENT: Exporting remote service.");
                String[] interfaces = exportedServices.split(Constants.INTERFACE_SEPARATOR);
                Object service = bundleContext.getService(serviceReference);

                Set<String> exportedInterfaces = getServiceInterfaces(service, interfaces);

                for (String iface : exportedInterfaces) {
                    //Add endpoint description to the set.
                    Version version = serviceReference.getBundle().getVersion();
                    String endpointId = iface + Constants.SEPARATOR + version.toString();

                    EndpointDescription endpoint;

                    if (remoteEndpoints.containsKey(endpointId)) {
                        endpoint = remoteEndpoints.get(endpointId);
                    } else {
                        endpoint = new EndpointDescription(endpointId,nodeId);
                    }

                    remoteEndpoints.put(endpointId, endpoint);

                    //Register the endpoint consumer
                    EventConsumer consumer = consumers.get(endpointId);
                    if(consumer == null) {
                        consumer = eventTransportFactory.getEventConsumer(Constants.INTERFACE_PREFIX + Constants.SEPARATOR + endpointId, false);
                        consumers.put(endpointId, consumer);
                    } else if(!consumer.isConsuming()) {
                        consumer.start();
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Removes {@link EventConsumer}s for consuming remote service calls.
     *
     * @param serviceReference
     */
    public void unExportService(ServiceReference serviceReference) {
      ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            String exportedServices = (String) serviceReference.getProperty(Constants.EXPORTED_INTERFACES);
            if (exportedServices != null && exportedServices.length() > 0) {
                LOGGER.info("CELLAR DOSGI EVENT: Unexporting remote service.");
                String[] interfaces = exportedServices.split(Constants.INTERFACE_SEPARATOR);
                Object service = bundleContext.getService(serviceReference);

                Set<String> exportedInterfaces = getServiceInterfaces(service, interfaces);

                for (String iface : exportedInterfaces) {
                    //Add endpoint description to the set.
                    Version version = serviceReference.getBundle().getVersion();
                    String endpointId = iface + Constants.SEPARATOR + version.toString();

                    EndpointDescription endpointDescription = remoteEndpoints.remove(endpointId);
                    endpointDescription.getNodes().remove(nodeId);
                    //If the endpoint is used for export from other nodes too, then put it back.
                    if(endpointDescription.getNodes().size() > 0) {
                        remoteEndpoints.put(endpointId,endpointDescription);
                    }

                    EventConsumer eventConsumer = consumers.remove(endpointId);
                    eventConsumer.stop();
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Returns a Set of interfaces that match the actual exported service interfaces of a service.
     *
     * @param service
     * @param services
     * @return
     */
    public Set<String> getServiceInterfaces(Object service, String[] services) {
        Set<String> interfaceList = new LinkedHashSet<String>();
        if (service != null && services != null && services.length > 0) {
            for (String s : services) {
                if (Constants.ALL_INTERFACES.equals(s)) {
                    Class[] classes = service.getClass().getInterfaces();
                    if (classes != null && classes.length > 0) {
                        for (Class c : classes) {
                            interfaceList.add(c.getCanonicalName());
                        }
                    }
                } else {
                    try {
                        ClassLoader classLoader = null;
                        if (service.getClass() != null && service.getClass().getClassLoader() != null) {
                            classLoader = service.getClass().getClassLoader();
                        } else {
                            classLoader = ClassLoader.getSystemClassLoader();
                        }

                        Class clazz = classLoader.loadClass(s);
                        String ifaceName = clazz.getCanonicalName();
                        interfaceList.add(ifaceName);
                    } catch (ClassNotFoundException e) {
                        LOGGER.warn("Could not load class", e);
                    }
                }
            }
        }
        return interfaceList;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
