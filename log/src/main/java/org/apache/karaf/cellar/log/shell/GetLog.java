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
package org.apache.karaf.cellar.log.shell;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllNodeCompleter;
import org.apache.karaf.cellar.log.GetLogCommand;
import org.apache.karaf.cellar.log.GetLogResult;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(scope = "cluster", name = "log-get", description = "Show the current set log loggers")
@Service
public class GetLog extends CellarCommandSupport {

    @Argument(index = 0, name = "logger", description = "The name of the logger, ALL or ROOT (default)", required = false, multiValued = false)
    String logger;

    @Argument(index = 1, name = "node", description = "The node(s) ID or alias", required = false, multiValued = true)
    @Completion(AllNodeCompleter.class)
    List<String> nodes;

    @Option(name = "-t", aliases = { "--timeout" }, description = "Consumer command timeout (in seconds)", required = false, multiValued = false)
    protected long timeout = 30;

    @Reference
    protected ExecutionContext executionContext;

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public Object doExecute() throws Exception {
        GetLogCommand command = new GetLogCommand(clusterManager.generateId());
        command.setTimeout(timeout * 1000);

        ShellTable table = new ShellTable();
        table.column(" ");
        table.column("Node");
        table.column("Logger");
        table.column("Level");

        Set<Node> recipientList = new HashSet<Node>();
        if (nodes != null && !nodes.isEmpty()) {
            for (String nodeId : nodes) {
                Node node = clusterManager.findNodeByIdOrAlias(nodeId);
                if (node == null) {
                    System.err.println("Node " + nodeId + " doesn't exist");
                } else {
                    recipientList.add(node);
                }
            }
        } else {
            recipientList = clusterManager.listNodes();
        }

        if (recipientList.size() < 1)
            return null;

        command.setDestination(recipientList);
        command.setLogger(logger);

        Map<Node, GetLogResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            System.err.println("No result received within given timeout");
        } else {
            for (Node node : results.keySet()) {
                String local = "";
                if (node.equals(clusterManager.getNode()))
                    local = "x";
                GetLogResult result = results.get(node);
                Map<String, String> loggers = result.getLoggers();
                for (String logger : loggers.keySet()) {
                    String nodeName = node.getAlias();
                    if (nodeName == null) {
                        nodeName = node.getId();
                    }
                    table.addRow().addContent(local, nodeName, logger, loggers.get(logger));
                }
            }
        }

        table.print(System.out);

        return null;
    }

}
