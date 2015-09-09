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
package org.apache.karaf.cellar.config.shell.completers;

import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Command completer for the configuration from the cluster.
 */
@Service
public class ClusterConfigCompleter implements Completer {

    @Reference
    protected ClusterManager clusterManager;

    @Reference
    protected GroupManager groupManager;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            Map<String, Group> groups = groupManager.listGroups();
            if (groups != null && !groups.isEmpty()) {
                for (String groupName : groups.keySet()) {
                    Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
                    if (clusterConfigurations != null && !clusterConfigurations.isEmpty()) {
                        for (String pid : clusterConfigurations.keySet()) {
                            if (delegate.getStrings() != null && !delegate.getStrings().contains(pid)) {
                                delegate.getStrings().add(pid);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            // nothing to do
        }
        return delegate.complete(session, commandLine, candidates);
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

}
