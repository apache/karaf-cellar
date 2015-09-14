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
package org.apache.karaf.cellar.bundle.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.ClusterBundleEvent;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;

import java.util.Map;

@Command(scope = "cluster", name = "bundle-start", description = "Start a bundle in a cluster group")
public class StartBundleCommand extends BundleCommandSupport {

    private EventProducer eventProducer;

    @Override
    protected Object doExecute() throws Exception {
        // check if the cluster group exists
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

        // update the bundle in the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        String location;
        String key = null;
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            key = selector(clusterBundles);

            if (key == null) {
                System.err.println("Bundle " + key + " not found in cluster group " + groupName);
            }

            BundleState state = clusterBundles.get(key);
            if (state == null) {
                System.err.println("Bundle " + key + " not found in cluster group " + groupName);
                return null;
            }
            location = state.getLocation();

            // check if the bundle is allowed
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                System.err.println("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
                return null;
            }

            state.setStatus(Bundle.ACTIVE);
            clusterBundles.put(key, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        String[] split = key.split("/");
        ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, Bundle.ACTIVE);
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
