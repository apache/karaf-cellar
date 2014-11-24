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
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * The ConfigurationSynchronizer is called when Cellar starts or when a node joins a cluster group.
 * The purpose is to synchronize local configurations with the configurations in the cluster groups.
 */
public class ConfigurationSynchronizer extends ConfigurationSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationSynchronizer.class);

    private EventProducer eventProducer;

    public ConfigurationSynchronizer() {
        // nothing to do
    }

    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                sync(group);
            }
        }
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Sync node and cluster states, depending of the sync policy.
     *
     * @param group the target cluster group.
     */
    @Override
    public void sync(Group group) {
        String policy = getSyncPolicy(group);
        if (policy != null && policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR CONFIG: sync policy is set as 'cluster' for cluster group " + group.getName());
            if (clusterManager.listNodesByGroup(group).size() == 1 && clusterManager.listNodesByGroup(group).contains(clusterManager.getNode())) {
                LOGGER.debug("CELLAR CONFIG: node is the first and only member of the group, pushing state");
                push(group);
            } else {
                LOGGER.debug("CELLAR CONFIG: pulling state");
                pull(group);
            }
        }
        if (policy != null && policy.equalsIgnoreCase("node")) {
            LOGGER.debug("CELLAR CONFIG: sync policy is set as 'cluster' for cluster group " + group.getName());
            push(group);
        }
    }

    /**
     * Pull the configuration from a cluster group to update the local ones.
     *
     * @param group the cluster group where to get the configurations.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR CONFIG: pulling configurations from cluster group {}", groupName);

            Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                for (String clusterConfiguration : clusterConfigurations.keySet()) {
                    if (isAllowed(group, Constants.CATEGORY, clusterConfiguration, EventType.INBOUND)) {
                        Dictionary clusterDictionary = clusterConfigurations.get(clusterConfiguration);
                        try {
                            // update the local configuration if needed
                            Configuration localConfiguration = configurationAdmin.getConfiguration(clusterConfiguration, null);
                            Dictionary localDictionary = localConfiguration.getProperties();
                            if (localDictionary == null)
                                localDictionary = new Properties();

                            localDictionary = filter(localDictionary);
                            if (!equals(localDictionary, clusterDictionary)) {
                                localConfiguration.update(localDictionary);
                                persistConfiguration(configurationAdmin, clusterConfiguration, localDictionary);
                            }
                        } catch (IOException ex) {
                            LOGGER.error("CELLAR CONFIG: failed to read local configuration", ex);
                        }
                    } else  LOGGER.trace("CELLAR CONFIG: configuration with PID {} is marked BLOCKED INBOUND for cluster group {}", clusterConfiguration, groupName);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push local configurations to a cluster group.
     *
     * @param group the cluster group where to update the configurations.
     */
    public void push(Group group) {

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR CONFIG: cluster event producer is OFF");
            return;
        }

        if (group != null) {
            String groupName = group.getName();
            LOGGER.debug("CELLAR CONFIG: pushing configurations to cluster group {}", groupName);
            Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                Configuration[] localConfigurations;
                try {
                    localConfigurations = configurationAdmin.listConfigurations(null);
                    for (Configuration localConfiguration : localConfigurations) {
                        String pid = localConfiguration.getPid();
                        // check if the pid is marked as local.
                        if (isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND)) {
                            Dictionary localDictionary = localConfiguration.getProperties();
                            localDictionary = filter(localDictionary);
                            // update the configurations in the cluster group
                            clusterConfigurations.put(pid, dictionaryToProperties(localDictionary));
                            // broadcast the cluster event
                            ClusterConfigurationEvent event = new ClusterConfigurationEvent(pid);
                            event.setSourceGroup(group);
                            eventProducer.produce(event);
                        } else
                            LOGGER.trace("CELLAR CONFIG: configuration with PID {} is marked BLOCKED OUTBOUND for cluster group {}", pid, groupName);
                    }
                } catch (IOException ex) {
                    LOGGER.error("CELLAR CONFIG: failed to read configuration (IO error)", ex);
                } catch (InvalidSyntaxException ex) {
                    LOGGER.error("CELLAR CONFIG: failed to read configuration (invalid filter syntax)", ex);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Get the bundle sync policy for the given cluster group.
     *
     * @param group the cluster group.
     * @return the current bundle sync policy for the given cluster group.
     */
    @Override
    public String getSyncPolicy(Group group) {
        String groupName = group.getName();
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                return properties.get(propertyKey).toString();
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR CONFIG: error while retrieving the sync policy", e);
        }

        return "disabled";
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
