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

import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.table.ShellTable;
import org.osgi.framework.BundleEvent;

import java.util.Map;

@Command(scope = "cluster", name = "bundle-list", description = "List the bundles in a cluster group")
public class ListBundleCommand extends  BundleCommandSupport {

    @Option(name = "-s", aliases = {}, description = "Shows the symbolic name", required = false, multiValued = false)
    boolean showSymbolicName;

    @Option(name = "-l", aliases = {}, description = "Shows the location", required = false, multiValued = false)
    boolean showLocation;

    @Option(name = "--cluster", description = "Shows only bundles on the cluster", required = false, multiValued = false)
    boolean onlyCluster;

    @Option(name = "--local", description = "Shows only bundles on the local node", required = false, multiValued = false)
    boolean onlyLocal;

    @Option(name = "--blocked", description = "Shows only blocked bundles", required = false, multiValued = false)
    boolean onlyBlocked;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(clusterManager);
        support.setGroupManager(groupManager);
        support.setConfigurationAdmin(configurationAdmin);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            Map<String, ExtendedBundleState> bundles = gatherBundles();
            if (bundles != null && !bundles.isEmpty()) {
                System.out.println(String.format("Bundles in cluster group " + groupName));

                ShellTable table = new ShellTable();
                table.column("ID").alignRight();
                table.column("State");
                table.column("Located");
                table.column("Blocked");
                table.column("Version");
                if (showLocation) {
                    table.column("Location");
                } else if (showSymbolicName) {
                    table.column("Symbolic Name");
                } else {
                    table.column("Name");
                }

                int id = 0;
                for (String bundle : bundles.keySet()) {
                    String[] tokens = bundle.split("/");
                    String symbolicName = null;
                    String version = null;
                    if (tokens.length == 2) {
                        symbolicName = tokens[0];
                        version = tokens[1];
                    } else {
                        symbolicName = bundle;
                        version = "";
                    }
                    ExtendedBundleState state = bundles.get(bundle);
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

                    String located = "";
                    boolean cluster = state.isCluster();
                    boolean local = state.isLocal();
                    if (cluster && local)
                        located = "cluster/local";
                    if (cluster && !local) {
                        located = "cluster";
                        if (onlyLocal) {
                            id++;
                            continue;
                        }
                    }
                    if (local && !cluster) {
                        located = "local";
                        if (onlyCluster) {
                            id++;
                            continue;
                        }
                    }

                    String blocked = "";
                    boolean inbound = support.isAllowed(group, Constants.CATEGORY, state.getLocation(), EventType.INBOUND);
                    boolean outbound = support.isAllowed(group, Constants.CATEGORY, state.getLocation(), EventType.OUTBOUND);
                    if (inbound && outbound && onlyBlocked) {
                        id++;
                        continue;
                    }
                    if (!inbound && !outbound)
                        blocked = "in/out";
                    if (!inbound && outbound)
                        blocked = "in";
                    if (outbound && !inbound)
                        blocked = "out";

                    if (showLocation) {
                        table.addRow().addContent(id, status, located, blocked, version, state.getLocation());
                    } else {
                        if (showSymbolicName) {
                            table.addRow().addContent(id, status, located, blocked, version, symbolicName);
                        } else {
                            table.addRow().addContent(id, status, located, blocked, version, state.getName());
                        }
                    }
                    id++;
                }

                table.print(System.out);
            } else {
                System.err.println("No bundle found in cluster group " + groupName);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

}
