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
package org.apache.karaf.cellar.etcd;

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
 * A factory for Etcd discovery services.
 */
public class EtcdDiscoveryServiceFactory implements ManagedServiceFactory {

    private static String getOrDefault(Properties properties, String var, String envVar, String def) {
        String val = (String) properties.get(ETCD_SERVICE);
        if(val == null) {
            val = System.getenv(envVar);
        }

        return val == null ? def : val;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdDiscoveryServiceFactory.class);

    private static final String ETCD_SERVICE = "etcd.service";
    private static final String ETCD_SERVICE_ENV = "ETCD_RO_SERVICE_NAME";
    private static final String ETCD_SERVICE_DEFAULT = "karaf";

    private static final String ETCD_ENPOINT = "etcd.endpoint";
    private static final String ETCD_ENPOINT_ENV = "ETCD_RO_ENDPOINT";
    private static final String ETCD_ENPOINT_DEFAULT = "http://127.0.0.1:2379";

    private static final String ETCD_USERNAME = "etcd.username";
    private static final String ETCD_USERNAME_ENV = "ETCD_RO_USERNAME";
    private static final String ETCD_USERNAME_DEFAULT = null;

    private static final String ETCD_PASSWORD = "etcd.password";
    private static final String ETCD_PASSWORD_ENV = "ETCD_RO_PASSWORD";
    private static final String ETCD_PASSWORD_DEFAULT = null;

    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();

    private final BundleContext bundleContext;

    public EtcdDiscoveryServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "CELLAR Etcd: discovery service factory";
    }

    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {
        ServiceRegistration newServiceRegistration = null;
        try {
            if (properties != null) {

                LOGGER.info("CELLAR Etcd: creating the discovery service ...");

                Properties serviceProperties = new Properties();
                Enumeration propKeys = properties.keys();
                while (propKeys.hasMoreElements()) {
                    Object key = propKeys.nextElement();
                    Object value = properties.get(key);
                    serviceProperties.put(key, value);
                }

                EtcdDiscoveryService etcdDiscoveryService = new EtcdDiscoveryService()
                    .setEtcdServiceName(
                        getOrDefault(serviceProperties, ETCD_SERVICE, ETCD_SERVICE_ENV, ETCD_SERVICE_DEFAULT))
                    .setEtcdEndpoint(
                        getOrDefault(serviceProperties, ETCD_ENPOINT, ETCD_ENPOINT_ENV, ETCD_ENPOINT_DEFAULT))
                    .setEtcdUsername(
                        getOrDefault(serviceProperties, ETCD_USERNAME, ETCD_USERNAME_ENV, ETCD_USERNAME_DEFAULT))
                    .setEtcdPassword(
                        getOrDefault(serviceProperties, ETCD_PASSWORD, ETCD_PASSWORD_ENV, ETCD_PASSWORD_DEFAULT));


                newServiceRegistration = bundleContext.registerService(DiscoveryService.class.getName(), etcdDiscoveryService, (Dictionary) serviceProperties);
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
        LOGGER.debug("CELLAR Etcd: delete discovery service {}", pid);
        ServiceRegistration oldServiceRegistration = registrations.remove(pid);
        if (oldServiceRegistration != null) {
            oldServiceRegistration.unregister();
        }
    }

}
