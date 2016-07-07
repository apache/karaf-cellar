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
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.Set;

@Command(scope = "cluster", name = "node-list", description = "List the nodes in the cluster")
@Service
public class NodesListCommand extends ClusterCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        Set<Node> nodes = clusterManager.listNodes();
        if (nodes != null && !nodes.isEmpty()) {
            ShellTable table = new ShellTable();
            table.column(" ");
            table.column("Id");
            table.column("Alias");
            table.column("Host Name");
            table.column("Port");
            for (Node node : nodes) {
                String local = "";
                if (node.equals(clusterManager.getNode()))
                    local = "x";
                table.addRow().addContent(local, node.getId(), node.getAlias(), node.getHost(), node.getPort());
            }
            table.print(System.out);
        } else {
            System.err.println("No node found in the cluster");
        }
        return null;
    }

}
