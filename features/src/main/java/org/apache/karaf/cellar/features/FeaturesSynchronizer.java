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
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
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

    private EventProducer eventProducer;

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
     * Pull features repositories and features status from a cluster group, and update the local status.
     *
     * @param group the cluster group where to pull.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR FEATURES_MAP: pulling features repositories and features from cluster group {}", groupName);
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES_LIST + Configurations.SEPARATOR + groupName);
                Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

                // get features URLs from the cluster group
                if (clusterRepositories != null && !clusterRepositories.isEmpty()) {
                    for (String url : clusterRepositories) {
                        try {
                            if (!isRepositoryRegisteredLocally(url)) {
                                LOGGER.debug("CELLAR FEATURES: adding repository {}", url);
                                featuresService.addRepository(new URI(url));
                            }
                        } catch (MalformedURLException e) {
                            LOGGER.warn("CELLAR FEATURES: failed to add clusterFeatures repository {} (URL is malformed)", url, e);
                        } catch (Exception e) {
                            LOGGER.warn("CELLAR FEATURES: failed to add clusterFeatures repository {}", url, e);
                        }
                    }
                }

                // get features status from the cluster group
                if (clusterFeatures != null && !clusterFeatures.isEmpty()) {
                    for (FeatureState state : clusterFeatures.values()) {
                        String name = state.getName();
                        // check if feature is blocked
                        if (isAllowed(group, Constants.CATEGORY, name, EventType.INBOUND)) {
                            Boolean clusterInstalled = state.getInstalled();
                            Boolean locallyInstalled = isFeatureInstalledLocally(state.getName(), state.getVersion());

                            // prevent NPE
                            if (clusterInstalled == null) {
                                clusterInstalled = false;
                            }
                            if (locallyInstalled == null) {
                                locallyInstalled = false;
                            }
                            // if feature has to be installed locally
                            if (clusterInstalled && !locallyInstalled) {
                                try {
                                    LOGGER.debug("CELLAR FEATURES: installing feature {}/{}", state.getName(), state.getVersion());
                                    featuresService.installFeature(state.getName(), state.getVersion());
                                } catch (Exception e) {
                                    LOGGER.warn("CELLAR FEATURES: failed to install feature {}/{} ", new Object[]{state.getName(), state.getVersion()}, e);
                                }
                            }
                        } else
                            LOGGER.warn("CELLAR FEATURES: feature {} is marked BLOCKED INBOUND for cluster group {}", name, groupName);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push local features repositories and features status to a cluster group.
     *
     * @param group the cluster group where to push.
     */
    @Override
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR FEATURES_MAP: pushing features repositories and features in cluster group {}.", groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES_LIST + Configurations.SEPARATOR + groupName);
                Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

                Repository[] repositoryList = new Repository[0];
                Feature[] featuresList = new Feature[0];

                try {
                    repositoryList = featuresService.listRepositories();
                    featuresList = featuresService.listFeatures();
                } catch (Exception e) {
                    LOGGER.warn("CELLAR FEATURES: unable to list features", e);
                }

                // push the local features repositories to the cluster group
                if (repositoryList != null && repositoryList.length > 0) {
                    for (Repository repository : repositoryList) {
                        if (!clusterRepositories.contains(repository.getURI().toString())) {
                            clusterRepositories.add(repository.getURI().toString());
                            LOGGER.debug("CELLAR FEATURES: pushing repository {} in cluster group {}", repository.getName(), groupName);
                        } else {
                            LOGGER.debug("CELLAR FEATURES: repository {} is already in cluster group {}", repository.getName(), groupName);
                        }
                    }
                }

                if (featuresList != null && featuresList.length > 0) {
                    for (Feature feature : featuresList) {
                        if (isAllowed(group, Constants.CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                            // push the local features status to the cluster group
                            FeatureState clusterFeatureState = new FeatureState();
                            clusterFeatureState.setName(feature.getName());
                            clusterFeatureState.setVersion(feature.getVersion());
                            clusterFeatureState.setInstalled(featuresService.isInstalled(feature));
                            clusterFeatures.put(feature.getName() + "/" + feature.getVersion(), clusterFeatureState);
                            LOGGER.debug("CELLAR FEATURES: pushing feature {} in cluster group {}", feature.getName(), group.getName());
                            // broadcast the cluster event
                            ClusterFeaturesEvent event;
                            if (featuresService.isInstalled(feature)) {
                                event = new ClusterFeaturesEvent(feature.getName(), feature.getVersion(), FeatureEvent.EventType.FeatureInstalled);
                            } else {
                                event = new ClusterFeaturesEvent(feature.getName(), feature.getVersion(), FeatureEvent.EventType.FeatureUninstalled);
                            }
                            eventProducer.produce(event);
                        } else {
                            LOGGER.debug("CELLAR FEATURES: feature {} is marked BLOCKED OUTBOUND for cluster group {}", feature.getName(), group.getName());
                        }
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
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                return properties.get(propertyKey).toString();
            }
        } catch (IOException e) {
            LOGGER.warn("CELLAR FEATURES_MAP: error while checking if sync is enabled", e);
        }
        return "disabled";
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
