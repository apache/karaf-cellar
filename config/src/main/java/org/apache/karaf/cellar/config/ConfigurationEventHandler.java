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
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration event handler.
 */
public class ConfigurationEventHandler extends ConfigurationSupport implements EventHandler<RemoteConfigurationEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ConfigurationEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.configuration.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private Node node;

    /**
     * Handles
     *
     * @param event
     */
    public void handle(RemoteConfigurationEvent event) {

        if (event == null || event.getSourceGroup() == null || node == null || node.equals(event.getSourceNode()))
            return;

        Group group = event.getSourceGroup();
        String groupName = group.getName();

        Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + groupName);

        if (eventSwitch.getStatus().equals(SwitchStatus.ON)) {
            String pid = event.getId();
            //Check if the pid is marked as local.
            if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, pid, EventType.INBOUND)) {
                Properties dictionary = configurationTable.get(pid);
                Configuration conf;
                try {
                    conf = configurationAdmin.getConfiguration(pid);
                    //Update the configurationTable.
                    if (conf != null && dictionary != null) {
                        Dictionary existingConfiguration = filterDictionary(conf.getProperties());
                        if (!dictionariesEqual(dictionary, existingConfiguration)) {
                            conf.update(preparePull(dictionary));
                        }
                        LOGGER.info("CELLAR CONFIG EVENT: pull configuration {}", pid);
                    }
                } catch (IOException ex) {
                    LOGGER.error("Failed to read remote configurationTable", ex);
                }
            } else LOGGER.debug("Configuration with pid {} is marked as local.", pid);
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

    public Switch getSwitch() {
        return eventSwitch;
    }

    public Class<RemoteConfigurationEvent> getType() {
        return RemoteConfigurationEvent.class;
    }

}
