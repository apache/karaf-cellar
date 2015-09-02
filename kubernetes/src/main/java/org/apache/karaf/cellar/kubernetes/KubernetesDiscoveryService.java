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

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;

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

    private String kubernetesHost;
    private String kubernetesPort;
    private String kubernetesPodLabelKey;
    private String kubernetesPodLabelValue;

    private KubernetesClient kubernetesClient;

    public KubernetesDiscoveryService() {
        LOGGER.debug("CELLAR KUBERNETES: create discovery service");
    }

    public void init() {
        try {
            String kubernetesUrl = "http://" + kubernetesHost + ":" + kubernetesPort;
            LOGGER.debug("CELLAR KUBERNETES: query API at {} ...", kubernetesUrl);
            Config config = new ConfigBuilder().withMasterUrl(kubernetesUrl).build();
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
                if (value != null && !value.isEmpty()) {
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

    public String getKubernetesHost() {
        return kubernetesHost;
    }

    public void setKubernetesHost(String kubernetesHost) {
        this.kubernetesHost = kubernetesHost;
    }

    public String getKubernetesPort() {
        return kubernetesPort;
    }

    public void setKubernetesPort(String kubernetesPort) {
        this.kubernetesPort = kubernetesPort;
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

}
