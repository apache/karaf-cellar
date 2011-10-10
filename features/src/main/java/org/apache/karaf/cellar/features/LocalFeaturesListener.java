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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Local features listener.
 */
public class LocalFeaturesListener extends FeaturesSupport implements org.apache.karaf.features.FeaturesListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalFeaturesListener.class);

    private List<EventProducer> producerList;

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
        if (event != null) {
            Set<Group> groups = groupManager.listLocalGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {

                    Feature feature = event.getFeature();
                    String name = feature.getName();
                    String version = feature.getVersion();

                    if (isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.OUTBOUND)) {
                        FeatureEvent.EventType type = event.getType();

                        //Check the event type.
                        //This is required because upon reception of the even the feature service considers the feature uninstalled.
                        if (FeatureEvent.EventType.FeatureInstalled.equals(event.getType())) {
                            pushFeature(event.getFeature(), group, true);
                        } else {
                            pushFeature(event.getFeature(), group, false);
                        }

                        RemoteFeaturesEvent featureEvent = new RemoteFeaturesEvent(name, version, type);
                        featureEvent.setSourceGroup(group);
                        //TODO: Choose group producer.
                        if (producerList != null && !producerList.isEmpty()) {
                            for (EventProducer producer : producerList) {
                                producer.produce(featureEvent);
                            }
                        }
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
        if (event != null && event.getRepository() != null) {
            Set<Group> groups = groupManager.listLocalGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {
                    RemoteRepositoryEvent repositoryEvent = new RemoteRepositoryEvent(event.getRepository().getURI().toString(), event.getType());
                    repositoryEvent.setSourceGroup(group);
                    RepositoryEvent.EventType type = event.getType();

                    if(RepositoryEvent.EventType.RepositoryAdded.equals(type)){
                        pushRepository(event.getRepository(), group);
                    } else {
                        removeRepository(event.getRepository(),group);
                    }
                    if (producerList != null && !producerList.isEmpty()) {
                        for (EventProducer producer : producerList) {
                            producer.produce(repositoryEvent);
                        }
                    }
                }
            }
        }
    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }

}
