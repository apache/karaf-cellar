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
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigurationEventHandler handles received configuration cluster event.
 */
public class ConfigurationEventHandler extends ConfigurationSupport implements EventHandler<ClusterConfigurationEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.configuration.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void handle(ClusterConfigurationEvent event) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR CONFIG: {} switch is OFF, cluster event not handled", SWITCH_ID);
            return;
        }
        
        if (groupManager == null) {
        	//in rare cases for example right after installation this happens!
        	LOGGER.error("CELLAR CONFIG: retrieved event {} while groupManager is not available yet!", event);
        	return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR CONFIG: node is not part of the event cluster group {}",event.getSourceGroup().getName());
            return;
        }

        Group group = event.getSourceGroup();
        String groupName = group.getName();

        Map<String, Properties> clusterConfigurations = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

        String pid = event.getId();

        if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, pid, EventType.INBOUND)) {

            Properties clusterDictionary = clusterConfigurations.get(pid);
            try {
                // update the local configuration
                Configuration[] localConfigurations = configurationAdmin.listConfigurations("(service.pid=" + pid + ")");
                if (event.getType() == ConfigurationEvent.CM_DELETED) {
                    // delete the configuration
                    if (localConfigurations != null && localConfigurations.length > 0) {
                        localConfigurations[0].delete();
                        deleteStorage(pid);
                    }
                } else {
                    if (clusterDictionary != null) {
                        Configuration localConfiguration = configurationAdmin.getConfiguration(pid, null);
                        Dictionary localDictionary = localConfiguration.getProperties();
                        if (localDictionary == null)
                            localDictionary = new Properties();
                        localDictionary = filter(localDictionary);
                        if (!equals(clusterDictionary, localDictionary)) {
                            localConfiguration.update((Dictionary) clusterDictionary);
                            persistConfiguration(configurationAdmin, pid, clusterDictionary);
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("CELLAR CONFIG: failed to read cluster configuration", ex);
            }
        } else LOGGER.debug("CELLAR CONFIG: configuration PID {} is marked BLOCKED INBOUND for cluster group {}", pid, groupName);
    }

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Get the cluster configuration event handler switch.
     *
     * @return the cluster configuration event handler switch.
     */
    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.HANDLER + "." + this.getClass().getName()));
                if (status) {
                    eventSwitch.turnOn();
                } else {
                    eventSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            // nothing to do
        }
        return eventSwitch;
    }

    /**
     * Get the cluster event type.
     *
     * @return the cluster configuration event type.
     */
    @Override
    public Class<ClusterConfigurationEvent> getType() {
        return ClusterConfigurationEvent.class;
    }

}
