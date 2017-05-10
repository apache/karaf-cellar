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

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.ClusterBundleEvent;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;

import java.util.List;
import java.util.Map;

@Command(scope = "cluster", name = "bundle-uninstall", description = "Uninstall bundles from a cluster group")
@Service
public class UninstallBundleCommand extends BundleCommandSupport {

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

        // update the bundle in the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(gatherBundles(true));

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    System.err.println("Bundle " + bundle + " not found in cluster group " + groupName);
                }
                String location = state.getLocation();

                // check if the bundle is allowed
                CellarSupport support = new CellarSupport();
                support.setClusterManager(this.clusterManager);
                support.setGroupManager(this.groupManager);
                support.setConfigurationAdmin(this.configurationAdmin);
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    System.err.println("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
                    return null;
                }

                clusterBundles.remove(bundle);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, null, Bundle.UNINSTALLED);
                event.setSourceGroup(group);
                event.setSourceNode(clusterManager.getNode());
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
