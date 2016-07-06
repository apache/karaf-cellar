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
import org.apache.karaf.cellar.log.SetLogCommand;
import org.apache.karaf.cellar.log.SetLogResult;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Command(scope = "cluster", name = "log-set", description = "Set the log level")
@Service
public class SetLog extends CellarCommandSupport {

    @Argument(index = 0, name = "level", description = "The log level to set (TRACE, DEBUG, INFO, WARN, ERROR) or DEFAULT to unset", required = true, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "DEFAULT" })
    String level;

    @Argument(index = 1, name = "logger", description = "Logger name or ROOT (default)", required = false, multiValued = false)
    String logger;

    @Argument(index = 2, name = "node", description = "The node(s) ID or alias", required = false, multiValued = true)
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
        SetLogCommand command = new SetLogCommand(clusterManager.generateId());
        command.setTimeout(timeout * 1000);

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
        command.setLevel(level);

        Map<Node, SetLogResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            System.err.println("No result received within given timeout");
        }

        return null;
    }

}
