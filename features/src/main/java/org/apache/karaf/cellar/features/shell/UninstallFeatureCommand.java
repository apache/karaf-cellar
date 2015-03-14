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
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.features.ClusterFeaturesEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.features.FeatureState;
import org.apache.karaf.features.FeatureEvent;

import java.util.Map;

@Command(scope = "cluster", name = "features-uninstall", description = "Uninstall a feature from a cluster group")
public class UninstallFeatureCommand extends FeatureCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "feature", description = "The feature name", required = true, multiValued = false)
    String feature;

    @Argument(index = 2, name = "version", description = "The feature version", required = false, multiValued = false)
    String version;

    private EventProducer eventProducer;

    @Override
    protected Object doExecute() throws Exception {
        // check if cluster group exists
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

        Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

        // try to resolve the feature version if not provided
        if (version == null) {
            for (FeatureState state : clusterFeatures.values()) {
                if (state.getName().equals(feature)) {
                    version = state.getVersion();
                }
            }
        }

        // check if the feature exists in the map
        if (!clusterFeatures.containsKey(feature + "/" + version)) {
            if (version != null)
                System.err.println("Feature " + feature + "/" + version + " doesn't exist in the cluster group " + groupName);
            else System.err.println("Feature " + feature + " doesn't exist in the cluster group " + groupName);
            return null;
        }

        // check if the outbound event is allowed
        if (!isAllowed(group, Constants.CATEGORY, feature, EventType.OUTBOUND)) {
            System.err.println("Feature " + feature + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        // update the features in the cluster group
        FeatureState clusterFeatureState = clusterFeatures.get(feature + "/" + version);
        if (clusterFeatureState == null) {
            clusterFeatureState = new FeatureState();
            clusterFeatureState.setName(feature);
            clusterFeatureState.setVersion(version);
        }
        clusterFeatureState.setInstalled(Boolean.FALSE);
        clusterFeatures.put(feature + "/" + version, clusterFeatureState);

        // broadcast the cluster event
        ClusterFeaturesEvent event = new ClusterFeaturesEvent(feature, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);

        return null;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
