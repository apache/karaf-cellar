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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
        scheduler.scheduleWithFixedDelay(this, 10, 10, TimeUnit.SECONDS);
    }

    public void destroy() {
        scheduler.shutdown();
    }

    @Override
    public void run() {
        LOGGER.trace("CELLAR DISCOVERY: Starting the discovery task.");
        if (configurationAdmin != null) {
            Set<String> members = new LinkedHashSet<String>();
            if (discoveryServices != null && !discoveryServices.isEmpty()) {
                for (DiscoveryService service : discoveryServices) {
                    service.refresh();
                    Set<String> discovered = service.discoverMembers();
                    members.addAll(discovered);
                }
            try {
                Configuration configuration = configurationAdmin.getConfiguration(Discovery.PID);
                Dictionary properties = configuration.getProperties();
                String newMemberText = buildMemberList(members);
                String memberText = (String) properties.get(Discovery.MEMBERS_PROPERTY_NAME);
                if (newMemberText != null && newMemberText.length() > 0 && !newMemberText.equals(memberText)) {
                    properties.put(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME, newMemberText);
                    configuration.update(properties);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to update member list", e);
            }
            }
        }
    }

    /**
     * Creates a comma delimited list of members.
     *
     * @param members
     * @return
     */
    private String buildMemberList(Set<String> members) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> memberIterator = members.iterator();
        while (memberIterator.hasNext()) {
            builder.append(memberIterator.next());
            if (memberIterator.hasNext()) {
                builder.append(",");
            }
        }
        return builder.toString();
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
