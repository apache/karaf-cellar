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
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureState;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.table.ShellTable;

import java.util.*;

@Command(scope = "cluster", name = "feature-list", description = "List the features in a cluster group")
public class ListFeaturesCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Option(name = "-i", aliases = { "--installed" }, description = "Display only installed features", required = false, multiValued = false)
    boolean installed;

    @Option(name = "-o", aliases = { "--ordered" }, description = "Display a list using alphabetical order", required = false, multiValued = false)
    boolean ordered;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Override
    protected Object doExecute() throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);
;
            if (clusterFeatures != null && !clusterFeatures.isEmpty()) {

                ShellTable table = new ShellTable();
                table.column("Name");
                table.column("Version");
                table.column("Installed");

                List<FeatureState> featureStates = new ArrayList<FeatureState>(clusterFeatures.values());
                if (ordered) {
                    Collections.sort(featureStates, new FeatureComparator());
                }
                for (FeatureState info : featureStates) {

                    String name = info.getName();
                    String version = info.getVersion();
                    boolean isInstalled = info.isInstalled();
                    if (version == null)
                        version = "";
                    if (!installed || (installed && isInstalled)) {
                        table.addRow().addContent(
                                name,
                                version,
                                isInstalled ? "x" : "");
                    }
                }

                table.print(System.out, !noFormat);
            } else System.err.println("No features in cluster group " + groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

    class FeatureComparator implements Comparator<FeatureState> {
        public int compare(FeatureState f1, FeatureState f2) {
            return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    }

}
