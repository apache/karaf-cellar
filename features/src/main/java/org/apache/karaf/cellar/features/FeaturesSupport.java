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

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Features support.
 */
public class FeaturesSupport extends CellarSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSupport.class);

    protected FeaturesService featuresService;

    /**
     * Initialization method
     */
    public void init() {
    }

    /**
     * Destruction method
     */
    public void destroy() {

    }

    /**
     * Returns true if the specified feature is installed.
     *
     * @param name
     * @param version
     * @return
     */
    public Boolean isInstalled(String name, String version) {
        if (featuresService != null) {
            Feature[] features = featuresService.listInstalledFeatures();

            if (features != null && features.length > 0) {
                for (Feature feature : features) {
                    if (feature.getName().equals(name) && (feature.getVersion().equals(version) || version == null))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Pushes a {@code Feature} and its status to the distributed list of features.
     *
     * @param feature
     */
    public void pushFeature(Feature feature, Group group) {
        if (feature != null) {
            String groupName = group.getName();
            Map<FeatureInfo, Boolean> features = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            if (isAllowed(group, Constants.FEATURES_CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                if (featuresService != null && features != null) {
                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                    Boolean installed = featuresService.isInstalled(feature);
                    features.put(info, installed);
                }
            } else LOGGER.debug("Feature with name {} is marked as BLOCKED OUTBOUND", feature.getName());
        } else LOGGER.debug("Feature is null");
    }

    /**
     * Pushes a {@code Feature} and its status to the distributed list of features.
     * This version of the method force the bundle status, without looking the features service.
     *
     * @param feature
     */
    public void pushFeature(Feature feature, Group group, Boolean force) {
        if (feature != null) {
            String groupName = group.getName();
            Map<FeatureInfo, Boolean> features = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            if (isAllowed(group, Constants.FEATURES_CATEGORY, feature.getName(), EventType.OUTBOUND)) {
                if (featuresService != null && features != null) {
                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                    features.put(info, force);
                }
            } else LOGGER.debug("Feature with name {} is marked as BLOCKED OUTBOUND", feature.getName());
        } else LOGGER.debug("Feature is null");
    }

    /**
     * Pushed a {@code Repository} to the distributed list of repositories.
     *
     * @param repository
     */
    public void pushRepository(Repository repository, Group group) {
        String groupName = group.getName();
        List<String> repositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);

        if (featuresService != null && repositories != null) {
            URI uri = repository.getURI();
            repositories.add(uri.toString());
        }
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
