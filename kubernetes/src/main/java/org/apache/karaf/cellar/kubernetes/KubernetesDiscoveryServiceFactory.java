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

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory kubernetes discovery service.
 */
public class KubernetesDiscoveryServiceFactory implements ManagedServiceFactory {

    private static String getEnvOrDefault(String var, String def) {
        final String val = System.getenv(var);
        return val == null ? def : val;
    }

    private static final transient Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscoveryServiceFactory.class);

    private static final String KUBE_POD_LABEL_KEY = "pod.label.key";
    private static final String KUBE_POD_LABEL_VALUE = "pod.label.value";

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
        ServiceRegistration newRegistration = null;
        try {
            if (properties != null) {

                Properties serviceProperties = new Properties();

                for (Map.Entry entry : serviceProperties.entrySet()) {
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    serviceProperties.put(key, val);
                }

                KubernetesDiscoveryService service = new KubernetesDiscoveryService();

                String kubeHost = getEnvOrDefault("KUBERNETES_RO_SERVICE_HOST", "localhost");
                String kubePort = getEnvOrDefault("KUBERNETES_RO_SERVICE_PORT", "8080");
                String kubePodLabelKey = (String) properties.get(KUBE_POD_LABEL_KEY);
                String kubePodLabelValue = (String) properties.get(KUBE_POD_LABEL_VALUE);

                service.setKubeHost(kubeHost);
                service.setKubePort(kubePort);
                service.setKubePodLabelKey(kubePodLabelKey);
                service.setKubePodLabelValue(kubePodLabelValue);
                service.init();

                newRegistration = bundleContext.registerService(DiscoveryService.class.getName(), service, (Dictionary) serviceProperties);
            }
        } finally {
            ServiceRegistration oldRegistration = (newRegistration == null) ? registrations.remove(pid) : registrations.put(pid, newRegistration);
            if (oldRegistration != null) {
                LOGGER.debug("CELLAR KUBERNETES: un-registering discovery service {}", pid);
                oldRegistration.unregister();
            }
        }
    }

    @Override
    public void deleted(String pid) {
        LOGGER.debug("CELLAR Kubernetes: deleting discovery service {}", pid);
        ServiceRegistration oldRegistration = registrations.remove(pid);
        if (oldRegistration != null) {
            oldRegistration.unregister();
        }
    }

}
