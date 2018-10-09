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
package org.apache.karaf.cellar.shell.consumer;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ConsumerSwitchCommand;
import org.apache.karaf.cellar.core.control.ConsumerSwitchResult;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.shell.ClusterCommandSupport;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic cluster event consumer shell command support.
 */
public abstract class ConsumerSupport extends ClusterCommandSupport {

    protected Object doExecute(List<String> nodeIdsOrAliases, SwitchStatus status) throws Exception {

        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());
        command.setTimeout(timeout * 1000);

        // looking for nodes and check if exist
        Set<Node> recipientList = new HashSet<Node>();
        if (nodeIdsOrAliases != null && !nodeIdsOrAliases.isEmpty()) {
            for (String nodeIdOrAlias : nodeIdsOrAliases) {
                Node node = clusterManager.findNodeByIdOrAlias(nodeIdOrAlias);
                if (node == null) {
                    System.err.println("Cluster node " + nodeIdOrAlias + " doesn't exist");
                } else {
                    recipientList.add(node);
                }
            }
        } else {
            if (status == null) {
                // in case of status display, select all nodes
                recipientList = clusterManager.listNodes();
            } else {
                // in case of status change, select only the local node
                recipientList.add(clusterManager.getNode());
            }
        }

        if (recipientList.size() < 1) {
            return null;
        }

        command.setDestination(recipientList);
        command.setStatus(status);

        Map<Node, ConsumerSwitchResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            System.out.println("No result received within given timeout");
        } else {
            ShellTable table = new ShellTable();
            table.column(" ");
            table.column("Node");
            table.column("Status");
            for (Node node : results.keySet()) {
                String local = "";
                if (node.equals(clusterManager.getNode())) {
                    local = "x";
                }
                ConsumerSwitchResult result = results.get(node);
                String statusString = "OFF";
                if (result.getStatus()) {
                    statusString = "ON";
                }
                String nodeName = node.getAlias();
                if (nodeName == null) {
                    nodeName = node.getId();
                }
                table.addRow().addContent(local, nodeName, statusString);
            }
            table.print(System.out);
        }
        return null;
    }

}
