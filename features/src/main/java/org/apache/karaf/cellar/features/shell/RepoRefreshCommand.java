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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.cellar.features.ClusterRepositoryEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.command.completers.AvailableRepoNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.net.URI;
import java.util.Map;

@Command(scope = "cluster", name = "feature-repo-refresh", description = "Refresh a features repository on the cluster")
@Service
public class RepoRefreshCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "name/url", description = "Shortcut name of the features repository or the full URL", required = false, multiValued = false)
    @Completion(AvailableRepoNameCompleter.class)
    String nameOrUrl;

    @Reference
    private EventProducer eventProducer;

    @Reference
    private FeaturesService featuresService;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        // check if the event producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            String uri = null;
            if (nameOrUrl != null) {
                // get the cluster features repositories
                Map<String, String> clusterFeaturesRepositories = clusterManager.getMap(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);

                for (Map.Entry<String, String> entry : clusterFeaturesRepositories.entrySet()) {
                    if (entry.getKey().equals(nameOrUrl) || entry.getValue().equals(nameOrUrl)) {
                        uri = entry.getKey();
                        break;
                    }
                }

                if (uri == null) {
                    System.err.println("Features repository " + nameOrUrl + " doesn't exist in cluster group " + groupName);
                    return null;
                }
            }

            // broadcast the cluster event
            ClusterRepositoryEvent event = new ClusterRepositoryEvent(uri, RepositoryEvent.EventType.RepositoryAdded);
            event.setRefresh(true);
            event.setSourceGroup(group);
            event.setSourceNode(clusterManager.getNode());
            eventProducer.produce(event);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

}
