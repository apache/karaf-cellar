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
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

/**
 * The BundleSynchronizer is called when Cellar starts or a node joins a cluster group.
 * The purpose is to synchronize bundles local state with the states in the cluster groups.
 */
public class BundleSynchronizer extends BundleSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleSynchronizer.class);

    private EventProducer eventProducer;

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public void init() {
        if (groupManager == null)
            return;
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                sync(group);
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Sync the node and cluster states, depending of the sync policy.
     *
     * @param group the target cluster group.
     */
    @Override
    public void sync(Group group) {
        String policy = getSyncPolicy(group);
        if (policy == null) {
            LOGGER.warn("CELLAR BUNDLE: sync policy is not defined for cluster group {}", group.getName());
        }
        if (policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy set as 'cluster' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR BUNDLE: updating node from the cluster (pull first)");
            pull(group);
            LOGGER.debug("CELLAR BUNDLE: updating cluster from the local node (push after)");
            push(group);
        } else if (policy.equalsIgnoreCase("node")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy set as 'node' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR BUNDLE: updating cluster from the local node (push first)");
            push(group);
            LOGGER.debug("CELLAR BUNDLE: updating node from the cluster (pull after)");
            pull(group);
        } else if (policy.equalsIgnoreCase("clusterOnly")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy set as 'clusterOnly' for cluster group " + group.getName());
            LOGGER.debug("CELLAR BUNDLE: updating node from the cluster (pull only)");
            pull(group);
        } else if (policy.equalsIgnoreCase("nodeOnly")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy set as 'nodeOnly' for cluster group " + group.getName());
            LOGGER.debug("CELLAR BUNDLE: updating cluster from the local node (push only)");
            push(group);
        } else {
            LOGGER.debug("CELLAR BUNDLE: sync policy set as 'disabled' for cluster group " + group.getName());
            LOGGER.debug("CELLAR BUNDLE: no sync");
        }
    }

    /**
     * Pull the bundles states from a cluster group.
     *
     * @param group the cluster group where to get the bundles states.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR BUNDLE: pulling bundles from cluster group {}", groupName);
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                for (Map.Entry<String, BundleState> entry : clusterBundles.entrySet()) {
                    String id = entry.getKey();
                    BundleState state = entry.getValue();

                    String[] tokens = id.split("/");
                    String symbolicName = tokens[0];
                    String version = tokens[1];
                    if (tokens != null && tokens.length == 2) {
                        if (state != null) {
                            String bundleLocation = state.getLocation();
                            if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.INBOUND)) {
                                try {
                                    if (state.getStatus() == Bundle.INSTALLED) {
                                        if (!isInstalled(state.getLocation())) {
                                            LOGGER.debug("CELLAR BUNDLE: installing bundle located {} on node", state.getLocation());
                                            installBundleFromLocation(state.getLocation());
                                        } else {
                                            LOGGER.debug("CELLAR BUNDLE: bundle located {} already installed on node", state.getLocation());
                                        }
                                    } else if (state.getStatus() == Bundle.ACTIVE) {
                                        if (!isInstalled(state.getLocation())) {
                                            LOGGER.debug("CELLAR BUNDLE: installing bundle located {} on node", state.getLocation());
                                            installBundleFromLocation(state.getLocation());
                                        }
                                        if (!isStarted(state.getLocation())) {
                                            LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{} on node", symbolicName, version);
                                            startBundle(symbolicName, version);
                                        } else {
                                            LOGGER.debug("CELLAR BUNDLE: bundle located {} already started on node", state.getLocation());
                                        }
                                    } else if (state.getStatus() == Bundle.UNINSTALLED) {
                                        if (isInstalled(state.getLocation())) {
                                            LOGGER.debug("CELLAR BUNDLE: uninstalling bundle {}/{} on node", symbolicName, version);
                                            uninstallBundle(symbolicName, version);
                                        } else {
                                            LOGGER.debug("CELLAR BUNDLE: bundle {}/{} already uninstalled on node", symbolicName, version);
                                        }
                                    }
                                } catch (BundleException e) {
                                    LOGGER.error("CELLAR BUNDLE: failed to pull bundle {}", id, e);
                                }
                            } else LOGGER.trace("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND for cluster group {}", bundleLocation, groupName);
                        }
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push local bundles states to a cluster group.
     *
     * @param group the cluster group where to update the bundles states.
     */
    @Override
    public void push(Group group) {

        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR BUNDLE: cluster event producer is OFF");
            return;
        }

        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR BUNDLE: pushing bundles to cluster group {}", groupName);
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                Bundle[] bundles;
                BundleContext bundleContext = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();

                bundles = bundleContext.getBundles();
                for (Bundle bundle : bundles) {
                    long bundleId = bundle.getBundleId();
                    String symbolicName = bundle.getSymbolicName();
                    String version = bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
                    String bundleLocation = bundle.getLocation();
                    int status = bundle.getState();
                    String id = symbolicName + "/" + version;

                    // check if the pid is marked as local.
                    if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {
                        if (!clusterBundles.containsKey(id)) {
                            LOGGER.debug("CELLAR BUNDLE: deploying bundle {} on the cluster", id);
                            BundleState bundleState = new BundleState();
                            // get the bundle name or location.
                            String name = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                            // if there is no name, then default to symbolic name.
                            name = (name == null) ? symbolicName : name;
                            // if there is no symbolic name, resort to location.
                            name = (name == null) ? bundle.getLocation() : name;
                            bundleState.setId(bundleId);
                            bundleState.setName(name);
                            bundleState.setSymbolicName(symbolicName);
                            bundleState.setVersion(version);
                            bundleState.setLocation(bundleLocation);
                            bundleState.setStatus(status);
                            // update cluster state
                            clusterBundles.put(id, bundleState);
                            // send cluster event
                            ClusterBundleEvent clusterEvent = new ClusterBundleEvent(symbolicName, version, bundleLocation, status);
                            clusterEvent.setSourceGroup(group);
                            clusterEvent.setSourceNode(clusterManager.getNode());
                            clusterEvent.setLocal(clusterManager.getNode());
                            eventProducer.produce(clusterEvent);
                        } else {
                            BundleState bundleState = clusterBundles.get(id);
                            if (bundleState.getStatus() != status) {
                                LOGGER.debug("CELLAR BUNDLE: updating bundle {} on the cluster", id);
                                // update cluster state
                                bundleState.setStatus(status);
                                clusterBundles.put(id, bundleState);
                                // send cluster event
                                ClusterBundleEvent clusterEvent = new ClusterBundleEvent(symbolicName, version, bundleLocation, status);
                                clusterEvent.setSourceGroup(group);
                                clusterEvent.setSourceNode(clusterManager.getNode());
                                clusterEvent.setLocal(clusterManager.getNode());
                                eventProducer.produce(clusterEvent);
                            }
                        }

                    } else LOGGER.trace("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, groupName);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Get the bundle sync policy for the given cluster group.
     *
     * @param group the cluster group.
     * @return the current bundle sync policy for the given cluster group.
     */
    @Override
    public String getSyncPolicy(Group group) {
        String groupName = group.getName();
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                return properties.get(propertyKey).toString();
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR BUNDLE: error while retrieving the sync policy", e);
        }

        return "disabled";
    }

}
