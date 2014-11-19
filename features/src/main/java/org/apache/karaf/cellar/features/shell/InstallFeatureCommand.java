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
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.features.ClusterFeaturesEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import java.util.List;

@Command(scope = "cluster", name = "feature-install", description = "Install a feature in a cluster group")
public class InstallFeatureCommand extends FeatureCommandSupport {

    @Option(name = "-c", aliases = { "--no-clean" }, description = "Do not uninstall bundles on failure", required = false, multiValued = false)
    boolean noClean;

    @Option(name = "-r", aliases = { "--no-auto-refresh" }, description = "Do not automatically refresh bundles", required = false, multiValued = false)
    boolean noRefresh;

    @Option(name = "-s", aliases = { "--no-auto-start" }, description = "Do not automatically start bundles", required = false, multiValued = false)
    boolean noStart;

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "features", description = "The name and version of the features to install. A feature id looks like name/version. The version is optional.", required = true, multiValued = true)
    List<String> features;

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
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        for (String feature : features) {
            String[] split = feature.split("/");
            String name = split[0];
            String version = null;
            if (split.length == 2) {
                version = split[1];
            }

            // check if the feature exists in the map
            if (!featureExists(groupName, name, version)) {
                if (version != null) {
                    System.err.println("Feature " + name + "/" + version + " doesn't exist in the cluster group " + groupName);
                } else {
                    System.err.println("Feature " + feature + " doesn't exist in the cluster group " + groupName);
                }
                continue;
            }

            // check if the feature is allowed (outbound)
            if (!isAllowed(group, Constants.CATEGORY, name, EventType.OUTBOUND)) {
                System.err.println("Feature " + feature + " is blocked outbound for cluster group " + groupName);
                continue;
            }

            // update the cluster state
            updateFeatureStatus(groupName, name, version, true);

            // broadcast the cluster event
            ClusterFeaturesEvent event = new ClusterFeaturesEvent(name, version, noClean, noRefresh, noStart, FeatureEvent.EventType.FeatureInstalled);
            event.setSourceGroup(group);
            eventProducer.produce(event);
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
