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
package org.apache.karaf.cellar.config;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * LocalConfigurationListener is listening for local configuration changes.
 * When a local configuration change occurs, this listener updates the cluster group and broadcasts a cluster config event.
 */
public class LocalConfigurationListener extends ConfigurationSupport implements ConfigurationListener {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalConfigurationListener.class);

    private EventProducer eventProducer;

    /**
     * Callback method called when a local configuration changes.
     *
     * @param event the local configuration event.
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {

        if (!isEnabled()) {
            LOGGER.trace("CELLAR CONFIG: local listener is disabled");
            return;
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR CONFIG: cluster event producer is OFF");
            return;
        }

        String pid = event.getPid();

        Set<Group> groups = groupManager.listLocalGroups();

        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                // check if the pid is allowed for outbound.
                if (isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND)) {

                    Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group.getName());

                    try {
                        if (event.getType() == ConfigurationEvent.CM_DELETED) {

                            if (clusterConfigurations.containsKey(pid)) {
                                String filename = (String) clusterConfigurations.get(pid).get(KARAF_CELLAR_FILENAME);
                                List<String> matchingPids = new ArrayList<String>();
                                for (Map.Entry<String, Properties> entry : clusterConfigurations.entrySet()) {
                                    if (filename.equals(entry.getValue().get(KARAF_CELLAR_FILENAME))) {
                                        matchingPids.add(entry.getKey());
                                    }
                                }
                                for (String matchingPid : matchingPids) {
                                    // update the configurations in the cluster group
                                    clusterConfigurations.put(matchingPid, getDeletedConfigurationMarker(clusterConfigurations.get(matchingPid)));
                                }
                                // send the cluster event
                                ClusterConfigurationEvent clusterConfigurationEvent = new ClusterConfigurationEvent(pid);
                                clusterConfigurationEvent.setType(event.getType());
                                clusterConfigurationEvent.setSourceNode(clusterManager.getNode());
                                clusterConfigurationEvent.setSourceGroup(group);
                                clusterConfigurationEvent.setLocal(clusterManager.getNode());
                                eventProducer.produce(clusterConfigurationEvent);
                            }
                        } else {

                            Configuration conf = configurationAdmin.getConfiguration(pid, null);
                            Dictionary localDictionary = conf.getProperties();
                            localDictionary = filter(localDictionary);

                            Properties distributedDictionary = clusterConfigurations.get(pid);

                            if (!equals(localDictionary, distributedDictionary) && canDistributeConfig(localDictionary)) {
                                // update the configurations in the cluster group
                                clusterConfigurations.put(pid, dictionaryToProperties(localDictionary));
                                // send the cluster event
                                ClusterConfigurationEvent clusterConfigurationEvent = new ClusterConfigurationEvent(pid);
                                clusterConfigurationEvent.setSourceGroup(group);
                                clusterConfigurationEvent.setSourceNode(clusterManager.getNode());
                                clusterConfigurationEvent.setLocal(clusterManager.getNode());
                                eventProducer.produce(clusterConfigurationEvent);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("CELLAR CONFIG: failed to update configuration with PID {} in the cluster group {}", pid, group.getName(), e);
                    }
                } else LOGGER.trace("CELLAR CONFIG: configuration with PID {} is marked BLOCKED OUTBOUND for cluster group {}", pid, group.getName());
            }
        }
    }

    /**
     * Check if the local config listener is enabled in the etc/org.apache.karaf.cellar.groups.cfg.
     *
     * @return true if enabled, false else.
     */
    private boolean isEnabled() {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String value = properties.get(Constants.CATEGORY + Configurations.SEPARATOR + Configurations.LISTENER).toString();
                return Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR CONFIG: can't check listener configuration", e);
        }
        return false;
    }

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
