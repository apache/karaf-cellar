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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.ClusterBundleEvent;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.management.CellarBundleMBean;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the Cellar Bundle MBean.
 */
public class CellarBundleMBeanImpl extends StandardMBean implements CellarBundleMBean {

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private ConfigurationAdmin configurationAdmin;
    private EventProducer eventProducer;

    public CellarBundleMBeanImpl() throws NotCompliantMBeanException {
        super(CellarBundleMBean.class);
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @Override
    public void install(String groupName, String location) throws Exception {
        // check if cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the bundle location is allowed
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
        }

        // get the name and version in the location MANIFEST
        JarInputStream jarInputStream = new JarInputStream(new URL(location).openStream());
        Manifest manifest = jarInputStream.getManifest();
        String name = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        String version = manifest.getMainAttributes().getValue("Bundle-Version");
        jarInputStream.close();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // update the cluster group
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            BundleState state = new BundleState();
            state.setLocation(location);
            state.setStatus(BundleEvent.INSTALLED);
            clusterBundles.put(name + "/" + version, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        ClusterBundleEvent event = new ClusterBundleEvent(name, version, location, BundleEvent.INSTALLED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    @Override
    public void uninstall(String groupName, String symbolicName, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        String key = null;
        String location = null;
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            key = selector(symbolicName, version, clusterBundles);

            if (key == null) {
                throw new IllegalArgumentException("Bundle " + key + " is not found in cluster group " + groupName);
            }

            BundleState state = clusterBundles.get(key);
            if (state == null) {
                throw new IllegalArgumentException("Bundle " + key + " is not found in cluster group " + groupName);
            }
            location = state.getLocation();

            // check if the bundle location is allowed outbound
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
            }

            clusterBundles.remove(key);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        String[] split = key.split("/");
        ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.UNINSTALLED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    @Override
    public void start(String groupName, String symbolicName, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        String key = null;
        String location = null;
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            key = selector(symbolicName, version, clusterBundles);

            if (key == null) {
                throw new IllegalStateException("Bundle " + key + " is not found in cluster group " + groupName);
            }

            BundleState state = clusterBundles.get(key);
            if (state == null) {
                throw new IllegalStateException("Bundle " + key + " is not found in cluster group " + groupName);
            }
            location = state.getLocation();

            // check if the bundle location is allowed
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
            }

            state.setStatus(BundleEvent.STARTED);
            clusterBundles.put(key, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        String[] split = key.split("/");
        ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.STARTED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    @Override
    public void stop(String groupName, String symbolicName, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        String key = null;
        String location = null;
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            key = selector(symbolicName, version, clusterBundles);

            if (key == null) {
                throw new IllegalStateException("Bundle " + key + " is not found in cluster group " + groupName);
            }

            BundleState state = clusterBundles.get(key);
            if (state == null) {
                throw new IllegalStateException("Bundle " + key + " is not found in cluster group " + groupName);
            }
            location = state.getLocation();

            // check if the bundle location is allowed
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
            }

            state.setStatus(BundleEvent.STOPPED);
            clusterBundles.put(key, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        String[] split = key.split("/");
        ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.STOPPED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    @Override
    public TabularData getBundles(String groupName) throws Exception {
        CompositeType compositeType = new CompositeType("Bundle", "Karaf Cellar bundle",
                new String[]{"id", "name", "version", "status", "location"},
                new String[]{"ID of the bundle", "Name of the bundle", "Version of the bundle", "Current status of the bundle", "Location of the bundle"},
                new OpenType[]{SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Bundles", "Table of all Karaf Cellar bundles", compositeType,
                new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tableType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            int id = 0;
            for (String bundle : clusterBundles.keySet()) {
                String[] tokens = bundle.split("/");
                String name = null;
                String version = null;
                if (tokens.length == 2) {
                    name = tokens[0];
                    version = tokens[1];
                } else {
                    name = bundle;
                }
                BundleState state = clusterBundles.get(bundle);
                String status;
                switch (state.getStatus()) {
                    case BundleEvent.INSTALLED:
                        status = "Installed";
                        break;
                    case BundleEvent.RESOLVED:
                        status = "Resolved";
                        break;
                    case BundleEvent.STARTED:
                        status = "Active";
                        break;
                    case BundleEvent.STARTING:
                        status = "Starting";
                        break;
                    case BundleEvent.STOPPED:
                        status = "Resolved";
                        break;
                    case BundleEvent.STOPPING:
                        status = "Stopping";
                        break;
                    case BundleEvent.UNINSTALLED:
                        status = "Uninstalled";
                        break;
                    default:
                        status = "";
                        break;
                }
                CompositeData data = new CompositeDataSupport(compositeType,
                        new String[]{"id", "name", "version", "status", "location"},
                        new Object[]{id, name, version, status, state.getLocation()});
                table.put(data);
                id++;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return table;
    }

    /**
     * Bundle selector.
     *
     * @param name the bundle name, regex, or ID.
     * @param version the bundle version.
     * @param clusterBundles the bundles in a cluster group.
     * @return the bundle key is the distributed bundle map.
     */
    private String selector(String name, String version, Map<String, BundleState> clusterBundles) {
        String key = null;
        if (version == null || version.trim().isEmpty()) {
            // looking for bundle using ID
            int id = -1;
            try {
                id = Integer.parseInt(name);
                int index = 0;
                for (String clusterBundle : clusterBundles.keySet()) {
                    if (index == id) {
                        key = clusterBundle;
                        break;
                    }
                    index++;
                }
            } catch (NumberFormatException nfe) {
                // ignore
            }
            if (id == -1) {

                // add regex support
                Pattern namePattern = Pattern.compile(name);

                // looking for bundle using only the name
                for (String bundle : clusterBundles.keySet()) {
                    BundleState state = clusterBundles.get(bundle);
                    if (state.getName() != null) {
                        // bundle name is populated, check if it matches the regex
                        Matcher matcher = namePattern.matcher(state.getName());
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        } else {
                            // no match on bundle name, fall back to symbolic name and check if it matches the regex
                            String[] split = bundle.split("/");
                            matcher = namePattern.matcher(split[0]);
                            if (matcher.find()) {
                                key = bundle;
                                break;
                            }
                        }
                    } else {
                        // no bundle name, fall back to symbolic name and check if it matches the regex
                        String[] split = bundle.split("/");
                        Matcher matcher = namePattern.matcher(split[0]);
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        }
                    }
                }
            }
        } else {
            // looking for the bundle using name and version

            // add regex support of the name
            Pattern namePattern = Pattern.compile(name);

            for (String bundle : clusterBundles.keySet()) {
                String[] split = bundle.split("/");
                BundleState state = clusterBundles.get(bundle);
                if (split[1].equals(version)) {
                    if (state.getName() != null) {
                        // bundle name is populated, check if it matches the regex
                        Matcher matcher = namePattern.matcher(state.getName());
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        } else {
                            // no match on bundle name, fall back to symbolic name and check if it matches the regex
                            matcher = namePattern.matcher(split[0]);
                            if (matcher.find()) {
                                key = bundle;
                                break;
                            }
                        }
                    } else {
                        // no bundle name, fall back to symbolic name and check if it matches the regex
                        Matcher matcher = namePattern.matcher(split[0]);
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        }
                    }
                }
            }
        }
        return key;
    }

}
