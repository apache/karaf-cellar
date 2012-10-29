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
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cellar support.
 */
public class CellarSupport {

    protected static final transient Logger LOGGER = LoggerFactory.getLogger(CellarSupport.class);

    protected ClusterManager clusterManager;
    protected GroupManager groupManager;
    protected ConfigurationAdmin configurationAdmin;

    /**
     * Lists the BlackList/WhiteList for the specified category..
     *
     * @param listType whitelist or blacklist.
     * @param category  the lookup category.
     * @param group the target group name.
     * @return the corresponding list entries.
     */
    public Set<String> getListEntries(String listType, String group, String category, EventType type) {
        Set<String> result = null;
        if (group != null) {
            try {
                Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
                Dictionary<String, String> dictionary = configuration.getProperties();
                if (dictionary != null) {
                    String parent = dictionary.get(group + Configurations.SEPARATOR + Configurations.PARENT);
                    if (parent != null) {
                        result = getListEntries(listType, parent, category, type);
                    }

                    String propertyName = group + Configurations.SEPARATOR + category + Configurations.SEPARATOR + listType + Configurations.SEPARATOR + type.name().toLowerCase();
                    String propertyValue = dictionary.get(propertyName);
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
     * Lists the BlackList for the specified feature.
     *
     * @param category
     * @param groups
     * @param category
     * @param type
     * @return
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
     * Lists the BlackList for the specified feature.
     *
     * @param category
     * @param category
     * @param type
     * @return
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
     * Returns true if the specified event is allowed.
     *
     * @param category
     * @param event
     * @param type
     * @return
     */
    public Boolean isAllowed(Group group, String category, String event, EventType type) {
        Boolean result = true;
        Set<String> whiteList = getListEntries(Configurations.WHITELIST, group, category, type);
        Set<String> blackList = getListEntries(Configurations.BLACKLIST, group, category, type);

        //If no white listed items we assume all are accepted.
        if (whiteList != null && !whiteList.isEmpty()) {
            result = false;
            for (String whiteListItem : whiteList) {
                if (wildCardMatch(event, whiteListItem)) {
                    result = true;
                }
            }
        }

        //If any blackList item matched, then false is returned.
        if (blackList != null && !blackList.isEmpty()) {
            for (String blackListItem : blackList) {
                if (wildCardMatch(event, blackListItem)) {
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Matches text using a pattern containing wildcards.
     *
     * @param item
     * @param pattern
     * @return
     */
    protected boolean wildCardMatch(String item, String pattern) {
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

}
