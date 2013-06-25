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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.features.ClusterRepositoryEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Command(scope = "cluster", name = "feature-url-add", description = "Add features repository URLs in a cluster group")
public class UrlAddCommand extends FeatureCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "urls", description = "One or more features repository URLs separated by whitespaces", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-i", aliases = { "--install-all" }, description = "Install all features contained in the repository URLs", required = false, multiValued = false)
    boolean install;

    private EventProducer eventProducer;

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
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // get the repositories in the cluster group
            List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
            // get the features in the cluster group
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            for (String url : urls) {
                // check if the URL is already registered
                boolean found = false;
                for (String repository : clusterRepositories) {
                    if (repository.equals(url)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // update the repository temporary locally
                    Repository repository  = null;
                    boolean localRegistered = false;
                    // local lookup
                    for (Repository registeredRepository : featuresService.listRepositories()) {
                        if (registeredRepository.getURI().equals(new URI(url))) {
                            repository = registeredRepository;
                            break;
                        }
                    }
                    if (repository == null) {
                        // registered locally
                        try {
                            featuresService.addRepository(new URI(url));
                        } catch (Exception e) {
                            System.err.println("Repository URL " + url + " is not valid: " + e.getMessage());
                            continue;
                        }
                        // get the repository
                        for (Repository registeredRepository : featuresService.listRepositories()) {
                            if (registeredRepository.getURI().equals(new URI(url))) {
                                repository = registeredRepository;
                                break;
                            }
                        }
                    } else {
                        localRegistered = true;
                    }

                    // update the repositories in the cluster group
                    clusterRepositories.add(url);

                    // update the features in the cluster group
                    for (Feature feature : repository.getFeatures()) {
                        FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                        clusterFeatures.put(info, false);
                    }

                    // un-register the repository if it's not local registered
                    if (!localRegistered)
                        featuresService.removeRepository(new URI(url));

                    // broadcast the cluster event
                    ClusterRepositoryEvent event = new ClusterRepositoryEvent(url, RepositoryEvent.EventType.RepositoryAdded);
                    event.setSourceGroup(group);
                    event.setInstall(install);
                    eventProducer.produce(event);
                } else {
                    System.err.println("Repository URL " + url + " already registered");
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

}
