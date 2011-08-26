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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.karaf.cellar.management.CellarFeaturesMBean;
import org.apache.karaf.features.FeatureEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.Map;

/**
 * Implementation of the CellarFeaturesMBean to manipulate Cellar features.
 */
public class CellarFeaturesMBeanImpl extends StandardMBean implements CellarFeaturesMBean {

    private ClusterManager clusterManager;
    private EventTransportFactory eventTransportFactory;
    private GroupManager groupManager;

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    public CellarFeaturesMBeanImpl() throws NotCompliantMBeanException {
        super(CellarFeaturesMBean.class);
    }

    public void install(String groupName, String name, String version) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        EventProducer producer = eventTransportFactory.getEventProducer(groupName,true);
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureInstalled);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);
    }

    public void install(String groupName, String name) throws Exception {
        this.install(groupName, name, null);
    }

    public void uninstall(String groupName, String name, String version) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        EventProducer producer = eventTransportFactory.getEventProducer(groupName,true);
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);
    }

    public void uninstall(String groupName, String name) throws Exception {
        this.uninstall(groupName, name, null);
    }

    public TabularData getFeatures(String group) throws Exception {
        CompositeType featuresType = new CompositeType("Feature", "Karaf Cellar feature",
                new String[]{"name", "version", "installed"},
                new String[]{"Name of the feature", "Version of the feature", "Whether the feature is installed or not"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN});

        TabularType tabularType = new TabularType("Features", "Table of all Karaf Cellar features",
                featuresType, new String[]{"name", "version"});

        TabularData table = new TabularDataSupport(tabularType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<FeatureInfo, Boolean> allFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group);
            if (allFeatures != null && !allFeatures.isEmpty()) {
                for (FeatureInfo feature : allFeatures.keySet()) {
                    boolean installed = allFeatures.get(feature);
                    CompositeData data = new CompositeDataSupport(featuresType,
                            new String[]{"name", "version", "installed"},
                            new Object[]{feature.getName(), feature.getVersion(), installed});
                    table.put(data);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return table;
    }

}
