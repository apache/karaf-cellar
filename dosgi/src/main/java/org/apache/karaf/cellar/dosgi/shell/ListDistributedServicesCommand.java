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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Command(scope = "cluster", name = "service-list", description = "List the services available on the cluster")
@Service
public class ListDistributedServicesCommand extends CellarCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Node localNode = clusterManager.getNode();
            Map<String, EndpointDescription> remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);

            if (remoteEndpoints != null && !remoteEndpoints.isEmpty()) {
                ShellTable table = new ShellTable();
                table.column("Service Class");
                table.column("Version");
                table.column("Consumer");
                table.column("Provider");
                table.column("Node");
                table.column(" ");
                table.column("Filter");
                BundleContext bundleContext = FrameworkUtil.getBundle(ListDistributedServicesCommand.class).getBundleContext();
                long dosgiBundleId = bundleContext.getBundle().getBundleId();
                Map.Entry<String, EndpointDescription>[] entrySet = remoteEndpoints.entrySet().toArray(new Map.Entry[remoteEndpoints.size()]);
                Arrays.sort(entrySet, new Comparator<Map.Entry<String, EndpointDescription>>() {
                    @Override
                    public int compare(Map.Entry<String, EndpointDescription> a, Map.Entry<String, EndpointDescription> b) {
                        return a.getKey().compareTo(b.getKey());
                    }
                });
                for (Map.Entry<String, EndpointDescription> entry : entrySet) {
                    EndpointDescription endpointDescription = entry.getValue();
                    String version = endpointDescription.getVersion();
                    String filter = endpointDescription.getFilter();
                    String serviceClass = endpointDescription.getServiceClass();
                    ServiceReference[] serviceReferences = bundleContext.getServiceReferences(serviceClass, filter);

                    Node[] nodes = endpointDescription.getNodes().toArray(new Node[endpointDescription.getNodes().size()]);
                    Arrays.sort(nodes, new Comparator<Node>() {
                        @Override
                        public int compare(Node a, Node b) {
                            return (a.getHost() + a.getPort()).compareTo(b.getHost() + b.getPort());
                        }
                    });

                    Set<Long> consumerBundleIds = new HashSet();
                    boolean hasNodeProvider = false;
                    boolean hasRemoteProvider = false;

                    for (Node node : nodes) {
                        String providerNode = node.getAlias();
                        if (providerNode == null) {
                            providerNode = node.getId();
                        }
                        Set<Long> providerBundleIds = new HashSet();
                        if (node.equals(localNode)) {
                            if (serviceReferences != null) {
                                for (ServiceReference serviceReference : serviceReferences) {
                                    providerBundleIds.add(serviceReference.getBundle().getBundleId());
                                    hasNodeProvider = true;
                                    Bundle[] bundles = serviceReference.getUsingBundles();
                                    if (bundles != null) {
                                        for (Bundle bundle : bundles) {
                                            consumerBundleIds.add(bundle.getBundleId());
                                        }
                                    }
                                }
                            }
                            String consumerBundleIdsString = consumerBundleIds.toString().replaceAll("^\\[|]$", "");
                            String providerBundleIdsString = providerBundleIds.toString().replaceAll("^\\[|]$", "");
                            table.addRow().addContent(serviceClass, version, consumerBundleIdsString, providerBundleIdsString, providerNode, "x", filter);
                        } else {
                            if (serviceReferences != null) {
                                for (ServiceReference serviceReference : serviceReferences) {
                                    providerBundleIds.add(dosgiBundleId);
                                    hasRemoteProvider = true;
                                    Bundle[] bundles = serviceReference.getUsingBundles();
                                    if (bundles != null) {
                                        for (Bundle bundle : bundles) {
                                            if (hasNodeProvider && hasRemoteProvider) {
                                                consumerBundleIds.remove(bundle.getBundleId());
                                            } else {
                                                consumerBundleIds.add(bundle.getBundleId());
                                            }
                                        }
                                    }
                                }
                                consumerBundleIds.remove(dosgiBundleId);
                            }
                            String consumerBundleIdsString = consumerBundleIds.toString().replaceAll("^\\[|]$", "");
                            String providerBundleIdsString = providerBundleIds.toString().replaceAll("^\\[|]$", "");
                            table.addRow().addContent(serviceClass, version, consumerBundleIdsString, providerBundleIdsString, providerNode, "", filter);
                        }
                    }
                }
                table.print(System.out);
            } else {
                System.out.println("No remote service available on the cluster");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

}