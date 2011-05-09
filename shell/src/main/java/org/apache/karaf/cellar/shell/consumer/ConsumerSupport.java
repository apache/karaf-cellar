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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract consumer command support.
 */
public abstract class ConsumerSupport extends ClusterCommandSupport {

    protected static final String OUTPUT_FORMAT = "%-20s %s";

    /**
     * Executes the command.
     *
     * @return
     * @throws Exception
     */
    protected Object doExecute(List<String> nodes, SwitchStatus status) throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());
        Set<Node> recipientList = clusterManager.listNodes(nodes);

        if (recipientList != null && !recipientList.isEmpty()) {
            command.setDestination(recipientList);
        }

        command.setStatus(status);

        Map<Node, ConsumerSwitchResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            System.out.println("No result received within given timeout");
        } else {
            System.out.println(String.format(OUTPUT_FORMAT, "Node", "Status"));
            for (Node node : results.keySet()) {
                ConsumerSwitchResult result = results.get(node);
                System.out.println(String.format(OUTPUT_FORMAT, node.getId(), result.getStatus()));
            }
        }
        return null;
    }

}
