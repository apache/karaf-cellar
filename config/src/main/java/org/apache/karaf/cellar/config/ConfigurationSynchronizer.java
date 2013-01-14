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
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration synchronizer.
 */
public class ConfigurationSynchronizer extends ConfigurationSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationSynchronizer.class);

    private EventProducer eventProducer;

    /**
     * Constructor
     */
    public ConfigurationSynchronizer() {

    }

    /**
     * Registration method
     */
    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else LOGGER.warn("CELLAR CONFIG: sync is disabled for cluster group {}", group.getName());
            }
        }
    }

    /**
     * Destruction method
     */
    public void destroy() {

    }

    /**
     * Gets the configuration from the distributed map.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Map<String, Properties> distributedConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                for (String pid : distributedConfigurations.keySet()) {
                    if (isAllowed(group, Constants.CATEGORY, pid, EventType.INBOUND)) {
                        Dictionary remoteDictionary = distributedConfigurations.get(pid);
                        try {
                            // update the local configuration if needed
                            Configuration conf = configurationAdmin.getConfiguration(pid, null);
                            Dictionary localDictionary = conf.getProperties();
                            if (localDictionary == null)
                                localDictionary = new Properties();

                            localDictionary = filter(localDictionary);
                            if (!equals(localDictionary, remoteDictionary)) {
                                conf.update(localDictionary);
                                persistConfiguration(configurationAdmin, pid, localDictionary);
                            }
                        } catch (IOException ex) {
                            LOGGER.error("CELLAR CONFIG: failed to read distributed map", ex);
                        }
                    } else  LOGGER.warn("CELLAR CONFIG: configuration with PID {} is marked as BLOCKED INBOUND", pid);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Publish local configuration to the cluster.
     */
    public void push(Group group) {

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR CONFIG: cluster event producer is OFF");
            return;
        }

        if (group != null) {
            String groupName = group.getName();
            Map<String, Properties> distributedConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                Configuration[] configs;
                try {
                    configs = configurationAdmin.listConfigurations(null);
                    for (Configuration conf : configs) {
                        String pid = conf.getPid();
                        // check if the pid is marked as local.
                        if (isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND)) {
                            Dictionary localDictionary = conf.getProperties();
                            localDictionary = filter(localDictionary);
                            // update the distributed map
                            distributedConfigurations.put(pid, dictionaryToProperties(localDictionary));
                            // broadcast the cluster event
                            RemoteConfigurationEvent event = new RemoteConfigurationEvent(pid);
                            event.setSourceGroup(group);
                            eventProducer.produce(event);
                        } else
                            LOGGER.warn("CELLAR CONFIG: configuration with PID {} is marked as BLOCKED OUTBOUND", pid);
                    }
                } catch (IOException ex) {
                    LOGGER.error("CELLAR CONFIG: failed to read the distributed map (IO error)", ex);
                } catch (InvalidSyntaxException ex) {
                    LOGGER.error("CELLAR CONFIG: failed to read the distributed map (invalid filter syntax)", ex);
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    @Override
    public Boolean isSyncEnabled(Group group) {
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                String propertyValue = (String) properties.get(propertyKey);
                result = Boolean.parseBoolean(propertyValue);
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR CONFIG: error while checking if sync is enabled", e);
        }
        return result;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
