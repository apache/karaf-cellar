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
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.HashMap;
import java.util.Map;

@Command(scope = "cluster", name = "feature-repo-list", description = "List the features repositories in a cluster group")
@Service
public class RepoListCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Option(name = "--local", description = "Shows only features repositories local to the node", required = false, multiValued = false)
    boolean onlyLocal;

    @Option(name = "--cluster", description = "Shows only features repositories on the cluster", required = false, multiValued = false)
    boolean onlyCluster;

    @Reference
    private FeaturesService featuresService;

    @Override
    protected Object doExecute() throws Exception {
        // check if the group  exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        Map<String, RepositoryState> repositories = gatherRepositories();

        ShellTable table = new ShellTable();
        table.column("Repository");
        table.column("Located").alignCenter();
        table.column("URL");
        for (String url : repositories.keySet()) {
            RepositoryState state = repositories.get(url);
            String located = "";
            boolean local = state.isLocal();
            boolean cluster = state.isCluster();
            if (local && cluster)
                located = "cluster/local";
            if (local && !cluster) {
                if (onlyCluster)
                    continue;
                located = "local";
            }
            if (cluster && !local) {
                if (onlyLocal)
                    continue;
                located = "cluster";
            }
            table.addRow().addContent(state.getName(), located, url);
        }
        table.print(System.out, !noFormat);

        return null;
    }

    private Map<String, RepositoryState> gatherRepositories() {
        Map<String, RepositoryState> repositories = new HashMap<String, RepositoryState>();

        // get the cluster features repositories
        Map<String, String> clusterRepositories = clusterManager.getMap(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);
        for (String url : clusterRepositories.keySet()) {
            RepositoryState state = new RepositoryState();
            state.setCluster(true);
            state.setLocal(true);
            state.setName(clusterRepositories.get(url));
            repositories.put(url, state);
        }

        // get the local features repositories
        try {
            for (Repository localRepository : featuresService.listRepositories()) {
                if (repositories.containsKey(localRepository.getURI().toString())) {
                    RepositoryState state = repositories.get(localRepository.getURI().toString());
                    state.setLocal(true);
                } else {
                    RepositoryState state = new RepositoryState();
                    state.setCluster(false);
                    state.setLocal(true);
                    state.setName(localRepository.getName());
                    repositories.put(localRepository.getURI().toString(), state);
                }
            }
        } catch (Exception e) {
            // nothing to do
        }

        return repositories;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    class RepositoryState {

        private String name;
        private boolean cluster;
        private boolean local;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isCluster() {
            return cluster;
        }

        public void setCluster(boolean cluster) {
            this.cluster = cluster;
        }

        public boolean isLocal() {
            return local;
        }

        public void setLocal(boolean local) {
            this.local = local;
        }

    }

}
