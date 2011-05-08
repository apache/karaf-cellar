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
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Local configuration listener.
 */
public class LocalConfigurationListener extends ConfigurationSupport implements ConfigurationListener {

    private static final Logger logger = LoggerFactory.getLogger(LocalConfigurationListener.class);

    private List<EventProducer> producerList;

    private Node node;

    /**
     * Handle local configuration events.
     * If the event is a pending event stop it. Else broadcast it to the cluster.
     *
     * @param event
     */
    public void configurationEvent(ConfigurationEvent event) {
        String pid = event.getPid();

        Set<Group> groups = groupManager.listLocalGroups();

        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                //Check if the pid is allowed for outbound.
                if (isAllowed(group, Constants.CATEGORY, pid, EventType.OUTBOUND)) {
                    RemoteConfigurationEvent configurationEvent = new RemoteConfigurationEvent(pid);
                    configurationEvent.setSourceGroup(group);
                    configurationEvent.setSourceNode(node);
                    push(pid, group);
                    if (producerList != null && !producerList.isEmpty()) {
                        for (EventProducer producer : producerList) {
                            producer.produce(configurationEvent);
                        }
                    }
                } else logger.debug("Configuration with pid {} is marked as local.", pid);
            }
        }
    }

    /**
     * Push configuration with pid to the table.
     *
     * @param pid
     */

    protected void push(String pid, Group group) {
        String groupName = group.getName();

        Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations("(service.pid=" + pid + ")");
            for (Configuration configuration : configurations) {
                Properties properties = dictionaryToProperties(preparePush(filterDictionary(configuration.getProperties())));
                configurationTable.put(configuration.getPid(), properties);
            }
        } catch (IOException e) {
            logger.error("Failed to push configuration with pid:" + pid, e);
        } catch (InvalidSyntaxException e) {
            logger.error("Failed to retrieve configuration with pid:" + pid, e);
        }
    }

    /**
     * Initialization Method.
     */
    public void init() {
        if (clusterManager != null) {
            node = clusterManager.getNode();
        }
    }

    /**
     * Destruction Method.
     */
    public void destroy() {

    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }

}
