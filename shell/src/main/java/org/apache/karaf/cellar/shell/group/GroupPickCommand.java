/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.shell.group;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupAction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Command(scope = "cluster", name = "group-pick", description = "Pick a number of nodes from one cluster group and moves them into another.")
public class GroupPickCommand extends GroupSupport {

    @Argument(index = 0, name = "source", description = "The source cluster group name that will act as a selection pool.", required = true, multiValued = false)
    String sourceGroupName;

    @Argument(index = 1, name = "destination", description = "The destination cluster group name.", required = true, multiValued = false)
    String targetGroupName;

    @Argument(index = 2, name = "count", description = "The number of nodes to transfer.", required = false, multiValued = false)
    int count = 1;

    @Override
    protected Object doExecute() throws Exception {
        Group sourceGroup = groupManager.findGroupByName(sourceGroupName);
        if (sourceGroup != null) {
            Set<Node> groupMembers = sourceGroup.getNodes();

            for (Node node : groupMembers) {
                List<String> recipient = new LinkedList<String>();
                recipient.add(node.getId());
                doExecute(ManageGroupAction.SET, targetGroupName, sourceGroup, recipient);
            }

            doExecute(ManageGroupAction.LIST, null, null, new ArrayList(), false);

        } else System.err.println("Cannot find source group with name: " + sourceGroupName);
        return null;
    }

}
