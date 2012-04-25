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
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.Map;

@Command(scope = "cluster", name = "feature-list", description = "List the features assigned to a cluster group.")
public class ListGroupFeatures extends FeatureCommandSupport {

    protected static final String OUTPUT_FORMAT = "%-40s %-20s %-4s ";

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

            Map<FeatureInfo, Boolean> features = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            if (features != null && !features.isEmpty()) {
                System.out.println(String.format("Features for group " + groupName));
                System.out.println(String.format(OUTPUT_FORMAT, "Name", "Version", "Status"));
                for (FeatureInfo info : features.keySet()) {
                    String name = info.getName();
                    String version = info.getVersion();
                    Boolean status = features.get(info);
                    if (version == null)
                        version = "";
                    System.out.println(String.format(OUTPUT_FORMAT, name, version, status));
                }
            } else System.err.println("No features found for group:" + groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

}
