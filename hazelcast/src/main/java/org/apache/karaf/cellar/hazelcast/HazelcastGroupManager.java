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
package org.apache.karaf.cellar.hazelcast;

import java.io.IOException;
import java.util.*;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.slf4j.Logger;

/**
 * Hazelcast group manager.
 * The role of this class is to provide means of creating groups, setting nodes to groups etc.
 * Keep in sync the distributed group configuration with the locally persisted.
 */
public class HazelcastGroupManager implements GroupManager, EntryListener, ConfigurationListener {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastGroupManager.class);

    private static final String GROUPS = "org.apache.karaf.cellar.groups";
    private static final String GROUPS_CONFIG = "org.apache.karaf.cellar.groups.config";
    private static final String DEFAULT_GROUP = "default";

    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();

    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> groupConsumer = new HashMap<String, EventConsumer>();

    private BundleContext bundleContext;

    private HazelcastInstance instance;
    private ConfigurationAdmin configurationAdmin;

    private EventTransportFactory eventTransportFactory;
    private CombinedClassLoader combinedClassLoader;

    public void init() {
        // create a listener for group configuration.
        IMap groupConfiguration = instance.getMap(GROUPS_CONFIG);
        groupConfiguration.addEntryListener(this, true);
        try {
            // create group stored in configuration admin
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP);
            if (configuration != null) {
                Dictionary<String, String> properties = configuration.getProperties();
                if (properties == null) {
                    properties = new Hashtable<String, String>();
                }
                String groups = properties.get(Configurations.GROUPS_KEY);
                Set<String> groupNames = convertStringToSet(groups);
                if (groupNames != null && !groupNames.isEmpty()) {
                    for (String groupName : groupNames) {
                        createGroup(groupName);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("CELLAR HAZELCAST: can't create group from configuration admin", e);
        }
        try {
            // add group membership from configuration
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Dictionary<String, String> properties = configuration.getProperties();
                if (properties == null) {
                    properties = new Hashtable<String, String>();
                }
                String groups = properties.get(Configurations.GROUPS_KEY);
                Set<String> groupNames = convertStringToSet(groups);
                if (groupNames != null && !groupNames.isEmpty()) {
                    for (String groupName : groupNames) {
                        registerGroup(groupName);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR HAZELCAST: can't set group membership for the current node", e);
        }
    }

    public void destroy() {
        // update the group
        Node local = this.getNode();
        Set<Group> groups = this.listGroups(local);
        for (Group group : groups) {
            String groupName = group.getName();
            group.getNodes().remove(local);
            listGroups().put(groupName, group);
        }
        // shutdown the group consumer/producers
        for (Map.Entry<String, EventConsumer> consumerEntry : groupConsumer.entrySet()) {
            EventConsumer consumer = consumerEntry.getValue();
            consumer.stop();
        }
        groupConsumer.clear();
        groupProducers.clear();
    }

    @Override
    public Node getNode() {
        Node node = null;
        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Member member = cluster.getLocalMember();
            node = new HazelcastNode(member.getInetSocketAddress().getHostName(), member.getInetSocketAddress().getPort());
        }
        return node;
    }

    public Group createGroup(String groupName) {
        Group group = listGroups().get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        if (!listGroups().containsKey(groupName)) {
            copyGroupConfiguration(Configurations.DEFAULT_GROUP_NAME, groupName);
            listGroups().put(groupName, group);
            try {
                // store the group list to configuration admin
                persist(listGroups());
            } catch (Exception e) {
                LOGGER.warn("CELLAR HAZELCAST: can't store group list", e);
            }
        }
        return group;
    }

    public void deleteGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            if (!groupName.equals(Configurations.DEFAULT_GROUP_NAME)) {
                listGroups().remove(groupName);
                try {
                    // store the group list to configuration admin
                    persist(listGroups());
                } catch (Exception e) {
                    LOGGER.warn("CELLAR HAZELCAST: can't store group list", e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Store the group names in configuration admin.
     *
     * @param groups the list of group to store.
     * @throws Exception in case of storage failure.
     */
    private void persist(Map<String, Group> groups) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP);
        if (configuration != null) {
            Dictionary<String, String> properties = configuration.getProperties();
            if (properties == null) {
                properties = new Hashtable<String, String>();
            }
            properties.put(Configurations.GROUPS_KEY, convertSetToString(groups.keySet()));
            configuration.update(properties);
        }
    }

    public Set<Group> listLocalGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroups(getNode());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public boolean isLocalGroup(String groupName) {
        Set<Group> localGroups = this.listLocalGroups();
        for (Group localGroup : localGroups) {
            if (localGroup.getName().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    public Set<Group> listAllGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return new HashSet<Group>(listGroups().values());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Group findGroupByName(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroups().get(groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Map<String, Group> listGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return instance.getMap(GROUPS);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<Group> listGroups(Node node) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Set<Group> result = new HashSet<Group>();

            Map<String, Group> groupMap = instance.getMap(GROUPS);
            Collection<Group> groupCollection = groupMap.values();
            if (groupCollection != null && !groupCollection.isEmpty()) {
                for (Group group : groupCollection) {
                    if (group.getNodes().contains(node)) {
                        result.add(group);
                    }
                }
            }
            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<String> listGroupNames() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroupNames(getNode());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public Set<String> listGroupNames(Node node) {
        Set<String> names = new HashSet<String>();
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Map<String, Group> groups = listGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups.values()) {
                    if (group.getNodes().contains(node)) {
                        names.add(group.getName());
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return names;
    }

    public void registerGroup(Group group) {
        String groupName = group.getName();
        createGroup(groupName);

        LOGGER.info("Registering group {}.", groupName);
        Properties serviceProperties = new Properties();
        serviceProperties.put("type", "group");
        serviceProperties.put("name", groupName);

        if (!producerRegistrations.containsKey(groupName)) {
            EventProducer producer = groupProducers.get(groupName);
            if (producer == null) {
                producer = eventTransportFactory.getEventProducer(groupName, Boolean.TRUE);
                groupProducers.put(groupName, producer);
            }

            ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), producer, serviceProperties);
            producerRegistrations.put(groupName, producerRegistration);
        }

        if (!consumerRegistrations.containsKey(groupName)) {
            EventConsumer consumer = groupConsumer.get(groupName);
            if (consumer == null) {
                consumer = eventTransportFactory.getEventConsumer(groupName, true);
                groupConsumer.put(groupName, consumer);
            } else if (!consumer.isConsuming()) {
                consumer.start();
            }
            ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), consumer, serviceProperties);
            consumerRegistrations.put(groupName, consumerRegistration);
        }

        group.getNodes().add(getNode());
        listGroups().put(groupName, group);

        //Add group to configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            Dictionary<String, String> properties = configuration.getProperties();
            String groups = properties.get(Configurations.GROUPS_KEY);
            if (groups == null || (groups.trim().length() < 1)) {
                groups = groupName;
            } else {

                Set<String> groupNamesSet = convertStringToSet(groups);
                groupNamesSet.add(groupName);
                groups = convertSetToString(groupNamesSet);
            }

            if (groups == null || (groups.trim().length() < 1)) {
                groups = groupName;
            }
            properties.put(Configurations.GROUPS_KEY, groups);
            configuration.update(properties);
        } catch (IOException e) {
            LOGGER.error("Error reading group configuration {}", group);
        }

        //Sync
        try {
            ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences("org.apache.karaf.cellar.core.Synchronizer", null);
            if (serviceReferences != null && serviceReferences.length > 0) {
                for (ServiceReference ref : serviceReferences) {
                    Synchronizer synchronizer = (Synchronizer) bundleContext.getService(ref);
                    if (synchronizer.isSyncEnabled(group)) {
                        synchronizer.pull(group);
                        synchronizer.push(group);
                    }
                    bundleContext.ungetService(ref);
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Error looking up for synchronizers.", e);
        }
    }

    public void registerGroup(String groupName) {
        Group group = listGroups().get(groupName);
        if (group == null) {
            group = new Group(groupName);
        }
        registerGroup(group);
    }

    public void unRegisterGroup(String groupName) {
        unRegisterGroup(listGroups().get(groupName));
    }

    public void unRegisterGroup(Group group) {
        String groupName = group.getName();
        //1. Remove local node from group.
        group.getNodes().remove(getNode());
        listGroups().put(groupName, group);

        //2. Unregister group consumers
        if (consumerRegistrations != null && !consumerRegistrations.isEmpty()) {
            ServiceRegistration consumerRegistration = consumerRegistrations.get(groupName);
            if (consumerRegistration != null) {
                consumerRegistration.unregister();
                consumerRegistrations.remove(groupName);
            }

        }

        //3. Unregister group producers
        if (producerRegistrations != null && !producerRegistrations.isEmpty()) {
            ServiceRegistration producerRegistration = producerRegistrations.get(groupName);
            if (producerRegistration != null) {
                producerRegistration.unregister();
                producerRegistrations.remove(groupName);
            }
        }

        //4. Remove Consumers & Producers
        groupProducers.remove(groupName);
        EventConsumer consumer = groupConsumer.remove(groupName);
        if (consumer != null) {
            consumer.stop();
        }

        //Remove group from configuration
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            Dictionary<String, String> properties = configuration.getProperties();
            String groups = properties.get(Configurations.GROUPS_KEY);
            if (groups == null || (groups.trim().length() < 1)) {
                groups = "";
            } else if (groups.contains(groupName)) {

                Set<String> groupNamesSet = convertStringToSet(groups);
                groupNamesSet.remove(groupName);
                groups = convertSetToString(groupNamesSet);

            }
            properties.put(Configurations.GROUPS_KEY, groups);
            configuration.update(properties);
        } catch (IOException e) {
            LOGGER.error("Error reading group configuration {}", group);
        }
    }

    /**
     * Copies the configuration of a {@link Group}.
     * <b>1.</b> Updates configuration admin from Hazelcast using source config.
     * <b>2.</b> Creates target configuration both on Hazelcast and configuration admin.
     *
     * @param sourceGroupName
     * @param targetGroupName
     */
    public void copyGroupConfiguration(String sourceGroupName, String targetGroupName) {
        try {
            Configuration conf = configurationAdmin.getConfiguration(Configurations.GROUP);
            if (conf != null) {

                //Get configuration from config admin.
                Dictionary configAdminProperties = conf.getProperties();
                if (configAdminProperties == null) {
                    configAdminProperties = new Properties();
                }
                //Get configuration from Hazelcast
                Map<String, String> sourceGropConfig = instance.getMap(GROUPS_CONFIG);

                //Update local configuration from cluster.
                for (Map.Entry<String, String> parentEntry : sourceGropConfig.entrySet()) {
                    configAdminProperties.put(parentEntry.getKey(), parentEntry.getValue());
                }

                Dictionary updatedProperties = new Properties();
                Enumeration keyEnumeration = configAdminProperties.keys();
                while (keyEnumeration.hasMoreElements()) {
                    String key = (String) keyEnumeration.nextElement();
                    String value = (String) configAdminProperties.get(key);

                    if (key.startsWith(sourceGroupName)) {
                        String newKey = key.replace(sourceGroupName, targetGroupName);
                        updatedProperties.put(newKey, value);
                        sourceGropConfig.put(key, value);
                    }
                    updatedProperties.put(key, value);
                }

                conf.update(updatedProperties);
            }

        } catch (IOException e) {
            LOGGER.error("Error reading group configuration.", e);
        }
    }

    /**
     * Utility method which converts a set to a String.
     *
     * @param set
     * @return
     */
    protected String convertSetToString(Set<String> set) {
        StringBuffer result = new StringBuffer();
        Iterator<String> groupIterator = set.iterator();
        while (groupIterator.hasNext()) {
            String name = groupIterator.next();
            result.append(name);
            if (groupIterator.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    /**
     * Utility method which converts String to Set.
     *
     * @param string
     * @return
     */
    protected Set<String> convertStringToSet(String string) {
        Set<String> result = new HashSet<String>();
        String[] groupNames = string.split(",");

        if (groupNames != null && groupNames.length > 0) {
            for (String name : groupNames) {
                result.add(name);
            }
        } else {
            result.add(string);
        }
        return result;
    }


    public void configurationEvent(ConfigurationEvent configurationEvent) {
        String pid = configurationEvent.getPid();
        if (pid.equals(GROUPS)) {
            Map groupConfiguration = instance.getMap(GROUPS_CONFIG);

            try {
                Configuration conf = configurationAdmin.getConfiguration(GROUPS);
                Dictionary properties = conf.getProperties();
                Enumeration keyEnumeration = properties.keys();
                while (keyEnumeration.hasMoreElements()) {
                    Object key = keyEnumeration.nextElement();
                    Object value = properties.get(key);
                    if (!groupConfiguration.containsKey(key) || groupConfiguration.get(key) == null || !groupConfiguration.get(key).equals(value)) {
                        groupConfiguration.put(key, value);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to update group configuration");
            }
        }
    }

    /**
     * Invoked when an entry is added.
     *
     * @param entryEvent entry event
     */
    public void entryAdded(EntryEvent entryEvent) {
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is removed.
     *
     * @param entryEvent entry event
     */
    public void entryRemoved(EntryEvent entryEvent) {
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is updated.
     *
     * @param entryEvent entry event
     */
    public void entryUpdated(EntryEvent entryEvent) {
        try {
            Configuration conf = configurationAdmin.getConfiguration(GROUPS);
            Dictionary props = conf.getProperties();
            Object key = entryEvent.getKey();
            Object value = entryEvent.getValue();
            if (props.get(key) == null || !props.get(key).equals(value)) {
                props.put(key, value);
                conf.update(props);
            }
        } catch (Exception ex) {
            LOGGER.warn("Error while updating local group configuration", ex);
        }
    }

    /**
     * Invoked when an entry is evicted.
     *
     * @param entryEvent entry event
     */
    public void entryEvicted(EntryEvent entryEvent) {
        entryUpdated(entryEvent);
    }


    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

    public CombinedClassLoader getCombinedClassLoader() {
        return combinedClassLoader;
    }

    public void setCombinedClassLoader(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }

}
