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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Command(scope = "cluster", name = "group-pick", description = "Picks a number of nodes from one cluster group and moves them into another")
public class GroupPickCommand extends GroupSupport {

    @Argument(index = 0, name = "sourceGroupName", description = "The source cluster group name", required = true, multiValued = false)
    String sourceGroupName;

    @Argument(index = 1, name = "targetGroupName", description = "The destination cluster group name", required = true, multiValued = false)
    String targetGroupName;

    @Argument(index = 2, name = "count", description = "The number of nodes to transfer", required = false, multiValued = false)
    int count = 1;

    @Override
    protected Object doExecute() throws Exception {
        Group sourceGroup = groupManager.findGroupByName(sourceGroupName);
        if (sourceGroup == null) {
            System.err.println("Source cluster group " + sourceGroupName + " doesn't exist");
            return null;
        }
        Group targetGroup = groupManager.findGroupByName(targetGroupName);
        if (targetGroup == null) {
            System.err.println("Target cluster group " + targetGroupName + " doesn't exist");
            return null;
        }

        Set<Node> groupMembers = sourceGroup.getNodes();

        if (count > groupMembers.size())
            count = groupMembers.size();

        int i = 0;
        for (Node node : groupMembers) {
            if (i >= count)
                break;
            List<String> recipients = new LinkedList<String>();
            recipients.add(node.getId());
            doExecute(ManageGroupAction.SET, targetGroupName, sourceGroup, recipients);
            i++;
        }

        return doExecute(ManageGroupAction.LIST, null, null, new ArrayList(), false);
    }

}
