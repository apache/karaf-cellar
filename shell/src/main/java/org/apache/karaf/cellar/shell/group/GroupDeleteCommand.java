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

package org.apache.karaf.cellar.shell.group;

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import java.util.LinkedList;
import java.util.List;

/**
 * @author iocanel
 */
@Command(scope = "cluster", name = "group-delete", description = "Deletes a group")
public class GroupDeleteCommand extends GroupSupport {


    @Argument(index = 0, name = "group", description = "The name of the group to delete", required = false, multiValued = false)
    String group;

    /**
     * Execute the command.
     *
     * @return
     * @throws Exception
     */
    @Override
    protected Object doExecute() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Group g = groupManager.findGroupByName(group);
            List<String> nodes = new LinkedList<String>();

            if (g.getMembers() != null && !g.getMembers().isEmpty()) {
                for (Node n : g.getMembers()) {
                    nodes.add(n.getId());
                }
                doExecute(ManageGroupAction.QUIT, group, nodes);
            }

            groupManager.deleteGroup(group);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }
}
