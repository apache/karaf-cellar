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

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.KubernetesFactory;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service that uses kubernetes API to discover other Cellar nodes.
 */
public class KubernetesDiscoveryService implements DiscoveryService {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(KubernetesDiscoveryService.class);

    private String kubeHost;
    private String kubePort;
    private String kubePodLabelKey;
    private String kubePodLabelValue;

    private KubernetesClient kubeClient;

    public KubernetesDiscoveryService() {
        LOGGER.debug("CELLAR Kubernetes: discovery service initialized");
    }

    public void init() {
        try {
            String kubeUrl = "http://" + kubeHost + ":" + kubePort;
            LOGGER.debug("CELLAR Kubernetes: Querying kubernetes API at {}..", kubeUrl);
            kubeClient = new KubernetesClient(new KubernetesFactory(kubeUrl));
        } catch (Exception ex) {
            LOGGER.error("CELLAR Kubernetes: error while initializing discovery service", ex);
        }
    }

    public void destroy() {
    }

    public void update(Map<String, Object> properties) {
        LOGGER.debug("CELLAR Kubernetes: updating properties");
    }

    /**
     * Returns a {@link Set} of IPs.
     *
     * @return a set of IPs.
     */
    @Override
    public Set<String> discoverMembers() {
        LOGGER.debug("CELLAR Kubernetes: Querying pods labeled with [{}={}]", kubePodLabelKey, kubePodLabelValue);
        Set<String> members = new HashSet<String>();
        try {
        PodList podList = kubeClient.getPods();
        for (Pod pod : podList.getItems()) {
            String val = pod.getLabels().get(kubePodLabelKey);
            if (val != null && !val.isEmpty()) {
                members.add(pod.getCurrentState().getPodIP());
            }
        }
        }catch(Exception ex){
            LOGGER.error("CELLAR Kubernetes: there was an error reading from Kubernetes API.");
        }
        LOGGER.debug("CELLAR Kubernetes: returning members {}", members);
        return members;
    }

    /**
     * Sign In member to the {@link DiscoveryService}.
     */
    @Override
    public void signIn() {
    }

    /**
     * Refresh member to the {@link DiscoveryService}.
     */
    @Override
    public void refresh() {
    }

    /**
     * Sign out member to the {@link DiscoveryService}.
     */
    @Override
    public void signOut() {
    }

    public String getKubeHost() {
        return kubeHost;
    }

    public void setKubeHost(String kubeHost) {
        this.kubeHost = kubeHost;
    }

    public String getKubePort() {
        return kubePort;
    }

    public void setKubePort(String kubePort) {
        this.kubePort = kubePort;
    }

    public String getKubePodLabelKey() {
        return kubePodLabelKey;
    }

    public void setKubePodLabelKey(String kubePodLabelKey) {
        this.kubePodLabelKey = kubePodLabelKey;
    }

    public String getKubePodLabelValue() {
        return kubePodLabelValue;
    }

    public void setKubePodLabelValue(String kubePodLabelValue) {
        this.kubePodLabelValue = kubePodLabelValue;
    }

}
