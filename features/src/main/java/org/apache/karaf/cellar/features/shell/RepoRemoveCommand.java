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
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureState;
import org.apache.karaf.cellar.features.ClusterRepositoryEvent;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.command.completers.InstalledRepoNameCompleter;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Command(scope = "cluster", name = "feature-repo-remove", description = "Remove features repository URLs from a cluster group")
@Service
public class RepoRemoveCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "repository", description = "Name or url of the repository to remove", required = true, multiValued = false)
    @Completion(InstalledRepoNameCompleter.class)
    String repository;

    @Option(name = "-u", aliases = {"--uninstall-all"}, description = "Uninstall all features contained in the features repositories", required = false, multiValued = false)
    boolean uninstall;

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

        // get the features repositories in the cluster group
        Map<String, String> clusterRepositories = clusterManager.getMap(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);
        // get the features in the cluster group
        Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            List<String> urls = new ArrayList<String>();
            Pattern pattern = Pattern.compile(repository);
            for (String repositoryUrl : clusterRepositories.keySet()) {
                String repositoryName = clusterRepositories.get(repositoryUrl);
                if (repositoryName != null && !repositoryName.isEmpty()) {
                    // repository has name, try regex on the name
                    Matcher nameMatcher = pattern.matcher(repositoryName);
                    if (nameMatcher.matches()) {
                        urls.add(repositoryUrl);
                    } else {
                        // the name regex doesn't match, fallback to repository URI regex
                        Matcher uriMatcher = pattern.matcher(repositoryUrl);
                        if (uriMatcher.matches()) {
                            urls.add(repositoryUrl);
                        }
                    }
                } else {
                    // the repository name is not defined, use repository URI regex
                    Matcher uriMatcher = pattern.matcher(repositoryUrl);
                    if (uriMatcher.matches()) {
                        urls.add(repositoryUrl);
                    }
                }
            }

            for (String url : urls) {
                // looking for the URL in the list
                boolean found = false;
                for (String repository : clusterRepositories.keySet()) {
                    if (this.repository.equals(url)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    Features repositoryModel = JaxbUtil.unmarshal(url, true);

                    // update the features repositories in the cluster group
                    clusterRepositories.remove(url);

                    // update the features in the cluster group
                    for (Feature feature : repositoryModel.getFeature()) {
                        clusterFeatures.remove(feature.getName() + "/" + feature.getVersion());
                    }

                    // broadcast a cluster event
                    ClusterRepositoryEvent event = new ClusterRepositoryEvent(url, RepositoryEvent.EventType.RepositoryRemoved);
                    event.setUninstall(uninstall);
                    event.setSourceGroup(group);
                    event.setSourceNode(clusterManager.getNode());
                    eventProducer.produce(event);
                } else {
                    System.err.println("Features repository URL " + url + " not found in cluster group " + groupName);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }
}
