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
import org.osgi.framework.BundleEvent;
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
        if (policy != null && policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy is set as 'cluster' for cluster group " + group.getName());
            if (clusterManager.listNodesByGroup(group).size() == 1 && clusterManager.listNodesByGroup(group).contains(clusterManager.getNode())) {
                LOGGER.debug("CELLAR BUNDLE: node is the first and only member of the group, pushing state");
                push(group);
            } else {
                LOGGER.debug("CELLAR BUNDLE: pulling state");
                pull(group);
            }
        }
        if (policy != null && policy.equalsIgnoreCase("node")) {
            LOGGER.debug("CELLAR BUNDLE: sync policy is set as 'node' for cluster group " + group.getName());
            push(group);
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
                                    if (state.getStatus() == BundleEvent.INSTALLED) {
                                        installBundleFromLocation(state.getLocation());
                                    } else if (state.getStatus() == BundleEvent.STARTED) {
                                        installBundleFromLocation(state.getLocation());
                                        startBundle(symbolicName, version);
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
                    String version = bundle.getVersion().toString();
                    String bundleLocation = bundle.getLocation();
                    int status = bundle.getState();
                    String id = symbolicName + "/" + version;

                    // check if the pid is marked as local.
                    if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {

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

                        if (status == Bundle.ACTIVE)
                            status = BundleEvent.STARTED;
                        if (status == Bundle.INSTALLED)
                            status = BundleEvent.INSTALLED;
                        if (status == Bundle.RESOLVED)
                            status = BundleEvent.RESOLVED;
                        if (status == Bundle.STARTING)
                            status = BundleEvent.STARTING;
                        if (status == Bundle.UNINSTALLED)
                            status = BundleEvent.UNINSTALLED;
                        if (status == Bundle.STOPPING)
                            status = BundleEvent.STARTED;

                        bundleState.setStatus(status);

                        clusterBundles.put(id, bundleState);

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
