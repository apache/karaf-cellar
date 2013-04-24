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
package org.apache.karaf.cellar.features.shell;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.List;

@Command(scope = "cluster", name = "feature-url-list", description = "List the features repository URLs in a cluster group")
public class UrlListCommand extends FeatureCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group  exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // get the features repositories in the cluster group
        List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);

        for (String repository : clusterRepositories) {
            System.out.println(repository);
        }

        return null;
    }

}
