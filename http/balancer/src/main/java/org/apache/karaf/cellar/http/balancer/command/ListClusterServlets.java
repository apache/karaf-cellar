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
package org.apache.karaf.cellar.http.balancer.command;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.cellar.http.balancer.Constants;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.List;
import java.util.Map;

@Command(scope = "cluster", name = "http-list", description = "List the HTTP servlets on the cluster")
@Service
public class ListClusterServlets extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    private String groupName;

    @Override
    public Object doExecute() throws Exception {

        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        Map<String, List<String>> clusterServlets = clusterManager.getMap(Constants.BALANCER_MAP + Configurations.SEPARATOR + groupName);

        ShellTable table = new ShellTable();
        table.column("Alias");
        table.column("Locations");

        for (String alias : clusterServlets.keySet()) {
            List<String> locations = clusterServlets.get(alias);
            StringBuilder builder = new StringBuilder();
            for (String location : locations) {
                builder.append(location).append(" ");
            }
            table.addRow().addContent(alias, builder.toString());
        }

        table.print(System.out);

        return null;
    }

}
