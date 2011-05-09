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
import org.apache.karaf.cellar.core.event.EventProducer;
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

    private static Logger logger = LoggerFactory.getLogger(FeaturesSynchronizer.class);

    private List<EventProducer> producerList;

    /**
     * Initialization method
     */
    public void init() {
        super.init();
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                }
            }
        }
    }

    /**
     * Destruction method
     */
    public void destroy() {
        super.destroy();
    }

    /**
     * Pulls the features from the cluster.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            List<String> repositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
            Map<FeatureInfo, Boolean> features = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            clusterManager.getList(Constants.FEATURES + Configurations.SEPARATOR + groupName);
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                //Retrieve remote feautre URLs.
                if (repositories != null && !repositories.isEmpty()) {
                    for (String url : repositories) {
                        try {
                            logger.debug("Adding repository {}", url);
                            featuresService.addRepository(new URI(url));
                        } catch (MalformedURLException e) {
                            logger.error("Failed to add features repository! Url {} is malformed", url);
                        } catch (Exception e) {
                            logger.error("Failed to add features repository.", e);
                        }
                    }
                }

                //Retrieve remote feautre status.
                if (features != null && !features.isEmpty()) {
                    for (FeatureInfo info : features.keySet()) {
                        String name = info.getName();
                        //Check if feature is blocked.
                        if (isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.INBOUND)) {
                            Boolean remotelyInstalled = features.get(info);
                            Boolean localyInstalled = isInstanlled(info.getName(), info.getVersion());

                            //If feature needs to be installed locally.
                            if (remotelyInstalled && !localyInstalled) {
                                try {
                                    logger.debug("Installing feature {} version {}", info.getName(), info.getVersion());
                                    featuresService.installFeature(info.getName(), info.getVersion());
                                } catch (Exception e) {
                                    logger.error("Failed to install feature {} {} ", info.getName(), info.getVersion());
                                }
                                //If feature needs to be localy uninstalled.
                            } else if (!remotelyInstalled && localyInstalled) {
                                try {
                                    logger.debug("Uninstalling feature {} version {}", info.getName(), info.getVersion());
                                    featuresService.uninstallFeature(info.getName(), info.getVersion());
                                } catch (Exception e) {
                                    logger.error("Failed to uninstall feature {} {} ", info.getName(), info.getVersion());
                                }
                            }
                        } else logger.debug("Feature with name {} is marked as BLOCKED INBOUND");
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Pushes features to the cluster.
     */
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            List<String> repositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
            Map<FeatureInfo, Boolean> features = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
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
                    logger.error("Error listing features.", e);
                }

                //Process repository list
                if (repositoryList != null && repositoryList.length > 0) {
                    for (Repository repository : repositoryList) {
                        pushRepository(repository, group);
                    }
                }

                //Process features list
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
            Dictionary<String, String> properties = configuration.getProperties();
            String propertyKey = groupName + Configurations.SEPARATOR + Constants.FEATURES_CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
            String propertyValue = properties.get(propertyKey);
            result = Boolean.parseBoolean(propertyValue);
        } catch (IOException e) {
            logger.error("Error while checking if sync is enabled.", e);
        }
        return result;
    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }


    public ClusterManager getCollectionManager() {
        return clusterManager;
    }

    public void setCollectionManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
