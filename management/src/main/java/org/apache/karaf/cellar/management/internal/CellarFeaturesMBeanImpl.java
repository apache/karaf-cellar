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
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.karaf.cellar.management.CellarFeaturesMBean;
import org.apache.karaf.cellar.management.codec.JmxFeature;
import org.apache.karaf.features.FeatureEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;
import java.util.ArrayList;
import java.util.Map;

/**
 * Implementation of the CellarFeaturesMBean to manipulate Cellar features.
 */
public class CellarFeaturesMBeanImpl extends StandardMBean implements CellarFeaturesMBean {

    private ClusterManager clusterManager;
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

    public CellarFeaturesMBeanImpl() throws NotCompliantMBeanException {
        super(CellarFeaturesMBean.class);
    }

    public void install(String groupName, String name, String version) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        EventProducer producer = clusterManager.getEventProducer(groupName);
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
        EventProducer producer = clusterManager.getEventProducer(groupName);
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);
    }

    public void uninstall(String groupName, String name) throws Exception {
        this.uninstall(groupName, name, null);
    }

    public TabularData listFeatures(String group) throws Exception {
        Map<FeatureInfo, Boolean> allFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group);
        ArrayList<JmxFeature> features = new ArrayList<JmxFeature>();
        if (allFeatures != null && !allFeatures.isEmpty()) {
            for (FeatureInfo feature : allFeatures.keySet()) {
                boolean installed = allFeatures.get(feature);
                JmxFeature jmxFeature = new JmxFeature(feature, installed);
                features.add(jmxFeature);
            }
        }
        TabularData table = JmxFeature.tableFrom(features);
        return table;
    }

}
