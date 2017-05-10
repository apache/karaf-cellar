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

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.Bundle;

import java.util.*;

@Command(scope = "cluster", name = "bundle-list", description = "List the bundles in a cluster group")
@Service
public class ListBundleCommand extends BundleCommandSupport {

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
            Map<String, ExtendedBundleState> allBundles = gatherBundles(false);
            if (allBundles != null && !allBundles.isEmpty()) {
                System.out.println(String.format("Bundles in cluster group " + groupName));

                ShellTable table = new ShellTable();
                table.column("ID").alignRight();
                table.column("State");
                table.column("Lvl");
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

                if (ids != null && !ids.isEmpty()) {
                    // do filtering by ids
                    Set<String> matchingBundles = new HashSet<String>(selector(allBundles));
                    for (Iterator<String> bundles = allBundles.keySet().iterator(); bundles.hasNext();) {
                        if (!matchingBundles.contains(bundles.next())) {
                            bundles.remove();
                        }
                    }
                }
                List<ExtendedBundleState> bundles = new ArrayList<ExtendedBundleState>(allBundles.values());
                Collections.sort(bundles, new BundleStateComparator());

                for (ExtendedBundleState bundle : bundles) {
                    String status;
                    switch (bundle.getStatus()) {
                        case Bundle.INSTALLED:
                            status = "Installed";
                            break;
                        case Bundle.RESOLVED:
                            status = "Resolved";
                            break;
                        case Bundle.ACTIVE:
                            status = "Active";
                            break;
                        case Bundle.STARTING:
                            status = "Starting";
                            break;
                        case Bundle.STOPPING:
                            status = "Stopping";
                            break;
                        case Bundle.UNINSTALLED:
                            status = "Uninstalled";
                            break;
                        default:
                            status = "";
                            break;
                    }

                    String located = "";
                    boolean cluster = bundle.isCluster();
                    boolean local = bundle.isLocal();
                    if (cluster && local)
                        located = "cluster/local";
                    if (cluster && !local) {
                        located = "cluster";
                        if (onlyLocal) {
                            continue;
                        }
                    }
                    if (local && !cluster) {
                        located = "local";
                        if (onlyCluster) {
                            continue;
                        }
                    }

                    String blocked = "";
                    boolean inbound = support.isAllowed(group, Constants.CATEGORY, bundle.getLocation(), EventType.INBOUND);
                    boolean outbound = support.isAllowed(group, Constants.CATEGORY, bundle.getLocation(), EventType.OUTBOUND);
                    if (inbound && outbound && onlyBlocked) {
                        continue;
                    }
                    if (!inbound && !outbound)
                        blocked = "in/out";
                    if (!inbound && outbound)
                        blocked = "in";
                    if (outbound && !inbound)
                        blocked = "out";

                    if (showLocation) {
                        table.addRow().addContent(bundle.getId(), status, bundle.getStartLevel(), located, blocked, bundle.getVersion(), bundle.getLocation());
                    } else {
                        if (showSymbolicName) {
                            table.addRow().addContent(bundle.getId(), status, bundle.getStartLevel(), located, blocked, bundle.getVersion(), bundle.getSymbolicName());
                        } else {
                            table.addRow().addContent(bundle.getId(), status, bundle.getStartLevel(), located, blocked, bundle.getVersion(), bundle.getName());
                        }
                    }
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

    class BundleStateComparator implements Comparator<BundleState> {
        public int compare(BundleState b1, BundleState b2) {
            return (int) (b1.getId() - b2.getId());
        }
    }

}
