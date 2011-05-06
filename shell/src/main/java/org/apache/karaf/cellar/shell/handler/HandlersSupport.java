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

package org.apache.karaf.cellar.shell.handler;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageHandlersCommand;
import org.apache.karaf.cellar.core.control.ManageHandlersResult;
import org.apache.karaf.cellar.shell.ClusterCommandSuppot;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author iocanel
 */
public abstract class HandlersSupport extends ClusterCommandSuppot {

    protected static final String OUTPUT_FORMAT = "%-20s %-7s %s";

    /**
     * Execute the command.
     *
     * @return
     * @throws Exception
     */
    protected Object doExecute(String handler, List<String> nodes, Boolean status) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());
        Set<Node> recipientList = clusterManager.listNodes(nodes);

        //Set the recipient list
        if (recipientList != null && !recipientList.isEmpty()) {
            command.setDestination(recipientList);
        }

        //Set the name of the handler.
        if (handler != null && handler.length() > 2) {
            handler = handler.substring(1);
            handler = handler.substring(0, handler.length() - 1);
            command.setHandlesName(handler);
        }

        command.setStatus(status);


        Map<Node, ManageHandlersResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            System.out.println("No result received within given timeout");
        } else {
            System.out.println(String.format(OUTPUT_FORMAT, "Node", "Status", "Event Handler"));
            for (Node node : results.keySet()) {
                ManageHandlersResult result = results.get(node);
                if (result != null && result.getHandlers() != null) {
                    for (String h : result.getHandlers().keySet()) {
                        String s = result.getHandlers().get(h);
                        System.out.println(String.format(OUTPUT_FORMAT, node.getId(), s, handler));
                    }
                }
            }
        }
        return null;
    }
}
