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
package org.apache.karaf.cellar.bundle.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.osgi.framework.BundleEvent;

import java.util.Map;

@Command(scope = "cluster", name = "bundle-list", description = "List the bundles assigned to a cluster group.")
public class ListBundleCommand extends CellarCommandSupport {

    protected static final String HEADER_FORMAT = " %-11s  %s";
    protected static final String OUTPUT_FORMAT = "[%-11s] %s";

    @Argument(index = 0, name = "group", description = "The cluster group name.", required = true, multiValued = false)
    String groupName;

    @Option(name = "-l", aliases = {}, description = "Show the locations", required = false, multiValued = false)
    boolean showLoc;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist.");
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            if (bundles != null && !bundles.isEmpty()) {
                System.out.println(String.format("Bundles for cluster group " + groupName));
                System.out.println(String.format(HEADER_FORMAT, "State", "Name"));
                for (String bundle : bundles.keySet()) {
                    String[] tokens = bundle.split("/");
                    String name = null;
                    String version = null;
                    if (tokens.length == 2) {
                        name = tokens[0];
                        version = tokens[1];
                    } else {
                        name = bundle;
                        version = "";
                    }
                    BundleState state = bundles.get(bundle);
                    String status;
                    switch (state.getStatus()) {
                        case BundleEvent.INSTALLED:
                            status = "Installed";
                            break;
                        case BundleEvent.RESOLVED:
                            status = "Resolved";
                            break;
                        case BundleEvent.STARTED:
                            status = "Active";
                            break;
                        case BundleEvent.STARTING:
                            status = "Starting";
                            break;
                        case BundleEvent.STOPPED:
                            status = "Resolved";
                            break;
                        case BundleEvent.STOPPING:
                            status = "Stopping";
                            break;
                        case BundleEvent.UNINSTALLED:
                            status = "Uninstalled";
                            break;
                        default:
                            status = "";
                            break;
                    }
                    if (showLoc) {
                        System.out.println(String.format(OUTPUT_FORMAT, status, state.getLocation()));
                    } else {
                        System.out.println(String.format(OUTPUT_FORMAT, status, name + " (" + version + ")"));
                    }
                }
            } else {
                System.err.println("No bundles found for cluster group " + groupName);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

}
