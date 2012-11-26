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
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local features listener.
 */
public class LocalFeaturesListener extends FeaturesSupport implements org.apache.karaf.features.FeaturesListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalFeaturesListener.class);

    private EventProducer eventProducer;

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Called when a {@code FeatureEvent} occurs.
     *
     * @param event
     */
    public void featureEvent(FeatureEvent event) {

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR FEATURES: cluster event producer is OFF");
            return;
        }

        if (event != null) {
            Set<Group> groups = groupManager.listLocalGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {

                    Feature feature = event.getFeature();
                    String name = feature.getName();
                    String version = feature.getVersion();

                    if (isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.OUTBOUND)) {
                        FeatureEvent.EventType type = event.getType();

                        // update the distributed map
                        if (FeatureEvent.EventType.FeatureInstalled.equals(event.getType())) {
                            pushFeature(event.getFeature(), group, true);
                        } else {
                            pushFeature(event.getFeature(), group, false);
                        }

                        // broadcast the cluster event
                        RemoteFeaturesEvent featureEvent = new RemoteFeaturesEvent(name, version, type);
                        featureEvent.setSourceGroup(group);
                        eventProducer.produce(featureEvent);
                    } else LOGGER.warn("CELLAR FEATURES: feature {} is marked as BLOCKED OUTBOUND", name);
                }
            }
        }
    }

    /**
     * Called when a {@code RepositoryEvent} occurs.
     *
     * @param event
     */
    public void repositoryEvent(RepositoryEvent event) {

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR FEATURES: cluster event producer is OFF");
            return;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            if (event != null && event.getRepository() != null) {
                Set<Group> groups = groupManager.listLocalGroups();

                if (groups != null && !groups.isEmpty()) {
                    for (Group group : groups) {
                        RemoteRepositoryEvent repositoryEvent = new RemoteRepositoryEvent(event.getRepository().getURI().toString(), event.getType());
                        repositoryEvent.setSourceGroup(group);
                        RepositoryEvent.EventType type = event.getType();

                        // update the distributed map
                        if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                            pushRepository(event.getRepository(), group);
                            // update the feature map
                            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group.getName());
                            try {
                                for (Feature feature : event.getRepository().getFeatures()) {
                                    // check the feature in the distributed map
                                    FeatureInfo featureInfo = null;
                                    for (FeatureInfo distributedFeature : distributedFeatures.keySet()) {
                                        if (distributedFeature.getName().equals(feature.getName()) && distributedFeature.getVersion().equals(feature.getVersion())) {
                                            featureInfo = distributedFeature;
                                            break;
                                        }
                                    }
                                    if (featureInfo == null) {
                                        featureInfo = new FeatureInfo(feature.getName(), feature.getVersion());
                                        distributedFeatures.put(featureInfo, false);
                                    }
                                }
                            } catch (Exception e) {
                                LOGGER.warn("CELLAR FEATURES: can't update the distributed features map", e);
                            }
                        } else {
                            removeRepository(event.getRepository(), group);
                            // update the feature map
                            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group.getName());
                            try {
                                for (Feature feature : event.getRepository().getFeatures()) {
                                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                                    distributedFeatures.remove(info);
                                }
                            } catch (Exception e) {
                                LOGGER.warn("CELLAR FEATURES: can't update the distributed features map", e);
                            }
                        }

                        // broadcast the cluster event
                        eventProducer.produce(repositoryEvent);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
