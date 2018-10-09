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
package org.apache.karaf.cellar.shell.group;

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.karaf.cellar.core.control.ManageGroupCommand;
import org.apache.karaf.cellar.core.control.ManageGroupResult;
import org.apache.karaf.cellar.shell.ClusterCommandSupport;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generic cluster group shell command support.
 */
public abstract class GroupSupport extends ClusterCommandSupport {

    protected Object doExecute(ManageGroupAction action, String group, Group source, Collection<String> nodes) throws Exception {
        return doExecute(action, group, source, nodes, true);
    }

    /**
     * Executes the command.
     *
     * @param action the group action to perform.
     * @param group the cluster group name.
     * @param nodeIdsOrAliases the node IDs.
     * @param suppressOutput true to display command output, false else.
     * @return the Object resulting of the command execution.
     * @throws Exception in case of execution failure.
     */
    protected Object doExecute(ManageGroupAction action, String group, Group source, Collection<String> nodeIdsOrAliases, Boolean suppressOutput) throws Exception {

        ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
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
            recipientList.add(clusterManager.getNode());
        }

        if (recipientList.size() < 1) {
            return null;
        }

        command.setDestination(recipientList);
        command.setAction(action);

        if (group != null) {
            command.setGroupName(group);
        }

        if (source != null) {
            command.setSourceGroup(source);
        }

        Map<Node, ManageGroupResult> results = executionContext.execute(command);
        if (!suppressOutput) {
            if (results == null || results.isEmpty()) {
                System.out.println("No result received within given timeout");
            } else {
                ShellTable table = new ShellTable();
                table.column(" ");
                table.column("Group");
                table.column("Members");
                for (Node node : results.keySet()) {
                    ManageGroupResult result = results.get(node);
                    if (result != null && result.getGroups() != null) {
                        for (Group g : result.getGroups()) {
                            StringBuffer buffer = new StringBuffer();
                            if (g.getNodes() != null && !g.getNodes().isEmpty()) {
                                String local = "";
                                for (Node member : g.getNodes()) {
                                    // display only up and running nodes in the cluster
                                    if (clusterManager.findNodeById(member.getId()) != null) {
                                        if (member.getAlias() != null) {
                                            buffer.append(member.getAlias());
                                        } else {
                                            buffer.append(member.getId());
                                        }
                                        if (member.equals(clusterManager.getNode())) {
                                            local = "x";
                                            buffer.append("(x)");
                                        }
                                        buffer.append(" ");
                                    }
                                }
                                table.addRow().addContent(local, g.getName(), buffer.toString());
                            } else table.addRow().addContent("", g.getName(), "");
                        }
                    }
                }
                table.print(System.out);
            }
        }
        return null;
    }

}
