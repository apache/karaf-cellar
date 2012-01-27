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
import org.apache.karaf.cellar.shell.ClusterCommandSupport;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class HandlersSupport extends ClusterCommandSupport {

    protected static final String OUTPUT_FORMAT = "%-20s %-7s %s";

    /**
     * Executes the command.
     *
     * @return
     * @throws Exception
     */
    protected Object doExecute(String handlerName, List<String> nodes, Boolean status) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());
        Set<Node> recipientList = clusterManager.listNodes(nodes);

        //Set the recipient list
        if (recipientList != null && !recipientList.isEmpty()) {
            command.setDestination(recipientList);
        }

        command.setHandlesName(handlerName);
        command.setStatus(status);

        Map<Node, ManageHandlersResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            System.out.println("No result received within given timeout");
        } else {
            System.out.println(String.format(OUTPUT_FORMAT, "Node", "Status", "Event Handler"));
            for (Map.Entry<Node,ManageHandlersResult> handlersResultEntry : results.entrySet()) {
                Node node = handlersResultEntry.getKey();
                ManageHandlersResult result = handlersResultEntry.getValue();
                if (result != null && result.getHandlers() != null) {

                    for (Map.Entry<String,String>  handlerEntry: result.getHandlers().entrySet()) {
                        String handler =  handlerEntry.getKey();
                        String s = handlerEntry.getValue();
                        System.out.println(String.format(OUTPUT_FORMAT, node.getId(), s, handler));
                    }
                }
            }
        }
        return null;
    }

}
