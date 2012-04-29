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
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.karaf.cellar.management.CellarFeaturesMBean;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.Map;

/**
 * Implementation of the Cellar features MBean to manipulate Cellar features.
 */
public class CellarFeaturesMBeanImpl extends StandardMBean implements CellarFeaturesMBean {

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private EventProducer eventProducer;

    public CellarFeaturesMBeanImpl() throws NotCompliantMBeanException {
        super(CellarFeaturesMBean.class);
    }

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

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public void install(String groupName, String name, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // TODO update the distributed resource

        // broadcast the cluster event
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureInstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void install(String groupName, String name) throws Exception {
        this.install(groupName, name, null);
    }

    public void uninstall(String groupName, String name, String version) throws Exception {
        // check if cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void uninstall(String groupName, String name) throws Exception {
        this.uninstall(groupName, name, null);
    }

    public TabularData getFeatures(String group) throws Exception {
        CompositeType compositeType = new CompositeType("Feature", "Karaf Cellar feature",
                new String[]{ "name", "version", "installed" },
                new String[]{ "Name of the feature", "Version of the feature", "Whether the feature is installed or not"},
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN });
        TabularType tableType = new TabularType("Features", "Table of all Karaf Cellar features",
                compositeType, new String[]{ "name", "version" });
        TabularData table = new TabularDataSupport(tableType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<FeatureInfo, Boolean> allFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group);
            if (allFeatures != null && !allFeatures.isEmpty()) {
                for (FeatureInfo feature : allFeatures.keySet()) {
                    boolean installed = allFeatures.get(feature);
                    CompositeData data = new CompositeDataSupport(compositeType,
                            new String[]{ "name", "version", "installed" },
                            new Object[]{ feature.getName(), feature.getVersion(), installed });
                    table.put(data);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return table;
    }

}
