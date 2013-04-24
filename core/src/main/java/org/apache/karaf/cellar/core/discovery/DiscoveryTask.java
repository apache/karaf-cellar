/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.core.discovery;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.cellar.core.utils.CellarUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiscoveryTask implements Runnable {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(DiscoveryTask.class);

    private List<DiscoveryService> discoveryServices;
    private ConfigurationAdmin configurationAdmin;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void init() {
    	LOGGER.debug("CELLAR DISCOVERY: a new Task initialized");
        scheduler.scheduleWithFixedDelay(this, 10, 10, TimeUnit.SECONDS);
    }

    public void destroy() {
    	LOGGER.debug("CELLAR DISCOVERY: task is being destroyed");
        scheduler.shutdown();
    }

    @Override
    public void run() {
        LOGGER.trace("CELLAR DISCOVERY: starting the discovery task");

        if (configurationAdmin != null) {
            Set<String> members = new LinkedHashSet<String>();
            if (discoveryServices != null && !discoveryServices.isEmpty()) {
                for (DiscoveryService service : discoveryServices) {
                    service.refresh();
                    Set<String> discovered = service.discoverMembers();
                    members.addAll(discovered);
                    LOGGER.trace("CELLAR DISCOVERY: service {} found members {}", service, discovered);
                }
                try {
                	LOGGER.trace("CELLAR DISCOVERY: retrieving configuration for PID={}", Discovery.PID);
                    Configuration configuration = configurationAdmin.getConfiguration(Discovery.PID);
                    Dictionary properties = configuration.getProperties();
                    if (properties == null) {
                    	// this is a new configuration ...
                    	LOGGER.trace("CELLAR DISCOVERY: configuration is new");
                    	properties = new Hashtable();
                    }
                    String newMemberText = CellarUtils.createStringFromSet(members, true);
                    String memberText = (String) properties.get(Discovery.MEMBERS_PROPERTY_NAME);
                    if (newMemberText != null && newMemberText.length() > 0 && !newMemberText.equals(memberText)) {
                        properties.put(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME, newMemberText);
                        LOGGER.trace("CELLAR DISCOVERY: adding a new member {} to configuration and updating it", newMemberText);
                        configuration.update(properties);
                    } else {
                    	LOGGER.trace("CELLAR DISCOVERY: found a valid member in the configuration will skip");
                    }
                } catch (IOException e) {
                    LOGGER.error("CELLAR DISCOVERY: failed to update member list", e);
                }
            } else {
            	LOGGER.trace("CELLAR DISCOVERY: no discovery services found ... ");
            }
        } else {
        	LOGGER.trace("CELLAR DISCOVERY: no config admin found");
        }
    }

    public List<DiscoveryService> getDiscoveryServices() {
        return discoveryServices;
    }

    public void setDiscoveryServices(List<DiscoveryService> discoveryServices) {
        this.discoveryServices = discoveryServices;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
