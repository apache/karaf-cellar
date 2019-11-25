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
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_API_VERSION;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_AUTH_BASIC_PASSWORD;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_AUTH_BASIC_USERNAME;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_AUTH_TOKEN;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CA_DATA;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CA_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_DATA;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_ALGO;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_DATA;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_DISABLE_HOSTNAME_VERIFICATION;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_KEYSTORE_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_KEYSTORE_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_MASTER;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TLS_VERSIONS;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TRUSTSTORE_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TRUSTSTORE_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TRUST_CERTIFICATES;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_USER_AGENT;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_WATCH_RECONNECTINTERVAL;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_WATCH_RECONNECTLIMIT;

/**
 * A factory for Kubernetes discovery services.
 */
public class KubernetesDiscoveryServiceFactory implements ManagedServiceFactory {

    private static String getEnvOrDefault(String var, String def) {
        final String val = System.getenv(var);
        return val == null ? def : val;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscoveryServiceFactory.class);

    // Deprecated constants, use the fabric8 default ones -> see io.fabric8.kubernetes.client.Config
    @Deprecated
    static final String KUBERNETES_HOST = "host";
    @Deprecated
    static final String KUBERNETES_PORT = "port";

    static final String KUBERNETES_POD_LABEL_KEY = "pod.label.key";
    static final String KUBERNETES_POD_LABEL_VALUE = "pod.label.value";
    static final String DEFAULT_POD_LABEL_KEY = "name";
    static final String DEFAULT_POD_LABEL_VALUE = "cellar";

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
                Enumeration propKeys = properties.keys();
                while (propKeys.hasMoreElements()) {
                    Object key = propKeys.nextElement();
                    Object value = properties.get(key);
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
                    kubernetesPodLabelKey = DEFAULT_POD_LABEL_KEY;
                }
                String kubernetesPodLabelValue = (String) properties.get(KUBERNETES_POD_LABEL_VALUE);
                if (kubernetesPodLabelValue == null) {
                    kubernetesPodLabelValue = DEFAULT_POD_LABEL_VALUE;
                }


                String kubernetesMaster = KUBERNETES_MASTER.getValue(properties);

                // Keep compatibility with old configuration scheme
                if (kubernetesMaster == null) {
                    kubernetesMaster = "http://" + kubernetesHost + ":" + kubernetesPort;
                }

                kubernetesDiscoveryService.setKubernetesMaster(kubernetesMaster);
                kubernetesDiscoveryService.setKubernetesApiVersion(KUBERNETES_API_VERSION.getValue(properties));
                kubernetesDiscoveryService.setKubernetesTrustCertificates(KUBERNETES_TRUST_CERTIFICATES.getValue(properties));
                kubernetesDiscoveryService.setKubernetesDisableHostnameVerification(KUBERNETES_DISABLE_HOSTNAME_VERIFICATION.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsCaFile(KUBERNETES_CERTS_CA_FILE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsCaData(KUBERNETES_CERTS_CA_DATA.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsClientFile(KUBERNETES_CERTS_CLIENT_FILE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsClientData(KUBERNETES_CERTS_CLIENT_DATA.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsClientKeyFile(KUBERNETES_CERTS_CLIENT_KEY_FILE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsClientKeyData(KUBERNETES_CERTS_CLIENT_KEY_DATA.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsClientKeyAlgo(KUBERNETES_CERTS_CLIENT_KEY_ALGO.getValue(properties));
                kubernetesDiscoveryService.setKubernetesCertsClientKeyPassphrase(KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesAuthBasicUsername(KUBERNETES_AUTH_BASIC_USERNAME.getValue(properties));
                kubernetesDiscoveryService.setKubernetesAuthBasicPassword(KUBERNETES_AUTH_BASIC_PASSWORD.getValue(properties));
                kubernetesDiscoveryService.setKubernetesOauthToken(KUBERNETES_AUTH_TOKEN.getValue(properties));
                kubernetesDiscoveryService.setKubernetesWatchReconnectInterval(KUBERNETES_WATCH_RECONNECTINTERVAL.getValue(properties));
                kubernetesDiscoveryService.setKubernetesWatchReconnectLimit(KUBERNETES_WATCH_RECONNECTLIMIT.getValue(properties));
                kubernetesDiscoveryService.setKubernetesUserAgent(KUBERNETES_USER_AGENT.getValue(properties));
                kubernetesDiscoveryService.setKubernetesTlsVersion(KUBERNETES_TLS_VERSIONS.getValue(properties));
                kubernetesDiscoveryService.setKubernetesTruststoreFile(KUBERNETES_TRUSTSTORE_FILE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesTruststorePassphrase(KUBERNETES_TRUSTSTORE_PASSPHRASE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesKeystoreFile(KUBERNETES_KEYSTORE_FILE.getValue(properties));
                kubernetesDiscoveryService.setKubernetesKeystorePassphrase(KUBERNETES_KEYSTORE_PASSPHRASE.getValue(properties));
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
