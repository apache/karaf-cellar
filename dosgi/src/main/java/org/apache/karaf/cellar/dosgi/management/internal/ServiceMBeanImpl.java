/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.dosgi.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.dosgi.Constants;
import org.apache.karaf.cellar.dosgi.EndpointDescription;
import org.apache.karaf.cellar.dosgi.management.ServiceMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.*;

public class ServiceMBeanImpl extends StandardMBean implements ServiceMBean {

    private ClusterManager clusterManager;

    public ServiceMBeanImpl() throws NotCompliantMBeanException {
        super(ServiceMBean.class);
    }

    public Map<String, List<String>> getServices() {
        Map<String, List<String>> services = new HashMap<String, List<String>>();
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Map<String, EndpointDescription> remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
            if (remoteEndpoints != null && !remoteEndpoints.isEmpty()) {
                for (Map.Entry<String, EndpointDescription> entry : remoteEndpoints.entrySet()) {
                    EndpointDescription endpointDescription = entry.getValue();
                    String serviceClass = endpointDescription.getServiceClass();
                    Set<Node> nodes = endpointDescription.getNodes();
                    LinkedList<String> providers = new LinkedList<String>();
                    for (Node node : nodes) {
                        providers.add(node.getId());
                    }
                    services.put(serviceClass, providers);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return services;
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

}
