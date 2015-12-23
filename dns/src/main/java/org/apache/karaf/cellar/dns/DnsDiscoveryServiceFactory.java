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
package org.apache.karaf.cellar.dns;

import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory for DNS SRV discovery services.
 */
public class DnsDiscoveryServiceFactory implements ManagedServiceFactory {

    private static String getEnvOrDefault(String var, String def) {
        final String val = System.getenv(var);
        return val == null ? def : val;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsDiscoveryServiceFactory.class);

    private static final String DNS_SERVICE = "dns.service";

    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();

    private final BundleContext bundleContext;

    public DnsDiscoveryServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "CELLAR DNS: discovery service factory";
    }

    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {

        ServiceRegistration newServiceRegistration = null;
        try {
            if (properties != null) {

                LOGGER.info("CELLAR DNS: creating the discovery service ...");

                Properties serviceProperties = new Properties();
                Enumeration propKeys = properties.keys();
                while (propKeys.hasMoreElements()) {
                    Object key = propKeys.nextElement();
                    Object value = properties.get(key);
                    serviceProperties.put(key, value);
                }

                DnsDiscoveryService dnsDiscoveryService = new DnsDiscoveryService();
                String dnsService = (String) properties.get(DNS_SERVICE);
                if (dnsService == null) {
                    dnsService = getEnvOrDefault("DNS_RO_SERVICE_NAME", "_karaf._tcp.");
                }

                dnsDiscoveryService.setDnsService(dnsService);

                newServiceRegistration = bundleContext.registerService(DiscoveryService.class.getName(), dnsDiscoveryService, (Dictionary) serviceProperties);
            }
        } finally {
            ServiceRegistration oldServiceRegistration = (newServiceRegistration == null) ? registrations.remove(pid) : registrations.put(pid, newServiceRegistration);
            if (oldServiceRegistration != null) {
                oldServiceRegistration.unregister();
            }
        }
    }

    @Override
    public void deleted(String pid) {
        LOGGER.debug("CELLAR DNS: delete discovery service {}", pid);
        ServiceRegistration oldServiceRegistration = registrations.remove(pid);
        if (oldServiceRegistration != null) {
            oldServiceRegistration.unregister();
        }
    }

}
