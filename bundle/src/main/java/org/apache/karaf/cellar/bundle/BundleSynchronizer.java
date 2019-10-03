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
import org.apache.karaf.features.BootFinished;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.service.cm.Configuration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
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

    public void init(BundleContext bundleContext) {
        // wait the end of Karaf boot process
        ServiceTracker tracker = new ServiceTracker(bundleContext, BootFinished.class, null);
        try {
            tracker.waitForService(120000);
        } catch (Exception e) {
            LOGGER.warn("Can't start BootFinished service tracker", e);
        }
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
        } else if (policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy set as 'cluster' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR BUNDLE: updating node from the cluster (pull first)");
            pull(group);
            LOGGER.debug("CELLAR BUNDLE: node is the only one in the cluster group, no pull");
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
            LOGGER.debug("CELLAR BUNDLE: node is the only one in the cluster group, no pull");
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

                // get the bundles on the cluster to update local bundles
                for (Map.Entry<String, BundleState> entry : clusterBundles.entrySet()) {
                    String id = entry.getKey();
                    BundleState state = entry.getValue();

                    String[] tokens = id.split("/");
                    if (tokens.length == 2) {
                        String symbolicName = tokens[0];
                        String version = tokens[1];
                        if (state != null) {
                            String bundleLocation = state.getLocation();
                            if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.INBOUND)) {
                                try {
                                    if (state.getStatus() == Bundle.INSTALLED) {
                                        ensureInstalled(state, state.getStartLevel());
                                        if (isStarted(state.getLocation())) {
                                            refreshBundle(findBundle(state.getLocation()));
                                            LOGGER.debug("CELLAR BUNDLE: refreshing {}/{}", state.getSymbolicName(), state.getVersion());
                                        }
                                    } else if (state.getStatus() == Bundle.ACTIVE) {
                                        ensureInstalled(state, state.getStartLevel());
                                        if (!isStarted(state.getLocation())) {
                                            LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{} on node", symbolicName, version);
                                            startBundle(symbolicName, version);
                                        } else {
                                            LOGGER.debug("CELLAR BUNDLE: bundle located {} already started on node", state.getLocation());
                                        }
                                    } else if (state.getStatus() == Bundle.RESOLVED) {
                                        ensureInstalled(state, state.getStartLevel());
                                        Bundle b = findBundle(state.getLocation());
                                        if (b != null) {
                                            if (b.getState() == Bundle.ACTIVE) {
                                                LOGGER.debug("CELLAR BUNDLE: stopping bundle {}/{} on node", symbolicName, version);
                                                stopBundle(symbolicName, version);
                                            } else if (b.getState() == Bundle.INSTALLED) {
                                                LOGGER.debug("CELLAR BUNDLE: resolving bundle {}/{} on node", symbolicName, version);
                                                resolveBundle(b);
                                            }
                                        } else {
                                            LOGGER.warn("CELLAR BUNDLE: unable to find bundle located {} on node", state.getLocation());
                                        }
                                    }
                                } catch (BundleException e) {
                                    LOGGER.error("CELLAR BUNDLE: failed to pull bundle {}", id, e);
                                }
                            } else LOGGER.trace("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND for cluster group {}", bundleLocation, groupName);
                        }
                    }
                }
                // cleanup the local bundles not present on the cluster if the node is not the first one in the cluster group
                if (getSynchronizerMap().containsKey(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName)) {
                    for (Bundle bundle : bundleContext.getBundles()) {
                        String id = getId(bundle);
                        if (!clusterBundles.containsKey(id) && isAllowed(group, Constants.CATEGORY, bundle.getLocation(), EventType.INBOUND)) {
                            // the bundle is not present on the cluster, so it has to be uninstalled locally
                            try {
                                LOGGER.debug("CELLAR BUNDLE: uninstalling local bundle {} which is not present in cluster", id);
                                bundle.uninstall();
                            } catch (Exception e) {
                                LOGGER.warn("Can't uninstall {}", id, e);
                            }
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
                // push local bundles to the cluster
                for (Bundle bundle : bundles) {
                    long bundleId = bundle.getBundleId();
                    String symbolicName = bundle.getSymbolicName();
                    String version = bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
                    String bundleLocation = bundle.getLocation();
                    int status = bundle.getState();
                    int level = bundle.adapt(BundleStartLevel.class).getStartLevel();

                    String id = getId(bundle);

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
                            bundleState.setStartLevel(level);
                            bundleState.setSymbolicName(symbolicName);
                            bundleState.setVersion(version);
                            bundleState.setLocation(bundleLocation);
                            bundleState.setStatus(status);
                            // update cluster state
                            clusterBundles.put(id, bundleState);
                            // send cluster event
                            ClusterBundleEvent clusterEvent = new ClusterBundleEvent(symbolicName, version, bundleLocation, level, status);
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
                                ClusterBundleEvent clusterEvent = new ClusterBundleEvent(symbolicName, version, bundleLocation, null, status);
                                clusterEvent.setSourceGroup(group);
                                clusterEvent.setSourceNode(clusterManager.getNode());
                                clusterEvent.setLocal(clusterManager.getNode());
                                eventProducer.produce(clusterEvent);
                            }
                        }

                    } else LOGGER.trace("CELLAR BUNDLE: bundle {} is marked BLOCKED OUTBOUND for cluster group {}", bundleLocation, groupName);
                }
                // clean bundles on the cluster not present locally
                for (Map.Entry<String, BundleState> entry : clusterBundles.entrySet()) {
                    String id = entry.getKey();
                    BundleState state = entry.getValue();
                    if (state != null && isAllowed(group, Constants.CATEGORY, state.getLocation(), EventType.OUTBOUND)) {
                        boolean found = false;
                        for (Bundle bundle : bundleContext.getBundles()) {
                            String localBundleId = getId(bundle);
                            if (id.equals(localBundleId)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            clusterBundles.remove(id);
                        }
                    }
                }
                getSynchronizerMap().putIfAbsent(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName, true);
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Return the Cellar bundle ID for a given bundle.
     *
     * @param bundle The bundle.
     * @return The Cellar bundle ID.
     */
    private String getId(Bundle bundle) {
        String symbolicName = bundle.getSymbolicName();
        String version = bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
        return symbolicName + "/" + version;
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
                if (properties.get(propertyKey) != null) {
                    return properties.get(propertyKey).toString();
                }
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR BUNDLE: error while retrieving the sync policy", e);
        }

        return null;
    }

    /**
     * Installs the bundle which corresponds to the supplied state if it is not installed yet. If it is already installed, performs its
     * update if needed (based on the last modified date).
     * 
     * @param state the cluster bundle state
     * @throws BundleException in case of bundle operation errors
     */
    private void ensureInstalled(BundleState state, int startLevel) throws BundleException {
        Bundle existingBundle = findBundle(state.getLocation());
        if (existingBundle == null) {
            LOGGER.debug("CELLAR BUNDLE: installing bundle located {} on node", state.getLocation());
            installBundleFromLocation(state.getLocation(), startLevel);
        } else if (requiresUpdate(existingBundle)) {
            LOGGER.debug("CELLAR BUNDLE: updating bundle located {} on node", state.getLocation());
            existingBundle.update();
        } else {
            LOGGER.debug("CELLAR BUNDLE: bundle located {} already installed on node", state.getLocation());
        }
    }

    private boolean requiresUpdate(Bundle bundle) {
        long currentLastModified = bundle.getLastModified();
        if (currentLastModified <= 0) {
            return false;
        }

        String location = bundle.getLocation();
        try {
            long newLastModified = new URL(location).openConnection().getLastModified();
            return newLastModified > 0 && newLastModified > currentLastModified;
        } catch (IOException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Unable to obtain last modified date for the location " + location + " of the bundle "
                        + bundle + ". Cause: " + e.getMessage(), e);
            } else {
                LOGGER.warn("Unable to obtain last modified date for the location {} of the bundle {}. Cause: {}",
                        new Object[] { location, bundle, e.getMessage() });
            }
        }

        return false;
    }

}
