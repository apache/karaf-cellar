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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Set;

@Command(scope = "cluster", name = "shutdown", description = "Shutdown the cluster removing the Cellar feature on all node")
@Service
public class ShutdownCommand extends ClusterCommandSupport {

    @Option(name = "-p", aliases = {"--poweroff"}, description = "Completely stop the nodes", required = false, multiValued = false)
    boolean halt;

    @Override
    protected Object doExecute() throws Exception {
        org.apache.karaf.cellar.core.control.ShutdownCommand command = new org.apache.karaf.cellar.core.control.ShutdownCommand(clusterManager.generateId());
        Set<Node> recipientList = clusterManager.listNodes();
        command.setDestination(recipientList);
        command.setHalt(halt);
        System.out.println("!! Cluster shutdown !!");
        executionContext.execute(command);
        return null;
    }

}
