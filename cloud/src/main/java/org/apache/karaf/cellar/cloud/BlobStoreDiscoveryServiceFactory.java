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
package org.apache.karaf.cellar.cloud;

import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory for blob store discovery service.
 */
public class BlobStoreDiscoveryServiceFactory implements ManagedServiceFactory {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BlobStoreDiscoveryServiceFactory.class);

    private static final String PROVIDER = "provider";
    private static final String IDENTITY = "identity";
    private static final String CREDENTIAL = "credential";
    private static final String CONTAINER = "container";
    private static final String VALIDITY = "validity";

    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();

    private final BundleContext bundleContext;

    public BlobStoreDiscoveryServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "CELLAR CLOUD: blob store discovery service factory";
    }

    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        ServiceRegistration newRegistration = null;
        try {
            if (properties != null) {

                Properties serviceProperties = new Properties();

                for (Map.Entry entry : serviceProperties.entrySet()) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    serviceProperties.put(key, val);
                }

                BlobStoreDiscoveryService service = new BlobStoreDiscoveryService();

                String provider = (String) properties.get(PROVIDER);
                String identity = (String) properties.get(IDENTITY);
                String credential = (String) properties.get(CREDENTIAL);
                String container = (String) properties.get(CONTAINER);
                String validity = (String) properties.get(VALIDITY);

                service.setProvider(provider);
                service.setIdentity(identity);
                service.setCredential(credential);
                service.setContainer(container);
                service.setValidityPeriod(Integer.parseInt(validity));
                service.init();

                newRegistration = bundleContext.registerService(DiscoveryService.class.getName(), service, (Dictionary) serviceProperties);
            }
        } finally {
            ServiceRegistration oldRegistration = (newRegistration == null) ? registrations.remove(pid) : registrations.put(pid, newRegistration);
            if (oldRegistration != null) {
                LOGGER.debug("CELLAR CLOUD: un-registering blob store discovery service {}", pid);
                oldRegistration.unregister();
            }
        }
    }

    @Override
    public void deleted(String pid) {
        LOGGER.debug("CELLAR CLOUD: deleting blob store discovery service {}", pid);
        ServiceRegistration oldRegistration = registrations.remove(pid);
        if (oldRegistration != null) {
            oldRegistration.unregister();
        }
    }

}
