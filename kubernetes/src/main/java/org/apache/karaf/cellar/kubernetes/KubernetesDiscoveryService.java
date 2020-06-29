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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import okhttp3.TlsVersion;
import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Discovery service that uses the Kubernetes API to discover Cellar nodes.
 */
public class KubernetesDiscoveryService implements DiscoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscoveryService.class);

    private String kubernetesMaster;
    private String kubernetesApiVersion;
    private boolean kubernetesTrustCertificates;
    private boolean kubernetesDisableHostnameVerification;
    private String kubernetesCertsCaFile;
    private String kubernetesCertsCaData;
    private String kubernetesCertsClientFile;
    private String kubernetesCertsClientData;
    private String kubernetesCertsClientKeyFile;
    private String kubernetesCertsClientKeyData;
    private String kubernetesCertsClientKeyAlgo;
    private String kubernetesCertsClientKeyPassphrase;
    private String kubernetesAuthBasicUsername;
    private String kubernetesAuthBasicPassword;
    private String kubernetesOauthToken;
    private int kubernetesWatchReconnectInterval;
    private int kubernetesWatchReconnectLimit;
    private String kubernetesUserAgent;
    private String kubernetesTlsVersion;
    private String kubernetesTruststoreFile;
    private String kubernetesTruststorePassphrase;
    private String kubernetesKeystoreFile;
    private String kubernetesKeystorePassphrase;
    private String kubernetesPodLabelKey;
    private String kubernetesPodLabelValue;

    private KubernetesClient kubernetesClient;

    public KubernetesDiscoveryService() {
        LOGGER.debug("CELLAR KUBERNETES: create discovery service");
    }

    Config createConfig() {
        return new ConfigBuilder()
                .withMasterUrl(kubernetesMaster)
                .withApiVersion(kubernetesApiVersion)
                .withTrustCerts(kubernetesTrustCertificates)
                .withDisableHostnameVerification(kubernetesDisableHostnameVerification)
                .withCaCertFile(kubernetesCertsCaFile)
                .withCaCertData(kubernetesCertsCaData)
                .withClientCertFile(kubernetesCertsClientFile)
                .withClientCertData(kubernetesCertsClientData)
                .withClientKeyFile(kubernetesCertsClientKeyFile)
                .withClientKeyData(kubernetesCertsClientKeyData)
                .withClientKeyAlgo(kubernetesCertsClientKeyAlgo)
                .withClientKeyPassphrase(kubernetesCertsClientKeyPassphrase)
                .withUsername(kubernetesAuthBasicUsername)
                .withPassword(kubernetesAuthBasicPassword)
                .withOauthToken(kubernetesOauthToken)
                .withWatchReconnectInterval(kubernetesWatchReconnectInterval)
                .withWatchReconnectLimit(kubernetesWatchReconnectLimit)
                .withUserAgent(kubernetesUserAgent)
                .withTlsVersions(TlsVersion.forJavaName(kubernetesTlsVersion))
                .withTrustStoreFile(kubernetesTruststoreFile)
                .withTrustStorePassphrase(kubernetesTruststorePassphrase)
                .withKeyStoreFile(kubernetesKeystoreFile)
                .withKeyStorePassphrase(kubernetesKeystorePassphrase)
                .build();
    }

    public void init() {
        try {
            LOGGER.debug("CELLAR KUBERNETES: query API at {} ...", kubernetesMaster);
            Config config = createConfig();
            kubernetesClient = new DefaultKubernetesClient(config);
            LOGGER.debug("CELLAR KUBERNETES: discovery service initialized");
        } catch (Exception e) {
            LOGGER.error("CELLAR KUBERNETES: can't init discovery service", e);
        }
    }

    public void destroy() {
        LOGGER.debug("CELLAR KUBERNETES: destroy discovery service");
    }

    public void update(Map<String, Object> properties) {
        LOGGER.debug("CELLAR KUBERNETES: update properties");
    }

    @Override
    public Set<String> discoverMembers() {
        LOGGER.debug("CELLAR KUBERNETES: query pods with labeled with [{}={}]", kubernetesPodLabelKey, kubernetesPodLabelValue);
        Set<String> members = new HashSet<String>();
        try {
            PodList podList = kubernetesClient.pods().list();
            for (Pod pod : podList.getItems()) {
                String value = pod.getMetadata().getLabels().get(kubernetesPodLabelKey);
                if (value != null && !value.isEmpty() && value.equals(kubernetesPodLabelValue)) {
                    members.add(pod.getStatus().getPodIP());
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR KUBERNETES: can't get pods", e);
        }
        return members;
    }

    @Override
    public void signIn() {
        // nothing to do for Kubernetes
    }

    @Override
    public void refresh() {
        // nothing to do for Kubernetes
    }

    @Override
    public void signOut() {
        // nothing to do for Kubernetes
    }

    void setKubernetesClient(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public String getKubernetesPodLabelKey() {
        return kubernetesPodLabelKey;
    }

    public void setKubernetesPodLabelKey(String kubernetesPodLabelKey) {
        this.kubernetesPodLabelKey = kubernetesPodLabelKey;
    }

    public String getKubernetesPodLabelValue() {
        return kubernetesPodLabelValue;
    }

    public void setKubernetesPodLabelValue(String kubernetesPodLabelValue) {
        this.kubernetesPodLabelValue = kubernetesPodLabelValue;
    }

    public String getKubernetesMaster() {
        return kubernetesMaster;
    }

    public void setKubernetesMaster(String kubernetesMaster) {
        this.kubernetesMaster = kubernetesMaster;
    }

    public String getKubernetesApiVersion() {
        return kubernetesApiVersion;
    }

    public void setKubernetesApiVersion(String kubernetesApiVersion) {
        this.kubernetesApiVersion = kubernetesApiVersion;
    }

    public boolean isKubernetesTrustCertificates() {
        return kubernetesTrustCertificates;
    }

    public void setKubernetesTrustCertificates(String kubernetesTrustCertificates) {
        if (kubernetesTrustCertificates != null) {
            this.kubernetesTrustCertificates = Boolean.parseBoolean(kubernetesTrustCertificates);
        }
    }

    public boolean isKubernetesDisableHostnameVerification() {
        return kubernetesDisableHostnameVerification;
    }

    public void setKubernetesDisableHostnameVerification(String kubernetesDisableHostnameVerification) {
        if (kubernetesDisableHostnameVerification != null) {
            this.kubernetesDisableHostnameVerification = Boolean.parseBoolean(kubernetesDisableHostnameVerification);
        }
    }

    public String getKubernetesCertsCaFile() {
        return kubernetesCertsCaFile;
    }

    public void setKubernetesCertsCaFile(String kubernetesCertsCaFile) {
        this.kubernetesCertsCaFile = kubernetesCertsCaFile;
    }

    public String getKubernetesCertsCaData() {
        return kubernetesCertsCaData;
    }

    public void setKubernetesCertsCaData(String kubernetesCertsCaData) {
        this.kubernetesCertsCaData = kubernetesCertsCaData;
    }

    public String getKubernetesCertsClientFile() {
        return kubernetesCertsClientFile;
    }

    public void setKubernetesCertsClientFile(String kubernetesCertsClientFile) {
        this.kubernetesCertsClientFile = kubernetesCertsClientFile;
    }

    public String getKubernetesCertsClientData() {
        return kubernetesCertsClientData;
    }

    public void setKubernetesCertsClientData(String kubernetesCertsClientData) {
        this.kubernetesCertsClientData = kubernetesCertsClientData;
    }

    public String getKubernetesCertsClientKeyFile() {
        return kubernetesCertsClientKeyFile;
    }

    public void setKubernetesCertsClientKeyFile(String kubernetesCertsClientKeyFile) {
        this.kubernetesCertsClientKeyFile = kubernetesCertsClientKeyFile;
    }

    public String getKubernetesCertsClientKeyData() {
        return kubernetesCertsClientKeyData;
    }

    public void setKubernetesCertsClientKeyData(String kubernetesCertsClientKeyData) {
        this.kubernetesCertsClientKeyData = kubernetesCertsClientKeyData;
    }

    public String getKubernetesCertsClientKeyAlgo() {
        return kubernetesCertsClientKeyAlgo;
    }

    public void setKubernetesCertsClientKeyAlgo(String kubernetesCertsClientKeyAlgo) {
        this.kubernetesCertsClientKeyAlgo = kubernetesCertsClientKeyAlgo;
    }

    public String getKubernetesCertsClientKeyPassphrase() {
        return kubernetesCertsClientKeyPassphrase;
    }

    public void setKubernetesCertsClientKeyPassphrase(String kubernetesCertsClientKeyPassphrase) {
        this.kubernetesCertsClientKeyPassphrase = kubernetesCertsClientKeyPassphrase;
    }

    public String getKubernetesAuthBasicUsername() {
        return kubernetesAuthBasicUsername;
    }

    public void setKubernetesAuthBasicUsername(String kubernetesAuthBasicUsername) {
        this.kubernetesAuthBasicUsername = kubernetesAuthBasicUsername;
    }

    public String getKubernetesAuthBasicPassword() {
        return kubernetesAuthBasicPassword;
    }

    public void setKubernetesAuthBasicPassword(String kubernetesAuthBasicPassword) {
        this.kubernetesAuthBasicPassword = kubernetesAuthBasicPassword;
    }

    public String getKubernetesOauthToken() {
        return kubernetesOauthToken;
    }

    public void setKubernetesOauthToken(String kubernetesOauthToken) {
        this.kubernetesOauthToken = kubernetesOauthToken;
    }

    public int getKubernetesWatchReconnectInterval() {
        return kubernetesWatchReconnectInterval;
    }

    public void setKubernetesWatchReconnectInterval(String kubernetesWatchReconnectInterval) {
        if (kubernetesWatchReconnectInterval != null) {
            this.kubernetesWatchReconnectInterval = Integer.parseInt(kubernetesWatchReconnectInterval);
        }
    }

    public int getKubernetesWatchReconnectLimit() {
        return kubernetesWatchReconnectLimit;
    }

    public void setKubernetesWatchReconnectLimit(String kubernetesWatchReconnectLimit) {
        if (kubernetesWatchReconnectLimit != null) {
            this.kubernetesWatchReconnectLimit = Integer.parseInt(kubernetesWatchReconnectLimit);
        }
    }

    public String getKubernetesUserAgent() {
        return kubernetesUserAgent;
    }

    public void setKubernetesUserAgent(String kubernetesUserAgent) {
        this.kubernetesUserAgent = kubernetesUserAgent;
    }

    public String getKubernetesTlsVersion() {
        return kubernetesTlsVersion;
    }

    public void setKubernetesTlsVersion(String kubernetesTlsVersion) {
        this.kubernetesTlsVersion = kubernetesTlsVersion;
    }

    public String getKubernetesTruststoreFile() {
        return kubernetesTruststoreFile;
    }

    public void setKubernetesTruststoreFile(String kubernetesTruststoreFile) {
        this.kubernetesTruststoreFile = kubernetesTruststoreFile;
    }

    public String getKubernetesTruststorePassphrase() {
        return kubernetesTruststorePassphrase;
    }

    public void setKubernetesTruststorePassphrase(String kubernetesTruststorePassphrase) {
        this.kubernetesTruststorePassphrase = kubernetesTruststorePassphrase;
    }

    public String getKubernetesKeystoreFile() {
        return kubernetesKeystoreFile;
    }

    public void setKubernetesKeystoreFile(String kubernetesKeystoreFile) {
        this.kubernetesKeystoreFile = kubernetesKeystoreFile;
    }

    public String getKubernetesKeystorePassphrase() {
        return kubernetesKeystorePassphrase;
    }

    public void setKubernetesKeystorePassphrase(String kubernetesKeystorePassphrase) {
        this.kubernetesKeystorePassphrase = kubernetesKeystorePassphrase;
    }
}
