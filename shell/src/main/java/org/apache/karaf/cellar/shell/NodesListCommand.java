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
package org.apache.karaf.cellar.shell;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.shell.commands.Command;

import java.util.Set;

@Command(scope = "cluster", name = "node-list", description = "List the nodes in the cluster.")
public class NodesListCommand extends ClusterCommandSupport {

    private static final String LIST_FORMAT = "%1s %4s %-20s %5s %s";

    @Override
    protected Object doExecute() throws Exception {
        if (clusterManager == null) {
            System.err.println("Cluster Manager not found!");
            return null;
        } else {
            Set<Node> nodes = clusterManager.listNodes();
            if (nodes != null && !nodes.isEmpty()) {
                int count = 1;
                System.out.println(String.format(LIST_FORMAT, " ", "No.", "Host Name", "Port", "ID"));
                for (Node node : nodes) {
                    String mark = " ";
                    if (node.equals(clusterManager.getNode()))
                        mark = "*";
                    System.out.println(String.format(LIST_FORMAT, mark, count++, node.getHost(), node.getPort(), node.getId()));
                }
            } else {
                System.err.println("No node found in the cluster");
                return null;
            }
        }
        return null;
    }

}
