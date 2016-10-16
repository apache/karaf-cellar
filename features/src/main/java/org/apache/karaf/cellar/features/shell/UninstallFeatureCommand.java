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
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;
import java.util.Map;

@Command(scope = "cluster", name = "feature-uninstall", description = "Uninstall a feature from a cluster group")
@Service
public class UninstallFeatureCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "features", description = "The name and version of the features to uninstall. A feature id looks like name/version. The version is optional.", required = true, multiValued = true)
    @Completion(AllFeatureCompleter.class)
    List<String> features;

    @Option(name = "-r", aliases = "--no-auto-refresh", description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Reference
    private EventProducer eventProducer;

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
            System.err.println("Cluster event producer is OFF for this node");
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {

            // get cluster features
            Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            CellarSupport support = new CellarSupport();
            support.setGroupManager(groupManager);
            support.setClusterManager(clusterManager);
            support.setConfigurationAdmin(configurationAdmin);

            for (String feature : features) {
                String[] split = feature.split("/");
                String name = split[0];
                String version = null;
                if (split.length == 2) {
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

                // check if the feature is allowed (outbound)
                if (!support.isAllowed(group, Constants.CATEGORY, found.getName(), EventType.OUTBOUND)) {
                    System.err.println("Feature " + found.getName() + " is blocked outbound for cluster group " + groupName);
                    continue;
                }

                // update the cluster state
                found.setInstalled(false);
                clusterFeatures.put(foundKey, found);

                // broadcast the cluster event
                ClusterFeaturesEvent event = new ClusterFeaturesEvent(found.getName(), found.getVersion(), noRefresh, false, false, false, FeatureEvent.EventType.FeatureUninstalled);
                event.setSourceGroup(group);
                eventProducer.produce(event);
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
