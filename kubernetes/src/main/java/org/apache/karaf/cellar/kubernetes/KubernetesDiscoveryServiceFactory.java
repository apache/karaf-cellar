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
package org.apache.karaf.cellar.kubernetes;

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
 * A factory for Kubernetes discovery services.
 */
public class KubernetesDiscoveryServiceFactory implements ManagedServiceFactory {

    private static String getEnvOrDefault(String var, String def) {
        final String val = System.getenv(var);
        return val == null ? def : val;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscoveryServiceFactory.class);

    private static final String KUBERNETES_HOST = "host";
    private static final String KUBERNETES_PORT = "port";
    private static final String KUBERNETES_POD_LABEL_KEY = "pod.label.key";
    private static final String KUBERNETES_POD_LABEL_VALUE = "pod.label.value";

    private final Map<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();

    private final BundleContext bundleContext;

    public KubernetesDiscoveryServiceFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getName() {
        return "CELLAR KUBERNETES: discovery service factory";
    }

    @Override
    public void updated(String pid, Dictionary properties) throws ConfigurationException {

        ServiceRegistration newServiceRegistration = null;
        try {
            if (properties != null) {

                LOGGER.info("CELLAR KUBERNETES: creating the discovery service ...");

                Properties serviceProperties = new Properties();
                for (Map.Entry entry : serviceProperties.entrySet()) {
                    Object key = entry.getKey();
                    Object value = entry.getValue();
                    serviceProperties.put(key, value);
                }

                KubernetesDiscoveryService kubernetesDiscoveryService = new KubernetesDiscoveryService();

                String kubernetesHost = (String) properties.get(KUBERNETES_HOST);
                String kubernetesPort = (String) properties.get(KUBERNETES_PORT);
                if (kubernetesHost == null) {
                    kubernetesHost = getEnvOrDefault("KUBERNETES_RO_SERVICE_HOST", "localhost");
                }
                if (kubernetesPort == null) {
                    kubernetesPort = getEnvOrDefault("KUBERNETES_RO_SERVICE_PORT", "8080");
                }
                String kubernetesPodLabelKey = (String) properties.get(KUBERNETES_POD_LABEL_KEY);
                if (kubernetesPodLabelKey == null) {
                    kubernetesPodLabelKey = "name";
                }
                String kubernetesPodLabelValue = (String) properties.get(KUBERNETES_POD_LABEL_VALUE);
                if (kubernetesPodLabelValue == null) {
                    kubernetesPodLabelValue = "cellar";
                }

                kubernetesDiscoveryService.setKubernetesHost(kubernetesHost);
                kubernetesDiscoveryService.setKubernetesPort(kubernetesPort);
                kubernetesDiscoveryService.setKubernetesPodLabelKey(kubernetesPodLabelKey);
                kubernetesDiscoveryService.setKubernetesPodLabelValue(kubernetesPodLabelValue);

                kubernetesDiscoveryService.init();

                newServiceRegistration = bundleContext.registerService(DiscoveryService.class.getName(), kubernetesDiscoveryService, (Dictionary) serviceProperties);
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
        LOGGER.debug("CELLAR KUBERNETES: delete discovery service {}", pid);
        ServiceRegistration oldServiceRegistration = registrations.remove(pid);
        if (oldServiceRegistration != null) {
            oldServiceRegistration.unregister();
        }
    }

}
