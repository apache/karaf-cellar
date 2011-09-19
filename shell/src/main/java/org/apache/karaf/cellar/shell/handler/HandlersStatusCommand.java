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

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import java.util.List;

/**
 * Handlers status command.
 */
@Command(scope = "cluster", name = "handler-status", description = "Status of a given event handler")
public class HandlersStatusCommand extends HandlersSupport {

    @Argument(index = 0, name = "handler-start", description = "The ID of the event handler", required = false, multiValued = false)
    String handler;

    @Argument(index = 1, name = "node", description = "The ID of the node(s)", required = false, multiValued = true)
    List<String> nodes;

    /**
     * Executes the command.
     *
     * @return
     * @throws Exception
     */
    @Override
    protected Object doExecute() throws Exception {
        return doExecute(handler, nodes, null);
    }

}
