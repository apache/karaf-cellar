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
package org.apache.karaf.cellar.config.shell;

import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.config.shell.completers.ClusterConfigCompleter;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Map;
import java.util.Properties;

@Command(scope = "cluster", name = "config-property-list", description = "List the configurations in a cluster group")
@Service
public class PropListCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "pid", description = "The configuration PID", required = true, multiValued = false)
    @Completion(ClusterConfigCompleter.class)
    String pid;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exist
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

        if (clusterConfigurations != null && !clusterConfigurations.isEmpty()) {
            Properties properties = clusterConfigurations.get(pid);
            if (properties == null || properties.isEmpty()) {
                System.err.println("Configuration PID " + pid + " not found in cluster group " + groupName);
            } else {
                System.out.println(String.format("Property list for configuration PID " + pid + " for cluster group " + groupName));

                for (Object key : properties.keySet()) {
                    String value = properties.getProperty((String) key);
                    System.out.println("   " + key + " = " + value);
                }
            }
        } else System.err.println("No configuration found in cluster group " + groupName);

        return null;
    }

}
