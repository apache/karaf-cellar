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

import com.hazelcast.core.*;
import org.apache.karaf.cellar.core.*;
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
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * A group manager implementation powered by Hazelcast.
 * The role of this class is to provide means of creating groups, setting nodes to groups etc.
 * Keep in sync the distributed group configuration with the locally persisted.
 */
public class HazelcastGroupManager implements GroupManager, EntryListener<String,Object>, SynchronousConfigurationListener {

    private static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HazelcastGroupManager.class);

    private static final String HAZELCAST_GROUPS = "org.apache.karaf.cellar.groups";
    private static final String HAZELCAST_GROUPS_CONFIG = "org.apache.karaf.cellar.groups.config";

    private Map<String, ServiceRegistration> producerRegistrations = new HashMap<String, ServiceRegistration>();
    private Map<String, ServiceRegistration> consumerRegistrations = new HashMap<String, ServiceRegistration>();

    private Map<String, Object> localConfig = new HashMap<String, Object>();

    private Map<String, EventProducer> groupProducers = new HashMap<String, EventProducer>();
    private Map<String, EventConsumer> groupConsumer = new HashMap<String, EventConsumer>();

    private BundleContext bundleContext;

    private HazelcastInstance instance;
    private ConfigurationAdmin configurationAdmin;

    private EventTransportFactory eventTransportFactory;
    private CombinedClassLoader combinedClassLoader;

    public void init() {
        try {
            // create group stored in configuration admin
            Configuration groupsConfiguration = getConfigurationForGroups();
            Dictionary<String, Object> properties = groupsConfiguration.getProperties();

            // create a listener for group configuration.
            IMap<String,Object> hazelcastGroupsConfig = getClusterGroupsConfig();

            hazelcastGroupsConfig.addEntryListener(this, true);

            if (hazelcastGroupsConfig.isEmpty()) {
                // First one to be here - initialize hazelcast map with local configuration
                LOGGER.debug("CELLAR HAZELCAST: initialize cluster with local config");

                Map<String, Object> updates = getUpdatesForHazelcastMap(properties);
                hazelcastGroupsConfig.putAll(updates);
            } else {
                if (properties != null) {
                    Enumeration<String> en = properties.keys();
                    while (en.hasMoreElements()) {
                        String key = en.nextElement();
                        localConfig.put(key , properties.get(key));
                    }
                } else {
                    properties = new Hashtable<String, Object>();
                }
                boolean updated = false;
                for (String key : hazelcastGroupsConfig.keySet()) {
                    Object value = hazelcastGroupsConfig.get(key);
                    updated |= updatePropertiesFromHazelcastMap(properties, key, value);
                }
                if (updated) {
                    updateConfiguration(groupsConfiguration, properties);
                }
            }

            Node node = getNode();

            // add group membership from configuration
            properties = getConfigurationForNode().getProperties();
            Set<String> groupNames = convertStringToSet(properties != null ? (String) properties.get(Configurations.GROUPS_KEY) : null);
            getClusterGroups().put(node, groupNames);
        } catch (IOException e) {
            LOGGER.warn("CELLAR HAZELCAST: can't create cluster group from configuration admin", e);
        }
    }

    private synchronized boolean updatePropertiesFromHazelcastMap(Dictionary<String, Object> properties, String key, Object value) {
        if (!(value instanceof Map)) {
            return false;
        }
        boolean changed = false;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String entryKey = entry.getKey();
            Object entryValue = entry.getValue();
            if (entryKey.equals(".change")) {
                Set<String> groups = convertStringToSet((String) properties.get(Configurations.GROUPS_KEY));
                if ((entryValue.equals("added") && !groups.contains(key))
                        || entryValue.equals("removed") && groups.contains(key)) {
                    LOGGER.debug("CELLAR HAZELCAST: get group " + key + " configuration from cluster : " + key
                            + " has been " + entryValue);
                    if (entryValue.equals("added")) {
                        groups.add(key);
                    } else {
                        groups.remove(key);
                    }
                    String newValue = convertSetToString(groups);
                    properties.put(Configurations.GROUPS_KEY, newValue);
                    localConfig.put(Configurations.GROUPS_KEY, newValue);
                    changed = true;
                }
            } else if (properties.get(entryKey) == null || !properties.get(entryKey).equals(entryValue)) {
                LOGGER.debug("CELLAR HAZELCAST: get group " + key + " configuration from cluster : " + entryKey + " = "
                        + entryValue);
                properties.put(entryKey, entryValue);
                localConfig.put(entryKey, entryValue);
                changed = true;
            }
        }
        return changed;
    }

    private synchronized Map<String, Object> getUpdatesForHazelcastMap(Dictionary<String, Object> properties) {
        Map<String,Object> updates = new HashMap<String,Object>();
        if (properties == null) {
            return updates;
        }
        Enumeration<String> en = properties.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            Object value = properties.get(key);

            if (!key.startsWith("felix.") && !key.startsWith("service.")) {
                Object localValue = localConfig.get(key);
                if (localValue == null || !localValue.equals(value)) {
                    if (key.equals(Configurations.GROUPS_KEY)) {
                        Set<String> removedGroups = convertStringToSet((String) localValue);
                        Set<String> addedGroups = convertStringToSet((String) value);
                        addedGroups.removeAll(removedGroups);
                        removedGroups.removeAll(convertStringToSet((String) value));
                        for (String addedGroup : addedGroups) {
                            getOrCreateMap(updates, addedGroup).put(".change", "added");
                        }
                        for (String removedGroup : removedGroups) {
                            getOrCreateMap(updates, removedGroup).put(".change", "removed");
                        }
                    } else {
                        int idx = key.indexOf(".");
                        if (idx > 0) {
                            String groupKey = key.substring(0, idx);
                            getOrCreateMap(updates, groupKey).put(key, value);
                        } else {
                            LOGGER.warn("CELLAR HAZELCAST: found group property that is not prefixed with a group name: {}. Skipping it", key); 
                        }
                    }
                }
            }

            localConfig.put(key , value);

        }
        return updates;
    }

    private Map<String, Object> getOrCreateMap(Map<String, Object> updates, String group) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) updates.get(group);
        if (props == null) {
            props = new HashMap<String, Object>();
            updates.put(group, props);
        }
        return props;
    }

    public void destroy() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            // update the group
            Node local = this.getNode();

            getClusterGroups().remove(local);

            // shutdown the group consumer/producers
            for (Map.Entry<String, EventConsumer> consumerEntry : groupConsumer.entrySet()) {
                EventConsumer consumer = consumerEntry.getValue();
                consumer.stop();
            }
            groupConsumer.clear();
            groupProducers.clear();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Node getNode() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Node node = null;
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Member member = cluster.getLocalMember();
                node = new HazelcastNode(member);
            }
            return node;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Group createGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Map<String, Group> listGroups = listGroups();
            Group group = listGroups.get(groupName);
            if (group == null) {
                group = new Group(groupName);
                try {
                    Configuration configuration = getConfigurationForGroups();
                    if (configuration != null) {
                        Dictionary<String, Object> properties = configuration.getProperties();
                        if (properties != null && !properties.isEmpty()) {
                            properties = copyGroupConfiguration(Configurations.DEFAULT_GROUP_NAME + '.', groupName + '.', properties);
                            Set<String> groups = convertStringToSet((String) properties.get(Configurations.GROUPS_KEY));
                            groups.add(groupName);
                            properties.put(Configurations.GROUPS_KEY, convertSetToString(groups));
                            updateConfiguration(configuration, properties);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("CELLAR HAZELCAST: failed to update cluster group configuration", e);
                }
            }
            return group;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void deleteGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            if (!groupName.equals(Configurations.DEFAULT_GROUP_NAME)) {
                try {
                    // store the group list to configuration admin
                    Configuration configuration = getConfigurationForGroups();
                    Dictionary<String, Object> properties = configuration.getProperties();
                    Set<String> groups = convertStringToSet((String) properties.get(Configurations.GROUPS_KEY));
                    groups.remove(groupName);
                    properties.put(Configurations.GROUPS_KEY, convertSetToString(groups));
                    updateConfiguration(configuration, properties);
                } catch (IOException e) {
                    LOGGER.warn("CELLAR HAZELCAST: can't store cluster group list", e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Set<Group> listLocalGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroups(getNode());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public boolean isLocalGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Set<Group> localGroups = this.listLocalGroups();
            for (Group localGroup : localGroups) {
                if (localGroup.getName().equals(groupName)) {
                    return true;
                }
            }
            return false;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Set<Group> listAllGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return new HashSet<Group>(listGroups().values());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Group findGroupByName(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroups().get(groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Map<String, Group> listGroups() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Map<String, Group> res = new HashMap<>();
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Map<Node, Set<String>> nodes = getClusterGroups();

            Set<String> groups = convertStringToSet((String) localConfig.get(Configurations.GROUPS_KEY));

            groups.add(Configurations.DEFAULT_GROUP_NAME);

            for (String groupName : groups) {
                Group group = new Group(groupName);
                res.put(groupName, group);
                for (Map.Entry<Node, Set<String>> entry : nodes.entrySet()) {
                    if (entry.getValue().contains(groupName)) {
                        group.getNodes().add(entry.getKey());
                    }
                }
            }

            return res;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Set<Group> listGroups(Node node) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            Set<Group> result = new HashSet<Group>();

            Map<Node, Set<String>> groupMap = getClusterGroups();
            Set<String> groupNames = groupMap.get(node);

            if (groupNames != null) {
                Map<String, Group> g = listGroups();
                g.keySet().retainAll(groupNames);
                result.addAll(g.values());
            }

            return result;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public Set<String> listGroupNames() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            return listGroupNames(getNode());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
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

    /**
     * Register a cluster {@link Group}.
     *
     * @param group the cluster group to register.
     */
    @Override
    public void registerGroup(Group group) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            String groupName = group.getName();

            LOGGER.debug("CELLAR HAZELCAST: registering cluster group {}.", groupName);
            Properties serviceProperties = new Properties();
            serviceProperties.put("type", "group");
            serviceProperties.put("name", groupName);

            if (!producerRegistrations.containsKey(groupName)) {
                EventProducer producer = groupProducers.get(groupName);
                if (producer == null) {
                    producer = eventTransportFactory.getEventProducer(groupName, Boolean.TRUE);
                    groupProducers.put(groupName, producer);
                }

                ServiceRegistration producerRegistration = bundleContext.registerService(EventProducer.class.getCanonicalName(), producer, (Dictionary) serviceProperties);
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
                ServiceRegistration consumerRegistration = bundleContext.registerService(EventConsumer.class.getCanonicalName(), consumer, (Dictionary) serviceProperties);
                consumerRegistrations.put(groupName, consumerRegistration);
            }

            Node node = getNode();
            group.getNodes().add(node);
            Map<Node, Set<String>> map = getClusterGroups();
            Set<String> groupNames = (Set<String>) map.get(node);
            groupNames = new HashSet<String>(groupNames);
            groupNames.add(groupName);
            map.put(node, groupNames);

            // add group to configuration
            try {
                Configuration configuration = getConfigurationForNode();
                if (configuration != null) {
                    Dictionary<String, Object> properties = configuration.getProperties();
                    if (properties != null) {
                        String groups = (String) properties.get(Configurations.GROUPS_KEY);
                        if (groups == null || groups.isEmpty()) {
                            groups = groupName;
                        } else {
                            Set<String> groupNamesSet = convertStringToSet(groups);
                            groupNamesSet.add(groupName);
                            groups = convertSetToString(groupNamesSet);
                        }

                        if (groups == null || groups.isEmpty()) {
                            groups = groupName;
                        }
                        properties.put(Configurations.GROUPS_KEY, groups);
                        updateConfiguration(configuration, properties);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("CELLAR HAZELCAST: error reading cluster group configuration {}", group);
            }

            // launch the synchronization on the group
            try {
                ServiceReference[] serviceReferences = bundleContext.getAllServiceReferences("org.apache.karaf.cellar.core.Synchronizer", null);
                if (serviceReferences != null && serviceReferences.length > 0) {
                    for (ServiceReference ref : serviceReferences) {
                        Synchronizer synchronizer = (Synchronizer) bundleContext.getService(ref);
                        if (synchronizer != null) {
                            synchronizer.sync(group);
                        }
                        bundleContext.ungetService(ref);
                    }
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.error("CELLAR HAZELCAST: failed to look for synchronizers", e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void registerGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            registerGroup(new Group(groupName));
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void unRegisterGroup(String groupName) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            unRegisterGroup(listGroups().get(groupName));
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void unRegisterGroup(Group group) {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
            String groupName = group.getName();
            // remove local node from cluster group
            group.getNodes().remove(getNode());
            listGroups().put(groupName, group);

            // un-register cluster group consumers
            if (consumerRegistrations != null && !consumerRegistrations.isEmpty()) {
                ServiceRegistration consumerRegistration = consumerRegistrations.get(groupName);
                if (consumerRegistration != null) {
                    consumerRegistration.unregister();
                    consumerRegistrations.remove(groupName);
                }
            }

            // un-register cluster group producers
            if (producerRegistrations != null && !producerRegistrations.isEmpty()) {
                ServiceRegistration producerRegistration = producerRegistrations.get(groupName);
                if (producerRegistration != null) {
                    producerRegistration.unregister();
                    producerRegistrations.remove(groupName);
                }
            }

            // remove consumers & producers
            groupProducers.remove(groupName);
            EventConsumer consumer = groupConsumer.remove(groupName);
            if (consumer != null) {
                consumer.stop();
            }

            Node node = getNode();
            group.getNodes().add(node);
            Map<Node, Set<String>> map = getClusterGroups();
            Set<String> groupNames = (Set<String>) map.get(node);
            groupNames = new HashSet<String>(groupNames);
            groupNames.remove(groupName);
            map.put(node, groupNames);

            // remove cluster group from configuration
            try {
                Configuration configuration = getConfigurationForNode();
                Dictionary<String, Object> properties = configuration.getProperties();
                String groups = (String) properties.get(Configurations.GROUPS_KEY);
                if (groups == null || groups.isEmpty()) {
                    groups = "";
                } else if (groups.contains(groupName)) {
                    Set<String> groupNamesSet = convertStringToSet(groups);
                    groupNamesSet.remove(groupName);
                    groups = convertSetToString(groupNamesSet);
                }
                properties.put(Configurations.GROUPS_KEY, groups);
                updateConfiguration(configuration, properties);
            } catch (IOException e) {
                LOGGER.error("CELLAR HAZELCAST: failed to read cluster group configuration", e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Copy the configuration of a cluster {@link Group}.
     *
     * @param sourceGroupPrefix the source cluster group prefix
     * @param targetGroupPrefix the target cluster group prefix
     * @return new properties object
     */
    public Dictionary<String, Object> copyGroupConfiguration(String sourceGroupPrefix, String targetGroupPrefix, Dictionary<String, Object> properties) {
        Dictionary<String, Object> updatedProperties = new Hashtable<String, Object>();
        Enumeration<String> keyEnumeration = properties.keys();
        while (keyEnumeration.hasMoreElements()) {
            String key = keyEnumeration.nextElement();
            Object value = properties.get(key);

            if (key.startsWith(sourceGroupPrefix)) {
                String newKey = targetGroupPrefix + key.substring(sourceGroupPrefix.length());
                updatedProperties.put(newKey, value);
            }
            updatedProperties.put(key, value);
        }
        return updatedProperties;
    }

    /**
     * Util method which converts a Set to a String.
     *
     * @param set the Set to convert.
     * @return the String corresponding to the Set.
     */
    protected String convertSetToString(Set<String> set) {
        StringBuilder result = new StringBuilder();
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
     * Util method which converts a String to a Set.
     *
     * @param string the String to convert.
     * @return the Set corresponding to the String.
     */
    protected Set<String> convertStringToSet(String string) {
        if (string == null)
            return new TreeSet<>();
        Set<String> result = new TreeSet<>();
        String[] groupNames = string.split(",");

        if (groupNames != null && groupNames.length > 0) {
            for (String name : groupNames) {
                result.add(name.trim());
            }
        } else {
            result.add(string);
        }
        return result;
    }

    /**
     * A local configuration listener to update the local Hazelcast instance when the configuration changes.
     *
     * @param configurationEvent the local configuration event.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void configurationEvent(ConfigurationEvent configurationEvent) {
        if (!Configurations.GROUP.equals(configurationEvent.getPid())) {
            return;
        }
        try {
            Map<String, Object> hazelcastGroupConfig = getClusterGroupsConfig();
            Configuration conf = getConfigurationForGroups();
            Dictionary<String, Object> properties = conf.getProperties();
            Map<String, Object> updates = getUpdatesForHazelcastMap(properties);
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object clusterValue = hazelcastGroupConfig.get(key);
                if (clusterValue == null || !clusterValue.equals(value)) {
                    LOGGER.debug("CELLAR HAZELCAST : sending updates to cluster : " + key + " = " + value);
                    if (clusterValue != null && value instanceof Map) {
                        @SuppressWarnings("rawtypes")
                        Map<String, Object> newValue = new HashMap((Map) clusterValue);
                        newValue.putAll((Map<? extends String, ?>) value);
                        hazelcastGroupConfig.put(key, newValue);
                    } else {
                        hazelcastGroupConfig.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR HAZELCAST: failed to update cluster group configuration", e);
        }
    }

    /**
     * Invoked when an entry is added.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryAdded(EntryEvent<String,Object> entryEvent) {
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is removed.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryRemoved(EntryEvent<String,Object> entryEvent) {
        entryUpdated(entryEvent);
    }

    /**
     * Invoked when an entry is updated.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryUpdated(EntryEvent<String,Object> entryEvent) {
        try {
            Configuration conf = getConfigurationForGroups();
            Dictionary<String, Object> properties = conf.getProperties();
            String key = entryEvent.getKey();
            Object value = entryEvent.getValue();

            if (updatePropertiesFromHazelcastMap(properties, key, value)) {
                LOGGER.debug("CELLAR HAZELCAST: cluster group configuration has been updated, updating local configuration: {} = {}", key, value);
                updateConfiguration(conf, properties);
            }
        } catch (Exception ex) {
            LOGGER.warn("CELLAR HAZELCAST: failed to update local configuration", ex);
        }
    }

    /**
     * Invoked when an entry is evicted.
     *
     * @param entryEvent entry event
     */
    @Override
    public void entryEvicted(EntryEvent<String,Object> entryEvent) {
        entryUpdated(entryEvent);
    }

    @Override
    public void mapCleared(MapEvent mapEvent) {
        // nothing to do
    }

    @Override
    public void mapEvicted(MapEvent mapEvent) {
        // nothing to do
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

    private Configuration getConfigurationForGroups() throws IOException {
        try {
            int max = 0;
            while (configurationAdmin.listConfigurations("(service.pid=" + Configurations.GROUP + ")") == null && max < 100) {
                Thread.sleep(500);
                max++;
            }
            if (max == 100) {
                LOGGER.warn("Timeout while loading {} configuration", Configurations.GROUP);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return configurationAdmin.getConfiguration(Configurations.GROUP, null);
    }

    private Configuration getConfigurationForNode() throws IOException {
        try {
            int max = 0;
            while (configurationAdmin.listConfigurations("(service.pid=" + Configurations.NODE + ")") == null && max < 100) {
                Thread.sleep(500);
                max++;
            }
            if (max == 100) {
                LOGGER.warn("Timeout while loading {} configuration", Configurations.NODE);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return configurationAdmin.getConfiguration(Configurations.NODE, null);
    }

    private IMap<Node, Set<String>> getClusterGroups() {
        return instance.getMap(HAZELCAST_GROUPS);
    }

    private IMap<String, Object> getClusterGroupsConfig() {
        return instance.getMap(HAZELCAST_GROUPS_CONFIG);
    }

    private void updateConfiguration(Configuration cfg, Dictionary<String, Object> properties) throws IOException {
        cfg.update(properties);
        LOGGER.debug("CELLAR HAZELCAST: updated configuration with pid: {}", cfg.getPid());
    }
}
