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
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Group;

import java.util.Map;
import java.util.Properties;

/**
 * List cluster config command.
 */
@Command(scope = "cluster", name = "config-list", description = "List the configuration PIDs assigned to a group.")
public class ListCommand extends ConfigCommandSupport {

    protected static final String OUTPUT_FORMAT = "%-40s";

    @Argument(index = 0, name = "group", description = "The cluster group name.", required = true, multiValued = false)
    String groupName;

    @Override
    protected Object doExecute() throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist.");
            return null;
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            if (configurationTable != null && !configurationTable.isEmpty()) {
                System.out.println(String.format("Configuration PIDs for group " + groupName));
                System.out.println(String.format(OUTPUT_FORMAT, "PID"));
                for (String pid : configurationTable.keySet()) {
                    System.out.println(String.format(OUTPUT_FORMAT, pid));
                }
            } else System.err.println("No configuration PID found for group " + groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);

        }
        return null;
    }

}
