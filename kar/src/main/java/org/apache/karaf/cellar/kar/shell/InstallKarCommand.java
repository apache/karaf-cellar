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
package org.apache.karaf.cellar.kar.shell;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.kar.ClusterKarEvent;
import org.apache.karaf.cellar.kar.Constants;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Map;

@Command(scope = "cluster", name = "kar-install", description = "Install a KAR in a cluster group")
@Service
public class InstallKarCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "url", description = "The URL of the KAR file to install on the cluster", required = true, multiValued = false)
    String url;

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
            System.err.println("Cluster event producer is OFF");
            return null;
        }

        CellarSupport support = new CellarSupport();
        support.setConfigurationAdmin(configurationAdmin);
        support.setGroupManager(groupManager);
        support.setClusterManager(clusterManager);

        // check if the kar is allowed
        if (support.isAllowed(group, Constants.CATEGORY, url, EventType.OUTBOUND)) {
            // broadcast cluster event
            ClusterKarEvent clusterEvent = new ClusterKarEvent(url, true);
            clusterEvent.setSourceGroup(group);
            clusterEvent.setInstall(true);
            eventProducer.produce(clusterEvent);
        } else {
            System.err.println("KAR " + url + " is blocked outbound for cluster group " + groupName);
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
