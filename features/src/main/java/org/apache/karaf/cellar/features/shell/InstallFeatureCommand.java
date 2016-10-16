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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.cellar.features.ClusterFeaturesEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureState;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;
import java.util.Map;

@Command(scope = "cluster", name = "feature-install", description = "Install a feature in a cluster group")
@Service
public class InstallFeatureCommand extends CellarCommandSupport {

    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Option(name = "-s", aliases = "--no-auto-start", description = "Do not start the bundles", required = false, multiValued = false)
    boolean noStart;

    @Option(name = "-m", aliases = "--no-auto-manager", description = "Do not automatically manage bundles", required = false, multiValued = false)
    boolean noManage;

    @Option(name = "-u", aliases = "--upgrade", description = "Perform an upgrade of feature if previous version are installed or install it", required = false, multiValued = false)
    boolean upgrade;

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "features", description = "The name and version of the features to install. A feature id looks like name/version. The version is optional.", required = true, multiValued = true)
    @Completion(AllFeatureCompleter.class)
    List<String> features;

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

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {

            // get cluster feature
            Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            CellarSupport support = new CellarSupport();
            support.setClusterManager(clusterManager);
            support.setConfigurationAdmin(configurationAdmin);
            support.setGroupManager(groupManager);

            for (String feature : features) {
                String[] split = feature.split("/");
                String name = split[0];
                String version = null;
                if (split.length == 2) {
                    // use the version provided by the user
                    version = split[1];
                }

                FeatureState found = null;
                String foundKey = null;
                for (String k : clusterFeatures.keySet()) {
                    FeatureState f = clusterFeatures.get(k);
                    foundKey = k;
                    if (version == null) {
                        if (f.getName().equals(name)) {
                            found = f;
                            break;
                        }
                    } else {
                        if (f.getName().equals(name) && f.getVersion().equals(version)) {
                            found = f;
                            break;
                        }
                    }
                }
                if (found == null) {
                    if (version == null)
                        throw new IllegalArgumentException("Feature " + name + " doesn't exist in cluster group " + groupName);
                    else
                        throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist in cluster group " + groupName);
                }

                // check if the feature is allowed
                if (support.isAllowed(group, Constants.CATEGORY, found.getName(), EventType.OUTBOUND)) {

                    // update the cluster group
                    found.setInstalled(true);
                    clusterFeatures.put(foundKey, found);

                    // broadcast the cluster event
                    ClusterFeaturesEvent event = new ClusterFeaturesEvent(found.getName(), found.getVersion(), noRefresh, noStart, noManage, upgrade, FeatureEvent.EventType.FeatureInstalled);
                    event.setSourceGroup(group);
                    eventProducer.produce(event);
                } else {
                    System.err.println("Feature name " + found.getName() + " is blocked outbound for cluster group " + groupName);
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
