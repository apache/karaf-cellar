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
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.Bundle;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

@Command(scope = "cluster", name = "bundle-install", description = "Install bundles in a cluster group")
@Service
public class InstallBundleCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Argument(index = 1, name = "urls", description = "Bundle URLs separated by whitespace", required = true, multiValued = true)
    List<String> urls;

    @Option(name = "-s", aliases = {"--start"}, description = "Start the bundle after installation", required = false, multiValued = false)
    boolean start;

    @Option(name = "-l", aliases = {"--start-level"}, description = "Set the start level of the bundle", required = false, multiValued = false)
    Integer level;

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
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);

        for (String url : urls) {
            // check if the bundle is allowed
            if (support.isAllowed(group, Constants.CATEGORY, url, EventType.OUTBOUND)) {

                // get the name and version in the location MANIFEST
                JarInputStream jarInputStream = new JarInputStream(new URL(url).openStream());
                Manifest manifest = jarInputStream.getManifest();
                if (manifest == null) {
                    System.err.println("Bundle location " + url + " doesn't seem correct");
                    continue;
                }
                String name = manifest.getMainAttributes().getValue("Bundle-Name");
                String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
                if (name == null) {
                    name = symbolicName;
                }
                if (name == null) {
                    name = url;
                }
                String version = manifest.getMainAttributes().getValue("Bundle-Version");
                jarInputStream.close();

                ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                try {
                    // update the cluster group
                    Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
                    BundleState state = new BundleState();
                    state.setName(name);
                    state.setSymbolicName(symbolicName);
                    state.setVersion(version);
                    state.setStartLevel(level);
                    state.setId(clusterBundles.size());
                    state.setLocation(url);
                    if (start) {
                        state.setStatus(Bundle.ACTIVE);
                    } else {
                        state.setStatus(Bundle.INSTALLED);
                    }
                    clusterBundles.put(symbolicName + "/" + version, state);
                } finally {
                    Thread.currentThread().setContextClassLoader(originalClassLoader);
                }

                // broadcast the cluster event
                ClusterBundleEvent event;
                if (start) {
                    event = new ClusterBundleEvent(symbolicName, version, url, level, Bundle.ACTIVE);
                    event.setSourceGroup(group);
                    event.setSourceNode(clusterManager.getNode());
                } else {
                    event = new ClusterBundleEvent(symbolicName, version, url, level, Bundle.INSTALLED);
                    event.setSourceGroup(group);
                    event.setSourceNode(clusterManager.getNode());
                }
                eventProducer.produce(event);
            } else {
                System.err.println("Bundle location " + url + " is blocked outbound for cluster group " + groupName);
            }
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
