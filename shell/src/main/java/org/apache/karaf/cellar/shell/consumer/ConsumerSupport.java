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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract cluster event consumer shell command support.
 */
public abstract class ConsumerSupport extends ClusterCommandSupport {

    protected static final String HEADER_FORMAT = "   %-30s   %-5s";
    protected static final String OUTPUT_FORMAT = "%1s [%-30s] [%-5s]";

    protected Object doExecute(List<String> nodeIds, SwitchStatus status) throws Exception {

        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());

        // looking for nodes and check if exist
        Set<Node> recipientList;
        if (nodeIds != null && !nodeIds.isEmpty()) {
            recipientList = new HashSet<Node>();
            for (String nodeId : nodeIds) {
                Node node = clusterManager.findNodeById(nodeId);
                if (node == null) {
                    System.err.println("Cluster node " + nodeId + " doesn't exist");
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
                recipientList = new HashSet<Node>();
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
            System.out.println(String.format(HEADER_FORMAT, "Node", "Status"));
            for (Node node : results.keySet()) {
                String local = " ";
                if (node.equals(clusterManager.getNode())) {
                    local = "*";
                }
                ConsumerSwitchResult result = results.get(node);
                String statusString = "OFF";
                if (result.getStatus()) {
                    statusString = "ON";
                }
                System.out.println(String.format(OUTPUT_FORMAT, local, node.getId(), statusString));
            }
        }
        return null;
    }

}
