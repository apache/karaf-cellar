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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LocalBundleListener is listening for local bundles changes.
 * When a local bundle change occurs, this listener updates the cluster and broadcasts a cluster bundle event.
 */
public class LocalBundleListener extends BundleSupport implements SynchronousBundleListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalBundleListener.class);

    private EventProducer eventProducer;

    /**
     * Callback method called when a local bundle status change.
     *
     * @param event the local bundle event.
     */
    @Override
    public void bundleChanged(BundleEvent event) {

        if (event.getBundle().getBundleId() == 0 && (event.getType() == BundleEvent.STOPPING || event.getType() == BundleEvent.STOPPED)) {
            LOGGER.debug("CELLAR BUNDLE: Karaf shutdown detected, removing Cellar LocalBundleListener");
            bundleContext.removeBundleListener(this);
            return;
        }

        if (!isEnabled()) {
            LOGGER.trace("CELLAR BUNDLE: local listener is disabled");
            return;
        }

        if (event.getBundle().getBundleId() == 0) {
            return;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR BUNDLE: cluster event producer is OFF");
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

                    // get the bundle name or location.
                    String name = (String) event.getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                    // if there is no name, then default to symbolic name.
                    name = (name == null) ? event.getBundle().getSymbolicName() : name;
                    // if there is no symbolic name, resort to location.
                    name = (name == null) ? event.getBundle().getLocation() : name;
                    String symbolicName = event.getBundle().getSymbolicName();
                    String version = event.getBundle().getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
                    String bundleLocation = event.getBundle().getLocation();
                    int status = event.getBundle().getState();
                    int startLevel = event.getBundle().adapt(BundleStartLevel.class).getStartLevel();

                    if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {

                        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                        try {
                            // update bundles in the cluster group
                            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + group.getName());
                            if (event.getType() == BundleEvent.UNINSTALLED) {
                                clusterBundles.remove(symbolicName + "/" + version);
                            } else {
                                BundleState state = clusterBundles.get(symbolicName + "/" + version);
                                if (state == null) {
                                    state = new BundleState();
                                }
                                state.setId(event.getBundle().getBundleId());
                                state.setName(name);
                                state.setVersion(version);
                                state.setSymbolicName(symbolicName);
                                state.setStatus(status);
                                state.setStartLevel(startLevel);
                                state.setLocation(bundleLocation);
                                clusterBundles.put(symbolicName + "/" + version, state);
                            }

                            // check the features first
                        	List<Feature> matchingFeatures = retrieveFeature(bundleLocation);
                        	for (Feature feature : matchingFeatures) {
            					if (!isAllowed(group, "feature", feature.getName(), EventType.OUTBOUND)) {
            						LOGGER.trace("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, feature.getName(), group.getName());
            						return;
            					}
            				}
                            
                            // broadcast the cluster event
                            ClusterBundleEvent clusterBundleEvent = new ClusterBundleEvent(symbolicName, version, bundleLocation, startLevel, status);
                            clusterBundleEvent.setSourceGroup(group);
                            clusterBundleEvent.setSourceNode(clusterManager.getNode());
                            clusterBundleEvent.setLocal(clusterManager.getNode());
                            eventProducer.produce(clusterBundleEvent);
                        } catch (Exception e) {
                        	LOGGER.error("CELLAR BUNDLE: failed to create bundle event", e);
						} finally {
                            Thread.currentThread().setContextClassLoader(originalClassLoader);
                        }

                    } else LOGGER.trace("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, group.getName());
                }
            }
        }
    }

    /**
     * Check if the local bundle listener is enabled in the etc/org.apache.karaf.cellar.groups.cfg.
     *
     * @return true if enabled, false else.
     */
    private boolean isEnabled() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String value = properties.get(Constants.CATEGORY + Configurations.SEPARATOR + Configurations.LISTENER).toString();
                return Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR BUNDLE: can't check listener configuration", e);
        }
        return false;
    }

    public void init() {
        getBundleContext().addBundleListener(this);
    }

    public void destroy() {
        bundleContext.removeBundleListener(this);
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
