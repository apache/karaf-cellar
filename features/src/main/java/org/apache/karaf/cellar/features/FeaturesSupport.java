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
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;

/**
 * Generic features support.
 */
public class FeaturesSupport extends CellarSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(FeaturesSupport.class);

    protected FeaturesService featuresService;

    public void init(BundleContext bundleContext) {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Check if a feature is already installed locally.
     *
     * @param name the feature name.
     * @param version the feature version.
     * @return true if the feature is already installed locally, false else.
     */
    public Boolean isFeatureInstalledLocally(String name, String version) {
        if (featuresService != null) {
            try {
                Feature[] localFeatures = featuresService.listInstalledFeatures();

                if (localFeatures != null && localFeatures.length > 0) {
                    for (Feature localFeature : localFeatures) {
                        if (localFeature.getName().equals(name) && (localFeature.getVersion().equals(version) || version == null))
                            return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("CELLAR FEATURES: can't check if the feature {}/{} is installed locally", name, version, e);
            }
        }
        return false;
    }

    /**
     * Check if a features repository is already registered locally.
     *
     * @param uri the features repository URI.
     * @return true if the features repository is already registered locally, false else.
     */
    public Boolean isRepositoryRegisteredLocally(String uri) {
        try {
            Repository[] localRepositories = featuresService.listRepositories();
            for (Repository localRepository : localRepositories) {
                if (localRepository.getURI().toString().equals(uri)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR FEATURES: can't check if the feature repository {} is registered locally", uri, e);
        }
        return false;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
