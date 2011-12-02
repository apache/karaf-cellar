/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.hazelcast.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.core.discovery.Discovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastConfigurationManager {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastServiceFactory.class);

    public static final String USERNAME="username";
    public static final String PASSWORD="password";

    public static final String MULTICAST_ENABLED="multicastEnabled";
    public static final String MULTICAST_GROUP="multicastGroup";
    public static final String MULTICAST_PORT="multicastPort";
    public static final String MULTICAST_TIMEOUT_IN_SECONDS="multicastTimeoutSeconds";

    public static final String TCPIP_ENABLED="tcpIpEnabled";
    public static final String TCPIP_MEMBERS="tcpIpMembers";

    private String username = "cellar";
    private String password = "pass";

    private boolean multicastEnabled = MulticastConfig.DEFAULT_ENABLED;
    private String multicastGroup = MulticastConfig.DEFAULT_MULTICAST_GROUP;
    private int multicastPort = MulticastConfig.DEFAULT_MULTICAST_PORT;
    private int multicastTimeoutSeconds = MulticastConfig.DEFAULT_MULTICAST_TIMEOUT_SECONDS;

    private boolean tcpIpEnabled = false;
    private String tcpIpMembers = "";
    private Set<String> tcpIpMemberSet = new LinkedHashSet<String>();
    private Set<String> discoveredMemberSet = new LinkedHashSet<String>();

    private String xmlConfigLocation = System.getProperty("karaf.home") + "/etc/hazelcast.xml";

    /**
     * Creates or Updates Hazelcast Instance.
     *
     * @param properties
     */
    public boolean isUpdated(Map properties) {
            Boolean updated = Boolean.FALSE;
            //We need it to properly instantiate Hazelcast.
            if (properties != null) {
                if (properties.containsKey(USERNAME)) {
                    String newUsername = (String) properties.get(USERNAME);
                    if (username != null && newUsername != null && !username.endsWith(newUsername)) {
                        LOGGER.info("Hazelcast username has been changed from {} to {}", username, newUsername);
                        this.username = newUsername;
                        updated = Boolean.TRUE;
                    }
                }

                if (properties.containsKey(PASSWORD)) {
                    String newPassword = (String) properties.get(PASSWORD);
                    if (password != null && !password.equals(newPassword)) {
                        LOGGER.info("Hazelcast password has been changed from {} to {}", password, newPassword);
                        this.password = newPassword;
                        updated = Boolean.TRUE;
                    }
                }

                if (properties.containsKey(MULTICAST_ENABLED)) {
                    Boolean newMulticastEnabled = Boolean.parseBoolean((String) properties.get(MULTICAST_ENABLED));
                    if (multicastEnabled != newMulticastEnabled) {
                        LOGGER.info("Hazelcast multicastEnabled has been changed from {} to {}", multicastEnabled, newMulticastEnabled);
                        this.multicastEnabled = newMulticastEnabled;
                        updated = Boolean.TRUE;
                    }
                }

                if (properties.containsKey(MULTICAST_GROUP)) {
                    String newMulticastGroup = (String) properties.get(MULTICAST_GROUP);
                    if (multicastGroup != null && newMulticastGroup != null && !multicastGroup.endsWith(newMulticastGroup)) {
                        LOGGER.info("Hazelcast multicastGroup has been changed from {} to {}", multicastGroup, newMulticastGroup);
                        this.multicastGroup = newMulticastGroup;
                        updated = Boolean.TRUE;
                    }

                }

                if (properties.containsKey(MULTICAST_PORT)) {
                    try {
                        int newMulticastPort = Integer.parseInt((String) properties.get(MULTICAST_PORT));
                        if (multicastPort != 0 && multicastPort != newMulticastPort) {
                            LOGGER.info("Hazelcast multicastPort has been changed from {} to {}", multicastPort, newMulticastPort);
                            this.multicastPort = newMulticastPort;
                            updated = Boolean.TRUE;
                        }
                    } catch (NumberFormatException ex) {
                        LOGGER.warn("Could not parse port number", ex);
                    }
                }

                if (properties.containsKey(MULTICAST_TIMEOUT_IN_SECONDS)) {
                    try {
                        int newMulticastTimeoutSeconds = Integer.parseInt((String) properties.get(MULTICAST_TIMEOUT_IN_SECONDS));
                        if (multicastTimeoutSeconds != 0 && multicastTimeoutSeconds != newMulticastTimeoutSeconds) {
                            LOGGER.info("Hazelcast multicastTimeoutSeconds has been changed from {} to {}", multicastTimeoutSeconds, newMulticastTimeoutSeconds);
                            this.multicastTimeoutSeconds = newMulticastTimeoutSeconds;
                            updated = Boolean.TRUE;
                        }
                    } catch (NumberFormatException ex) {
                        LOGGER.warn("Could not parse multicast timeout in seconds", ex);
                    }
                }

                if (properties.containsKey(TCPIP_ENABLED)) {
                    Boolean newTcpIpEnabled = Boolean.parseBoolean((String) properties.get(TCPIP_ENABLED));
                    if (tcpIpEnabled != newTcpIpEnabled) {
                        LOGGER.info("Hazelcast tcpIpEnabled has been changed from {} to {}", tcpIpEnabled, newTcpIpEnabled);
                        this.tcpIpEnabled = newTcpIpEnabled;
                        updated = Boolean.TRUE;
                    }
                }

                if (properties.containsKey(TCPIP_MEMBERS)) {
                    Set<String> newTcpIpMemberSet = createSetFromString((String) properties.get(TCPIP_MEMBERS));
                    if (!collectionEquals(tcpIpMemberSet, newTcpIpMemberSet)) {
                        LOGGER.info("Hazelcast tcpIpMemberSet has been changed from {} to {}", tcpIpMemberSet, newTcpIpMemberSet);
                        tcpIpMemberSet = newTcpIpMemberSet;
                        updated = Boolean.TRUE;
                    }
                }


                if (properties.containsKey(TCPIP_MEMBERS)) {
                    Set<String> newDiscoveredMemberSet = createSetFromString((String) properties.get(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME));
                    if (!collectionEquals(discoveredMemberSet, newDiscoveredMemberSet)) {
                        LOGGER.info("Hazelcast discoveredMemberSet has been changed from {} to {}", discoveredMemberSet, newDiscoveredMemberSet);
                        discoveredMemberSet = newDiscoveredMemberSet;
                        updated = Boolean.TRUE;
                    }
                }
            }

            return updated;
    }
     /**
     * Builds a Hazelcast {@link com.hazelcast.config.Config}
     *
     * @return
     */
    public Config getHazelcastConfig() {
        System.setProperty("hazelcast.config", xmlConfigLocation);
        Config config = new XmlConfigBuilder().build();
        config.setGroupConfig(buildGroupConfig());
        config.getNetworkConfig().getJoin().setMulticastConfig(buildMulticastConfig());
        config.getNetworkConfig().getJoin().setTcpIpConfig(buildTcpIpConfig());
        return config;
    }

    /**
     * Builds a {@link com.hazelcast.config.GroupConfig}
     *
     * @return
     */
    public GroupConfig buildGroupConfig() {
        GroupConfig groupConfig = new GroupConfig();
        groupConfig.setName(username);
        groupConfig.setPassword(password);
        return groupConfig;
    }

    /**
     * Builds a {@link com.hazelcast.config.MulticastConfig}
     *
     * @return
     */
    public MulticastConfig buildMulticastConfig() {
        MulticastConfig multicastConfig = new MulticastConfig();
        multicastConfig.setEnabled(multicastEnabled);
        multicastConfig.setMulticastPort(multicastPort);
        multicastConfig.setMulticastGroup(multicastGroup);
        multicastConfig.setMulticastTimeoutSeconds(multicastTimeoutSeconds);
        return multicastConfig;
    }

    /**
     * Builds a {@link com.hazelcast.config.TcpIpConfig}.
     *
     * @return
     */
    public TcpIpConfig buildTcpIpConfig() {
        TcpIpConfig tcpIpConfig = new TcpIpConfig();
        tcpIpConfig.setEnabled(tcpIpEnabled);
        tcpIpConfig.setMembers(new ArrayList(tcpIpMemberSet));
        if(discoveredMemberSet != null && !discoveredMemberSet.isEmpty() && tcpIpConfig.getMembers() != null) {
            tcpIpConfig.getMembers().addAll(new ArrayList(discoveredMemberSet));
        }
        return tcpIpConfig;
    }

    /**
     * Converts a comma delimited String to a Set of Strings.
     *
     * @param text
     * @return
     */
    private Set<String> createSetFromString(String text) {
        Set<String> result = new LinkedHashSet<String>();
        if (text != null) {
            String[] items = text.split(",");
            if (items != null && items.length > 0) {

                for (String item : items) {
                    if(item != null && item.length() > 0) {
                     result.add(item.trim());
                    }
                }
            }
        }
        return result;
    }

        /**
     * Returns true if both {@link java.util.Collection}s contain exactly the same items (order doesn't matter).
     *
     * @param col1
     * @param col2
     * @return
     */
    public boolean collectionEquals(Collection col1, Collection col2) {
        return collectionSubset(col1, col2) && collectionSubset(col2, col1);
    }

    /**
     * Returns true if one {@link Collection} contains all items of the others
     *
     * @param source
     * @param target
     * @return
     */
    private boolean collectionSubset(Collection source, Collection target) {
        if (source == null && target == null) {
            return true;
        } else if (source == null || target == null) {
            return false;
        } else if (source.isEmpty() && target.isEmpty()) {
            return true;
        } else {
            for (Object item : source) {
                if (!target.contains(item)) {
                    return false;
                }
            }
            return true;
        }
    }
}
