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
package org.apache.karaf.cellar.features.shell;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Abstract feature command.
 */
@Command(scope = "cluster", name = "feature-install", description = "Installs a feature to all members of a group")
public abstract class FeatureCommandSupport extends CellarCommandSupport {

    protected static final Logger logger = LoggerFactory.getLogger(FeatureCommandSupport.class);

    protected FeaturesService featuresService;
    protected BundleContext bundleContext;

    /**
     * Forces the features status for a specific group.
     * Why? Its required if no group member currently in the cluster.
     * If a member of the group joins later, it won't find the change, unless we force it.
     *
     * @param groupName
     * @param feature
     * @param version
     * @param status
     */
    public Boolean updateFeatureStatus(String groupName, String feature, String version, Boolean status) {
        Boolean result = Boolean.FALSE;
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Group group = groupManager.findGroupByName(groupName);
            if (group == null || group.getMembers().isEmpty()) {
                FeatureInfo info = new FeatureInfo(feature, version);
                Map<FeatureInfo, Boolean> features = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);
                //1st check the existing configuration
                if (version == null || (version.trim().length() < 1)) {
                    for (FeatureInfo f : features.keySet()) {
                        if (f.getName().equals(feature)) {
                            version = f.getVersion();
                            info.setVersion(version);
                        }
                    }
                }

                //2nd check the Features Service.
                try {
                    for (Feature f : featuresService.listFeatures()) {
                        if (f.getName().equals(feature)) {
                            version = f.getVersion();
                            info.setVersion(version);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error while browsing features", e);
                }

                if (info.getVersion() != null && (info.getVersion().trim().length() > 0)) {
                    features.put(info, status);
                    result = Boolean.TRUE;
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return result;
    }


    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

}
