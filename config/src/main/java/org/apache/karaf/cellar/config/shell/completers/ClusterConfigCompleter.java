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
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Command completer for configurations in cluster groups.
 */
public class ClusterConfigCompleter implements Completer {

    protected ClusterManager clusterManager;
    protected GroupManager groupManager;

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            Map<String, Group> groups = groupManager.listGroups();
            if (groups != null && !groups.isEmpty()) {
                for (String groupName : groups.keySet()) {
                    Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
                    if (clusterConfigurations != null && !clusterConfigurations.isEmpty()) {
                        for (String clusterConfiguration : clusterConfigurations.keySet()) {
                            if (delegate.getStrings() != null && !delegate.getStrings().contains(clusterConfiguration)) {
                                delegate.getStrings().add(clusterConfiguration);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // nothing to do
        }
        return delegate.complete(buffer, cursor, candidates);
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
