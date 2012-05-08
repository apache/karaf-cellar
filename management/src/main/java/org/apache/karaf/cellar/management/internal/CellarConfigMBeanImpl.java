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

import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.config.RemoteConfigurationEvent;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.management.CellarConfigMBean;
import org.osgi.service.cm.ConfigurationEvent;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.*;

/**
 * Implementation of the Cellar Config MBean allowing to manipulate Cellar config admin layer.
 */
public class CellarConfigMBeanImpl extends StandardMBean implements CellarConfigMBean {

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private EventProducer eventProducer;

    public CellarConfigMBeanImpl() throws NotCompliantMBeanException {
        super(CellarConfigMBean.class);
    }


    public List<String> listConfig(String groupName) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        List<String> result = new ArrayList<String>();

        Map<String, Properties> config = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        for (String pid : config.keySet()) {
            result.add(pid);
        }

        return result;
    }

    public void deleteConfig(String groupName, String pid) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        Map<String, Properties> configurationMap = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        if (configurationMap != null) {
            // update the distributed map
            Properties properties = configurationMap.remove(pid);

            // broadcast the cluster event
            RemoteConfigurationEvent event = new RemoteConfigurationEvent(pid);
            event.setSourceGroup(group);
            event.setType(ConfigurationEvent.CM_DELETED);
            eventProducer.produce(event);
        } else {
            throw new IllegalStateException("Configuration distributed map not found for cluster group " + groupName);
        }
    }

    public TabularData listProperties(String group, String pid) throws Exception {
        CompositeType compositeType = new CompositeType("Property", "Karaf Cellar Config property",
                new String[]{"key", "value"},
                new String[]{"Property key", "Property value"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Properties", "Table of all properties in the config PID",
                compositeType, new String[]{"key"});
        TabularData table = new TabularDataSupport(tableType);

        Map<String, Properties> config = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
        Properties properties = config.get(pid);
        if (properties != null) {
            Enumeration propertyNames = properties.propertyNames();
            while (propertyNames.hasMoreElements()) {
                String key = (String) propertyNames.nextElement();
                String value = (String) properties.get(key);
                CompositeDataSupport data = new CompositeDataSupport(compositeType,
                        new String[]{"key", "value"},
                        new String[]{key, value});
                table.put(data);
            }
        }
        return table;
    }

    public void setProperty(String groupName, String pid, String key, String value) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        Map<String, Properties> configurationMap = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        if (configurationMap != null) {
            // update the distributed map
            Properties properties = configurationMap.get(pid);
            if (properties == null) {
                properties = new Properties();
            }
            properties.put(key, value);
            configurationMap.put(pid, properties);

            // broadcast the cluster event
            RemoteConfigurationEvent event = new RemoteConfigurationEvent(pid);
            event.setSourceGroup(group);
            eventProducer.produce(event);
        } else {
            throw new IllegalStateException("Configuration distributed map not found for cluster group " + groupName);
        }
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
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

}
