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

import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.karaf.cellar.core.shell.completer.AllNodeCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Command(scope = "cluster", name = "group-list", description = "List the cluster groups")
@Service
public class GroupListCommand extends GroupSupport {

    @Argument(index = 0, name = "node", description = "The node(s) ID or alias", required = false, multiValued = true)
    @Completion(AllNodeCompleter.class)
    List<String> nodes;

    @Override
    protected Object doExecute() throws Exception {
        return doExecute(ManageGroupAction.LIST, null, null, nodes,false);
    }

}
