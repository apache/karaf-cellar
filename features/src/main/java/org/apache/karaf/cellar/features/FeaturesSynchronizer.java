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
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Features synchronizer.
 */
public class FeaturesSynchronizer extends FeaturesSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSynchronizer.class);

    @Override
    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                sync(group);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Sync node and cluster states, depending of the sync policy.
     *
     * @param group the target cluster group.
     */
    @Override
    public void sync(Group group) {
        String policy = getSyncPolicy(group);
        if (policy != null && policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR FEATURE: sync policy is set as 'cluster' for cluster group " + group.getName());
            if (clusterManager.listNodesByGroup(group).size() == 1 && clusterManager.listNodesByGroup(group).contains(clusterManager.getNode())) {
                LOGGER.debug("CELLAR FEATURE: node is the first and only member of the group, pushing state");
                push(group);
            } else {
                LOGGER.debug("CELLAR FEATURE: pulling state");
                pull(group);
            }
        }
        if (policy != null && policy.equalsIgnoreCase("node")) {
            LOGGER.debug("CELLAR FEATURE: sync policy is set as 'node' for cluster group " + group.getName());
            push(group);
        }
    }

    /**
     * Pull the features repositories and features states from a cluster group, and update the local states.
     *
     * @param group the cluster group.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR FEATURES: pulling features repositories and features from cluster group {}", groupName);
            Map<String, String> clusterRepositories = clusterManager.getMap(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);
            Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);
            clusterManager.getList(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                // get the features repositories URLs from the cluster group
                if (clusterRepositories != null && !clusterRepositories.isEmpty()) {
                    for (String url : clusterRepositories.keySet()) {
                        try {
                            if (!isRepositoryRegisteredLocally(url)) {
                                LOGGER.debug("CELLAR FEATURES: adding new features repository {}", url);
                                featuresService.addRepository(new URI(url));
                            }
                        } catch (MalformedURLException e) {
                            LOGGER.error("CELLAR FEATURES: failed to add features repository URL {} (malformed)", url, e);
                        } catch (Exception e) {
                            LOGGER.error("CELLAR FEATURES: failed to add features repository URL {}", url, e);
                        }
                    }
                }

                // get the features from the cluster group
                if (clusterFeatures != null && !clusterFeatures.isEmpty()) {
                    for (FeatureState info : clusterFeatures.values()) {
                        String name = info.getName();
                        // check if feature is blocked
                        if (isAllowed(group, Constants.CATEGORY, name, EventType.INBOUND)) {
                            Boolean remotelyInstalled = info.isInstalled();
                            Boolean locallyInstalled = isFeatureInstalledLocally(info.getName(), info.getVersion());

                            // prevent NPE
                            if (remotelyInstalled == null) {
                                remotelyInstalled = false;
                            }
                            if (locallyInstalled == null) {
                                locallyInstalled = false;
                            }

                            // if feature has to be installed locally
                            if (remotelyInstalled && !locallyInstalled) {
                                try {
                                    LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", info.getName(), info.getVersion());
                                    featuresService.installFeature(info.getName(), info.getVersion());
                                } catch (Exception e) {
                                    LOGGER.error("CELLAR FEATURES: failed to install feature {}/{} ", new Object[]{info.getName(), info.getVersion()}, e);
                                }
                                // if feature has to be uninstalled locally
                            } else if (!remotelyInstalled && locallyInstalled) {
                                try {
                                    LOGGER.debug("CELLAR FEATURES: un-installing feature {}/{}", info.getName(), info.getVersion());
                                    featuresService.uninstallFeature(info.getName(), info.getVersion());
                                } catch (Exception e) {
                                    LOGGER.error("CELLAR FEATURES: failed to uninstall feature {}/{} ", new Object[]{info.getName(), info.getVersion()}, e);
                                }
                            }
                        } else LOGGER.trace("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group {}", name, groupName);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push features repositories and features local states to a cluster group.
     *
     * @param group the cluster group.
     */
    @Override
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR FEATURES: pushing features repositories and features in cluster group {}", groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            try {
                Repository[] repositoryList = new Repository[0];
                Feature[] featuresList = new Feature[0];

                try {
                    repositoryList = featuresService.listRepositories();
                    featuresList = featuresService.listFeatures();
                } catch (Exception e) {
                    LOGGER.error("CELLAR FEATURES: error listing features", e);
                }

                // push features repositories to the cluster group
                if (repositoryList != null && repositoryList.length > 0) {
                    for (Repository repository : repositoryList) {
                        pushRepository(repository, group);
                    }
                }

                // push features to the cluster group
                if (featuresList != null && featuresList.length > 0) {
                    for (Feature feature : featuresList) {
                        pushFeature(feature, group);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Get the features sync policy for the given cluster group.
     *
     * @param group the cluster group.
     * @return the current features sync policy for the given cluster group.
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
            LOGGER.error("CELLAR FEATURE: error while retrieving the sync policy", e);
        }

        return "disabled";
    }

}
