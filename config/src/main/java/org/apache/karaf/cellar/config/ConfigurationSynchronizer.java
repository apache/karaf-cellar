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
 * @author iocanel
 */
public class ConfigurationSynchronizer extends ConfigurationSupport implements Synchronizer {

    private static Logger logger = LoggerFactory.getLogger(ConfigurationSynchronizer.class);

    private List<EventProducer> producerList;

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
                }
            }
        }
    }

    /**
     * Destruction method
     */
    public void destroy() {

    }

    /**
     * Reads the configuration from the remote map.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                for (String pid : configurationTable.keySet()) {
                    //Check if the pid is marked as local.
                    if (isAllowed(group, Constants.CATEGORY, pid, EventType.INBOUND)) {
                        Properties dictionary = configurationTable.get(pid);
                        try {
                            Configuration conf = configurationAdmin.getConfiguration(pid);
                            //Update the configuration.
                            if (conf != null) {
                                //Mark the remote configuration event.
                                RemoteConfigurationEvent event = new RemoteConfigurationEvent(conf.getPid());
                                conf.update(preparePull(dictionary));
                            }
                        } catch (IOException ex) {
                            logger.error("Failed to read remote configuration", ex);
                        }
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Publishses local configuration to the cluster.
     */
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                Configuration[] configs;
                try {
                    configs = configurationAdmin.listConfigurations(null);
                    for (Configuration conf : configs) {
                        String pid = conf.getPid();
                        //Check if the pid is marked as local.
                        if (isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND)) {
                            Properties source = dictionaryToProperties(preparePush(filterDictionary(conf.getProperties())));
                            Properties target = configurationTable.get(pid);
                            if (target != null) {
                                boolean requiresUpdate = false;
                                if (source != null && source.keys() != null) {
                                    Enumeration keys = source.keys();
                                    while (keys.hasMoreElements()) {
                                        String key = (String) keys.nextElement();
                                        if (target.get(key) == null || target.get(key).equals(source.get(key))) {
                                            requiresUpdate = true;
                                            target.put(key, source.get(key));
                                        }
                                    }
                                    configurationTable.put(pid, target);
                                    if (requiresUpdate) {
                                        RemoteConfigurationEvent event = new RemoteConfigurationEvent(conf.getPid());
                                        if (producerList != null && !producerList.isEmpty()) {
                                            for (EventProducer producer : producerList) {
                                                producer.produce(event);
                                            }
                                        }

                                    }
                                    logger.info("Publishing PID:" + pid);
                                }
                            } else {
                                RemoteConfigurationEvent event = new RemoteConfigurationEvent(conf.getPid());
                                configurationTable.put(pid, source);
                                if (producerList != null && !producerList.isEmpty()) {
                                    for (EventProducer producer : producerList) {
                                        producer.produce(event);
                                    }
                                }
                                logger.info("Publishing PID:" + pid);
                            }
                        }
                    }
                } catch (IOException ex) {
                    logger.error("Failed to read remote configuration, due to I/O error:", ex);
                } catch (InvalidSyntaxException ex) {
                    logger.error("Failed to read remote configuration, due to invalid filter syntax:", ex);
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
            Dictionary<String, String> properties = configuration.getProperties();
            String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
            String propertyValue = properties.get(propertyKey);
            result = Boolean.parseBoolean(propertyValue);
        } catch (IOException e) {
            logger.error("Error while checking if sync is enabled.", e);
        }
        return result;
    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }
}
