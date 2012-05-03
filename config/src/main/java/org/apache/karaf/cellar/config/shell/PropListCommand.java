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
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;

import java.util.Map;
import java.util.Properties;

/**
 * Config properties list cluster command.
 */
@Command(scope = "cluster", name = "config-proplist", description = "List the configuration PIDs assigned to a cluster group.")
public class PropListCommand extends CellarCommandSupport {

    protected static final String OUTPUT_FORMAT = "%-40s %s";

    @Argument(index = 0, name = "group", description = "The cluster group name.", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "pid", description = "The configuration PID.", required = true, multiValued = false)
    String pid;

    @Override
    protected Object doExecute() throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist.");
            return null;
        }

        Map<String, Properties> configurationMap = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

        if (configurationMap != null && !configurationMap.isEmpty()) {
            Properties properties = configurationMap.get(pid);
            if (properties == null || properties.isEmpty()) {
                System.err.println("No configuration PID found for group " + groupName);
            } else {
                System.out.println(String.format("Property list for configuration PID " + pid + " for group " + groupName));
                System.out.println(String.format(OUTPUT_FORMAT, "Key", "Value"));
                for (Object key : properties.keySet()) {
                    String value = properties.getProperty((String) key);
                    System.out.println(String.format(OUTPUT_FORMAT, key, value));
                }
            }
        } else System.err.println("No configuration PID found for group " + groupName);

        return null;
    }

}
