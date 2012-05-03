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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.features.FeatureEvent;

@Command(scope = "cluster", name = "features-uninstall", description = "Uninstall a feature assigned to a cluster group.")
public class UninstallFeatureCommand extends FeatureCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name.", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "feature", description = "The feature name.", required = true, multiValued = false)
    String feature;

    @Argument(index = 2, name = "version", description = "The feature version.", required = false, multiValued = false)
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

        // check if the feature exists in the map
        if (!featureExists(groupName, feature, version)) {
            if (version != null)
                System.err.println("Feature " + feature + "/" + version + " doesn't exist for the cluster group " + groupName);
            else System.err.println("Feature " + feature + " doesn't exist for the cluster group " + groupName);
            return null;
        }

        // update distributed set
        updateFeatureStatus(groupName, feature, version, true);

        // broadcast the cluster event
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(feature, version, FeatureEvent.EventType.FeatureUninstalled);
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
