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

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
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

    public void init() {
        super.init();
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else LOGGER.warn("CELLAR FEATURES: sync is disabled for group {}", group.getName());
            }
        }
    }

    public void destroy() {
        super.destroy();
    }

    /**
     * Get the features from the cluster group.
     *
     * @param group the cluster group.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            clusterManager.getList(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                // get the features repositories URLs from the cluster group
                if (clusterRepositories != null && !clusterRepositories.isEmpty()) {
                    for (String url : clusterRepositories) {
                        try {
                            if (!isRepositoryRegisteredLocally(url)) {
                                LOGGER.debug("CELLAR FEATURES: adding new repository {}", url);
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
                    for (FeatureInfo info : clusterFeatures.keySet()) {
                        String name = info.getName();
                        // check if feature is blocked
                        if (isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.INBOUND)) {
                            Boolean remotelyInstalled = clusterFeatures.get(info);
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
                        } else LOGGER.warn("CELLAR FEATURES: feature {} is marked as BLOCKED INBOUND", name);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push features repositories and features status to the cluster group.
     *
     * @param group the cluster group.
     */
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.info("CELLAR FEATURES: Pulling features from group {}.",groupName);
            clusterManager.getList(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

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

    @Override
    public Boolean isSyncEnabled(Group group) {
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.FEATURES_CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                String propertyValue = (String) properties.get(propertyKey);
                result = Boolean.parseBoolean(propertyValue);
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR FEATURES: error while checking if sync is enabled", e);
        }
        return result;
    }

}
