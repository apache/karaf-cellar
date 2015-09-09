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

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Set;

@Command(scope = "cluster", name = "feature-block", description = "Change the blocking policy for a feature")
@Service
public class BlockCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "featurePattern", description = "The feature pattern to block.", required = false, multiValued = false)
    String featurePattern;

    @Option(name = "-in", description = "Update the inbound direction", required = false, multiValued = false)
    boolean in = false;

    @Option(name = "-out", description = "Update the outbound direction", required = false, multiValued = false)
    boolean out = false;

    @Option(name = "-whitelist", description = "Allow the feature by updating the whitelist", required = false, multiValued = false)
    boolean whitelist = false;

    @Option(name = "-blacklist", description = "Block the feature by updating the blacklist", required = false, multiValued = false)
    boolean blacklist = false;

    public Object doExecute() throws Exception {

        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(clusterManager);
        support.setGroupManager(groupManager);
        support.setConfigurationAdmin(configurationAdmin);

        if (!in && !out) {
            in = true;
            out = true;
        }
        if (!whitelist && !blacklist) {
            whitelist = true;
            blacklist = true;
        }

        if (featurePattern == null || featurePattern.isEmpty()) {
            // display mode
            if (in) {
                System.out.println("INBOUND:");
                if (whitelist) {
                    System.out.print("\twhitelist: ");
                    Set<String> list = support.getListEntries(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.INBOUND);
                    System.out.println(list.toString());
                }
                if (blacklist) {
                    System.out.print("\tblacklist: ");
                    Set<String> list = support.getListEntries(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.INBOUND);
                    System.out.println(list.toString());
                }
            }
            if (out) {
                System.out.println("OUTBOUND:");
                if (whitelist) {
                    System.out.print("\twhitelist: ");
                    Set<String> list = support.getListEntries(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.OUTBOUND);
                    System.out.println(list.toString());
                }
                if (blacklist) {
                    System.out.print("\tblacklist: ");
                    Set<String> list = support.getListEntries(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.OUTBOUND);
                    System.out.println(list.toString());
                }
            }
        } else {
            // edit mode
            System.out.println("Updating blocking policy for " + featurePattern);
            if (in) {
                if (whitelist) {
                    System.out.println("\tinbound whitelist ...");
                    support.switchListEntry(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.INBOUND, featurePattern);
                }
                if (blacklist) {
                    System.out.println("\tinbound blacklist ...");
                    support.switchListEntry(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.INBOUND, featurePattern);
                }
            }
            if (out) {
                if (whitelist) {
                    System.out.println("\toutbound whitelist ...");
                    support.switchListEntry(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.OUTBOUND, featurePattern);
                }
                if (blacklist) {
                    System.out.println("\toutbound blacklist ...");
                    support.switchListEntry(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.OUTBOUND, featurePattern);
                }
            }
        }

        return null;
    }

}
