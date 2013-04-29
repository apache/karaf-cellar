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
package org.apache.karaf.cellar.obr.shell;

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.obr.ClusterObrBundleEvent;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

@Command(scope = "cluster", name = "obr-deploy", description = "Deploy an OBR bundle in a cluster group")
public class ObrDeployCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name="bundleId", description = "The bundle ID (symbolicname,version in the OBR) to deploy", required = true, multiValued = false)
    String bundleId;

    @Option(name = "-s", aliases = { "--start" }, description = "Start the deployed bundles.", required = false, multiValued = false)
    boolean start = false;

    private EventProducer eventProducer;

    @Override
    public Object doExecute() throws Exception {
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

        // check if the bundle is allowed
        if (!isAllowed(group, Constants.BUNDLES_CONFIG_CATEGORY, bundleId, EventType.OUTBOUND)) {
            System.err.println("OBR bundle " + bundleId + " is blocked outbound for cluster group " + groupName);
            return null;
        }

        // broadcast a cluster event
        int type = 0;
        if (start) type = Constants.BUNDLE_START_EVENT_TYPE;
        ClusterObrBundleEvent event = new ClusterObrBundleEvent(bundleId, type);
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
