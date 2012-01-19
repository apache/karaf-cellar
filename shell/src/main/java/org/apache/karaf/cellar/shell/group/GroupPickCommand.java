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

@Command(scope = "cluster", name = "group-pick", description = "Picks a number of nodes from one group and moves them into an other")
public class GroupPickCommand extends GroupSupport {

    @Argument(index = 0, name = "sourceGroupName", description = "The name of the source group that will act as a selection pool", required = true, multiValued = false)
    String sourceGroupName;

    @Argument(index = 1, name = "targetGroupName", description = "The name of the the destination group", required = true, multiValued = false)
    String targetGroupName;

    @Argument(index = 2, name = "count", description = "The number of nodes to transfer", required = false, multiValued = false)
    int count = 1;

    @Override
    protected Object doExecute() throws Exception {
        Group sourceGroup = groupManager.findGroupByName(sourceGroupName);
        if (sourceGroup != null) {
            List<String> eligibleMembers = new LinkedList<String>();
            Set<Node> groupMembers = sourceGroup.getNodes();

            for (Node node : groupMembers) {
                Set<Group> nodeGroups = groupManager.listGroups(node);
                //If the node only belongs to the source group then it is eligible.
                if (nodeGroups != null && nodeGroups.size() == 1) {
                    eligibleMembers.add(node.getId());
                }
            }

            if (eligibleMembers.size() == 0) {
                System.out.println("Could not find eligible members from transfer in group:" + sourceGroupName);
            } else if (eligibleMembers.size() < count) {
                System.out.println("There are fewer(" + eligibleMembers.size() + ") eligible members for transfer in group:" + sourceGroupName);
            }

            //TODO: The loop should not be necessary since the method already accepts a list. However this is breaks for some reason.
            for (String eligible : eligibleMembers) {
                List<String> recipient = new LinkedList<String>();
                recipient.add(eligible);
                doExecute(ManageGroupAction.SET, targetGroupName, recipient);
            }

            doExecute(ManageGroupAction.LIST, null, new ArrayList(), false);

        } else System.err.println("Cannot find source group with name:" + sourceGroupName);
        return null;
    }

}
