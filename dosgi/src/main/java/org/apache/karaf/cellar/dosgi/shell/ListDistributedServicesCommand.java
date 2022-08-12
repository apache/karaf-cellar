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
package org.apache.karaf.cellar.dosgi.shell;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.dosgi.Constants;
import org.apache.karaf.cellar.dosgi.EndpointDescription;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

@Command(scope = "cluster", name = "service-list", description = "List the services available on the cluster")
@Service
public class ListDistributedServicesCommand extends CellarCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Map<String, EndpointDescription> remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
            if (remoteEndpoints != null && !remoteEndpoints.isEmpty()) {
                ShellTable table = new ShellTable();
                table.column("Service Class");
                table.column("Provider Node");
                table.column("Endpoint ID");
                Map.Entry<String, EndpointDescription>[] entrySet = remoteEndpoints.entrySet().toArray(new Map.Entry[0]);
                Arrays.sort(entrySet, new Comparator<Map.Entry<String, EndpointDescription>>() {
                    @Override
                    public int compare(Map.Entry<String, EndpointDescription> a, Map.Entry<String, EndpointDescription> b) {
                        return a.getKey().compareTo(b.getKey());
                    }
                });
                for (Map.Entry<String, EndpointDescription> entry : entrySet) {
                    EndpointDescription endpointDescription = entry.getValue();
                    String serviceClass = endpointDescription.getServiceClass();
                    String endpointId = endpointDescription.getId();
                    Node[] nodes = endpointDescription.getNodes().toArray(new Node[0]);
                    Arrays.sort(nodes, new Comparator<Node>() {
                        @Override
                        public int compare(Node a, Node b) {
                            return (a.getHost() + a.getPort()).compareTo(b.getHost() + b.getPort());
                        }
                    });
                    for (Node node : nodes) {
                        String nodeName = node.getAlias();
                        if (nodeName == null) {
                            nodeName = node.getId();
                        }
                        table.addRow().addContent(serviceClass, nodeName, endpointId);
                    }
                }
                table.print(System.out);
            } else {
                System.out.println("No service available on the cluster");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

}
