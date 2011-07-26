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

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.core.discovery.DiscoveryTask;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.BundleContextAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A factory for a Hazelcast Instance, which integration with OSGi Service Registry and Config Admin.
 */
public class HazelcastServiceFactory implements BundleContextAware {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastServiceFactory.class);

    private String username = GroupConfig.DEFAULT_GROUP_NAME;
    private String password = GroupConfig.DEFAULT_GROUP_PASSWORD;

    private boolean multicastEnabled = MulticastConfig.DEFAULT_ENABLED;
    private String multicastGroup = MulticastConfig.DEFAULT_MULTICAST_GROUP;
    private int multicastPort = MulticastConfig.DEFAULT_MULTICAST_PORT;
    private int multicastTimeoutSeconds = MulticastConfig.DEFAULT_MULTICAST_TIMEOUT_SECONDS;

    private boolean tcpIpEnabled = true;
    private String tcpIpMembers = "";
    private Set<String> tcpIpMemberSet = new LinkedHashSet<String>();

    private BundleContext bundleContext;

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private DiscoveryTask discoveryTask;

    private Semaphore semaphore = new Semaphore(1);

    /**
     * Constructor
     */
    public HazelcastServiceFactory() {
        try {
            //Make sure that an the properties will be applied before an instance is created.
            semaphore.acquire();
        } catch (InterruptedException e) {
            LOGGER.error("Failed to acquire initialization semaphore.", e);
        }
    }

    public void init() {
        scheduler.scheduleAtFixedRate(discoveryTask, 0, 10, TimeUnit.SECONDS);
    }

    public void destroy() {
        scheduler.shutdown();
    }

    /**
     * Creates or Updates HazelcastInstace.
     *
     * @param properties
     */
    public void createOrUpdate(Map properties) {
        try {
            Boolean updated = Boolean.FALSE;
            //We need it to properly instantiate Hazelcast.
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            if (properties != null) {
                String newUsername = (String) properties.get("username");
                if (username != null && !username.endsWith(newUsername)) {
                    this.username = newUsername;
                    updated = Boolean.TRUE;
                }

                String newPassword = (String) properties.get("password");
                if (password != null && !password.equals(newPassword)) {
                    this.password = newPassword;
                }

                Boolean newMulticastEnabled = Boolean.parseBoolean((String) properties.get("multicastEnabled"));
                if (multicastEnabled != newMulticastEnabled) {
                    this.multicastEnabled = newMulticastEnabled;
                    updated = Boolean.TRUE;
                }

                String newMulticastGroup = (String) properties.get("multicastGroup");
                if (multicastGroup != null && !multicastGroup.endsWith(newMulticastGroup)) {
                    this.multicastGroup = newMulticastGroup;
                    updated = Boolean.TRUE;
                }

                int multicastPort = Integer.parseInt((String) properties.get("multicastPort"));
                if (multicastPort != 0) {
                    this.multicastPort = multicastPort;
                    updated = Boolean.TRUE;
                }

                int newMulticastTimeoutSeconds = Integer.parseInt((String) properties.get("multicastTimeoutSeconds"));
                if (multicastTimeoutSeconds != 0 && multicastTimeoutSeconds != newMulticastTimeoutSeconds) {
                    this.multicastTimeoutSeconds = newMulticastTimeoutSeconds;
                    updated = Boolean.TRUE;
                }

                Boolean newTcpIpEnabled = Boolean.parseBoolean((String) properties.get("tcpIpEnabled"));
                if (tcpIpEnabled != newTcpIpEnabled) {
                    this.tcpIpEnabled = newTcpIpEnabled;
                    updated = Boolean.TRUE;
                }

                String newTcpIpMembers = (String) properties.get("tcpIpMembers");
                if (tcpIpMembers != null && !tcpIpMembers.endsWith(newTcpIpMembers)) {
                    updated = Boolean.TRUE;

                    String[] members = tcpIpMembers.split(",");
                    if (members != null && members.length > 0) {
                        tcpIpMemberSet = new LinkedHashSet<String>();
                        for (String member : members) {
                            tcpIpMemberSet.add(member);
                        }
                    }
                }

                if (updated) {
                    HazelcastInstance instance = lookupInstance();
                    if (instance != null) {
                        try {
                            instance.getConfig().setGroupConfig(buildGroupConfig());
                            instance.getConfig().getNetworkConfig().getJoin().setMulticastConfig(buildMulticastConfig());
                            instance.getConfig().getNetworkConfig().getJoin().setTcpIpConfig(buildTcpIpConfig());
                            instance.getLifecycleService().restart();
                        } catch (Exception ex) {
                            LOGGER.error("Error while restarting Hazelcast instance.", ex);
                        }
                    }
                }
            }
        } finally {
            //Release the semaphore so that an instance can be created.
            // This is necessary becasue the execution order is random.
            if (semaphore.availablePermits() == 0) {
                semaphore.release(1);
            }
        }
    }


    public void updateMemberList() {
        HazelcastInstance instance = lookupInstance();
        if (instance != null) {
            try {
                instance.getConfig().setGroupConfig(buildGroupConfig());
                instance.getConfig().getNetworkConfig().getJoin().setMulticastConfig(buildMulticastConfig());
                instance.getConfig().getNetworkConfig().getJoin().setTcpIpConfig(buildTcpIpConfig());
                instance.getLifecycleService().restart();
            } catch (Exception ex) {
                LOGGER.error("Error while restarting Hazelcast instance.", ex);
            }
        }
    }


    /**
     * Returs a Hazelcast instance from service registry.
     *
     * @return
     */
    public HazelcastInstance lookupInstance() {
        HazelcastInstance instance = null;
        try {
            if (bundleContext != null) {
                ServiceReference reference = bundleContext.getServiceReference("com.hazelcast.core.HazelcastInstance");
                instance = (HazelcastInstance) bundleContext.getService(reference);
                bundleContext.ungetService(reference);
            }
        } catch (Exception ex) {
            LOGGER.warn("No Hazelcast instance found in service registry.");
        }
        return instance;
    }


    /**
     * Builds a {@link HazelcastInstance}
     *
     * @return
     */
    public HazelcastInstance buildInstance() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            LOGGER.error("Failed to acquire instance semaphore", e);
        }
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(buildConfig());
        return instance;
    }


    /**
     * Builds a Hazelcast {@link Config}
     *
     * @return
     */
    public Config buildConfig() {
        Config config = new XmlConfigBuilder().build();
        config.setGroupConfig(buildGroupConfig());
        config.getNetworkConfig().getJoin().setMulticastConfig(buildMulticastConfig());
        config.getNetworkConfig().getJoin().setTcpIpConfig(buildTcpIpConfig());
        return config;
    }

    /**
     * Builds a {@link GroupConfig}
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
     * Builds a {@link MulticastConfig}
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
     * Builds a {@link TcpIpConfig}.
     *
     * @return
     */
    public TcpIpConfig buildTcpIpConfig() {
        TcpIpConfig tcpIpConfig = new TcpIpConfig();
        tcpIpConfig.setEnabled(tcpIpEnabled);
        tcpIpConfig.setMembers(new ArrayList(tcpIpMemberSet));
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
                    result.add(item);
                }
            }
        }
        return result;
    }

    /**
     * Returns true if both {@link Collection}s contain exactly the same items (order doesn't matter).
     *
     * @param col1
     * @param col2
     * @return
     */
    private boolean collectionEquals(Collection col1, Collection col2) {
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


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isMulticastEnabled() {
        return multicastEnabled;
    }

    public void setMulticastEnabled(boolean multicastEnabled) {
        this.multicastEnabled = multicastEnabled;
    }

    public String getMulticastGroup() {
        return multicastGroup;
    }

    public void setMulticastGroup(String multicastGroup) {
        this.multicastGroup = multicastGroup;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    public void setMulticastPort(int multicastPort) {
        this.multicastPort = multicastPort;
    }

    public boolean isTcpIpEnabled() {
        return tcpIpEnabled;
    }

    public void setTcpIpEnabled(boolean tcpIpEnabled) {
        this.tcpIpEnabled = tcpIpEnabled;
    }

    public String getTcpIpMembers() {
        return tcpIpMembers;
    }

    public void setTcpIpMembers(String tcpIpMembers) {
        this.tcpIpMembers = tcpIpMembers;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public DiscoveryTask getDiscoveryTask() {
        return discoveryTask;
    }

    public void setDiscoveryTask(DiscoveryTask discoveryTask) {
        this.discoveryTask = discoveryTask;
    }
}
