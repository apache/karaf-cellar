/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.bundle;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalBundleListener extends BundleSupport implements BundleListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalBundleListener.class);

    private EventProducer eventProducer;

    /**
     * Process {@link BundleEvent}s.
     *
     * @param event
     */
    public void bundleChanged(BundleEvent event) {

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR BUNDLE: cluster event producer is OFF");
            return;
        }

        if (event != null && event.getBundle() != null) {
            Set<Group> groups = null;
            try {
                groups = groupManager.listLocalGroups();
            } catch (Exception ex) {
                LOGGER.warn("Failed to list local groups. Is Cellar uninstalling ?");
            }

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {

                    String symbolicName = event.getBundle().getSymbolicName();
                    String version = event.getBundle().getVersion().toString();
                    String bundleLocation = event.getBundle().getLocation();
                    int type = event.getType();

                    if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {

                        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                        try {
                            // update the cluster map
                            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + group.getName());
                            if (type == BundleEvent.UNINSTALLED) {
                                bundles.remove(symbolicName + "/" + version);
                            } else {
                                BundleState state = bundles.get(symbolicName + "/" + version);
                                if (state == null) {
                                    state = new BundleState();
                                }
                                state.setStatus(type);
                                state.setLocation(bundleLocation);
                                bundles.put(symbolicName + "/" + version, state);
                            }

                            // broadcast the cluster event
                            RemoteBundleEvent remoteBundleEvent = new RemoteBundleEvent(symbolicName, version, bundleLocation, type);
                            remoteBundleEvent.setSourceGroup(group);
                            eventProducer.produce(remoteBundleEvent);
                        } finally {
                            Thread.currentThread().setContextClassLoader(originalClassLoader);
                        }

                    } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked as BLOCKED OUTBOUND", bundleLocation);
                }
            }
        }
    }

    /**
     * Initialization Method.
     */
    public void init() {
        getBundleContext().addBundleListener(this);
    }

    /**
     * Destruction Method.
     */
    public void destroy() {

    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
