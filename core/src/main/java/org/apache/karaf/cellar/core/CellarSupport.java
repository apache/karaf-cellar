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
package org.apache.karaf.cellar.core;

import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cellar generic support. This class provides a set of util methods used by other classes.
 */
public class CellarSupport {

    protected static final transient Logger LOGGER = LoggerFactory.getLogger(CellarSupport.class);

    protected ClusterManager clusterManager;
    protected GroupManager groupManager;
    protected ConfigurationAdmin configurationAdmin;

    /**
     * If the entry is not present in the list, add it. If the entry is present in the list, remove it.
     *
     * @param listType the comma separated list of resources.
     * @param group the cluster group name.
     * @param category the resource category name.
     * @param entry the entry to switch.
     */
    public void switchListEntry(String listType, String group, String category, EventType type, String entry) throws Exception {
        if (group != null) {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary dictionary = configuration.getProperties();
            if (dictionary == null) {
                dictionary = new Properties();
            }
            String key = group + Configurations.SEPARATOR + category + Configurations.SEPARATOR + listType + Configurations.SEPARATOR + type.name().toLowerCase();
            if (dictionary.get(key) != null) {
                String value = dictionary.get(key).toString();
                if (value.contains(entry)) {
                    value = value.replace(entry, "");
                } else {
                    value = value + "," + entry;
                }
                if (value.startsWith(",")) value = value.substring(1);
                if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
                value = value.replace("\n\n", "");
                value = value.replace(",,", ",");
                dictionary.put(key, value);
            } else {
                dictionary.put(key, entry);
            }
            configuration.update(dictionary);
        }
    }

    /**
     * Get a set of resources in the Cellar cluster groups configuration.
     *
     * @param listType the comma separated list of resources.
     * @param group the cluster group name.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, String group, String category, EventType type) {
        Set<String> result = null;
        if (group != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
                Dictionary<String, Object> dictionary = configuration.getProperties();
                if (dictionary != null) {
                    String parent = (String) dictionary.get(group + Configurations.SEPARATOR + Configurations.PARENT);
                    if (parent != null) {
                        result = getListEntries(listType, parent, category, type);
                    }

                    String propertyName = group + Configurations.SEPARATOR + category + Configurations.SEPARATOR + listType + Configurations.SEPARATOR + type.name().toLowerCase();
                    String propertyValue = (String) dictionary.get(propertyName);
                    if (propertyValue != null) {
                        propertyValue = propertyValue.replaceAll("\n","");
                        String[] itemList = propertyValue.split(Configurations.DELIMETER);

                        if (itemList != null && itemList.length > 0) {
                            if (result == null) {
                                result = new HashSet<String>();
                            }
                            for (String item : itemList) {
                                if (item != null) {
                                    result.add(item.trim());
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error looking up for clustering group configuration cfg");
            }
        }
        return result;
    }

    /**
     * Get the resources in the Cellar cluster groups configuration.
     *
     * @param listType the comma separated string of resources.
     * @param groups the cluster groups names.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, Collection<String> groups, String category, EventType type) {
        Set<String> result = null;
        if (groups != null && !groups.isEmpty()) {
            for (String group : groups) {
                Set<String> items = getListEntries(listType, group, category, type);
                if (items != null && !items.isEmpty()) {
                    if (result == null)
                        result = new HashSet<String>();
                    result.addAll(items);
                }
            }
        }
        return result;
    }

    /**
     * Get a set of resources in the Cellar cluster groups configuration.
     *
     * @param listType a comma separated string of resources.
     * @param group the cluster group.
     * @param category the resource category name.
     * @param type the event type (inbound, outbound).
     * @return the set of resources.
     */
    public Set<String> getListEntries(String listType, Group group, String category, EventType type) {
        Set<String> result = null;
        if (group != null) {
            String groupName = group.getName();
            Set<String> items = getListEntries(listType, groupName, category, type);
            if (items != null && !items.isEmpty()) {
                if (result == null)
                    result = new HashSet<String>();
                result.addAll(items);
            }
        }
        return result;
    }

    /**
     * Check if a resource is allowed for a type of cluster event.
     *
     * @param group the cluster group.
     * @param category the resource category name.
     * @param event the resource name.
     * @param type the event type (inbound, outbound).
     */
    public Boolean isAllowed(Group group, String category, String event, EventType type) {
        Set<String> whiteList = getListEntries(Configurations.WHITELIST, group, category, type);
        Set<String> blackList = getListEntries(Configurations.BLACKLIST, group, category, type);

        if (blackList == null || whiteList == null) {
            // If one list is missing, we probably have a configuration issue - do not synchronize anything
            LOGGER.warn("No whitelist/blacklist found for " + group.getName() + ", check your configuration !");
            return false;
        }

        // if no white listed items we assume all are accepted.
        Boolean result = true;
        if (!whiteList.isEmpty()) {
            result = false;
            for (String whiteListItem : whiteList) {
                if (wildCardMatch(event, whiteListItem)) {
                    result = true;
                    break;
                }
            }
        }

        if (result) {
            // we passed whitelist, now check the blacklist
            // if any blackList item matched, then false is returned.
            for (String blackListItem : blackList) {
                if (wildCardMatch(event, blackListItem)) {
                    return false;
                }
            }
        }

        return result;
    }

    /**
     * Check if a string match a regex.
     *
     * @param item the string to check.
     * @param pattern the regex pattern.
     * @return true if the item string matches the pattern, false else.
     */
    protected boolean wildCardMatch(String item, String pattern) {
        if (item == null || pattern == null) {
            return false;
        }
        // update the pattern to have a valid regex pattern
        pattern = pattern.replace("*", ".*");
        // use the regex
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(item);
        return m.matches();
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
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

    @SuppressWarnings("unchecked")
    protected Map<String, Boolean> getSynchronizerMap() {
        return clusterManager.getMap("org.apache.karaf.cellar.synchronizers");
    }
}
