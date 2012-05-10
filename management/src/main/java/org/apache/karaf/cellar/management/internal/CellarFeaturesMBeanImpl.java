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

import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.karaf.cellar.management.CellarFeaturesMBean;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.osgi.service.cm.ConfigurationAdmin;

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
    private ConfigurationAdmin configurationAdmin;

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

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
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

        // check if the feature is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Feature " + name + " is blocked outbound");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // get the features distributed map
            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            // check if the feature exist
            FeatureInfo feature = null;
            for (FeatureInfo info : distributedFeatures.keySet()) {
                if (version == null) {
                    if (info.getName().equals(name)) {
                        feature = info;
                        break;
                    }
                } else {
                    if (info.getName().equals(name) && info.getVersion().equals(version)) {
                        feature = info;
                        break;
                    }
                }
            }

            if (feature == null) {
                if (version == null)
                    throw new IllegalArgumentException("Feature " + name + " doesn't exist for cluster group " + groupName);
                else throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist for cluster group " + groupName);
            }

            // update the distributed map
            distributedFeatures.put(feature, true);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

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

        // check if the feature is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Feature " + name + " is blocked outbound");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // get the features distributed map
            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            // check if the feature exist
            FeatureInfo feature = null;
            for (FeatureInfo info : distributedFeatures.keySet()) {
                if (version == null) {
                    if (info.getName().equals(name)) {
                        feature = info;
                        break;
                    }
                } else {
                    if (info.getName().equals(name) && info.getVersion().equals(version)) {
                        feature = info;
                        break;
                    }
                }
            }

            if (feature == null) {
                if (version == null)
                    throw new IllegalArgumentException("Feature " + name + " doesn't exist for cluster group " + groupName);
                else throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist for cluster group " + groupName);
            }

            // update the distributed map
            distributedFeatures.put(feature, false);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
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
