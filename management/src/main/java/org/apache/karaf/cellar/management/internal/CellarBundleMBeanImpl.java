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
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.bundle.RemoteBundleEvent;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.management.CellarBundleMBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.net.URL;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Implementation of the Cellar bundle MBean.
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

    public void install(String groupName, String location) throws Exception {
        // check if cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // check if the bundle location is allowed
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound");
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
            // populate the cluster map
            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            BundleState state = new BundleState();
            state.setLocation(location);
            state.setStatus(BundleEvent.INSTALLED);
            bundles.put(name + "/" + version, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the event
        RemoteBundleEvent event = new RemoteBundleEvent(name, version, location, BundleEvent.INSTALLED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void uninstall(String groupName, String symbolicName, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // update the cluster map
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            BundleState state = bundles.get(symbolicName + "/" + version);
            if (state == null) {
                throw new IllegalArgumentException("Bundle " + symbolicName + "/" + version + " is not found in cluster group " + groupName);
            }
            String location = state.getLocation();

            // check if the bundle location is allowed outbound
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound");
            }

            bundles.remove(symbolicName + "/" + version);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the event
        RemoteBundleEvent event = new RemoteBundleEvent(symbolicName, version, null, BundleEvent.UNINSTALLED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void start(String groupName, String symbolicName, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // update the cluster map
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            BundleState state = bundles.get(symbolicName + "/" + version);
            if (state == null) {
                throw new IllegalStateException("Bundle " + symbolicName + "/" + version + " not found in cluster group " + groupName);
            }
            String location = state.getLocation();

            // check if the bundle location is allowed
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound");
            }

            state.setStatus(BundleEvent.STARTED);
            bundles.put(symbolicName + "/" + version, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the event
        RemoteBundleEvent event = new RemoteBundleEvent(symbolicName, version, null, BundleEvent.STARTED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void stop(String groupName, String symbolicName, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // update the cluster map
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            BundleState state = bundles.get(symbolicName + "/" + version);
            if (state == null) {
                throw new IllegalStateException("Bundle " + symbolicName + "/" + version + " not found in cluster group " + groupName);
            }
            String location = state.getLocation();

            // check if the bundle location is allowed
            CellarSupport support = new CellarSupport();
            support.setClusterManager(this.clusterManager);
            support.setGroupManager(this.groupManager);
            support.setConfigurationAdmin(this.configurationAdmin);
            if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound");
            }

            state.setStatus(BundleEvent.STOPPED);
            bundles.put(symbolicName + "/" + version, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the event
        RemoteBundleEvent event = new RemoteBundleEvent(symbolicName, version, null, BundleEvent.STOPPED);
        event.setForce(true);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public TabularData getBundles(String groupName) throws Exception {
        CompositeType compositeType = new CompositeType("Bundle", "Karaf Cellar bundle",
                new String[]{"name", "version", "status", "location"},
                new String[]{"Name of the bundle", "Version of the bundle", "Current status of the bundle", "Location of the bundle"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Bundles", "Table of all KarafCellar bundles", compositeType,
                new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tableType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> bundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            for (String bundle : bundles.keySet()) {
                String[] tokens = bundle.split("/");
                String name = tokens[0];
                String version = tokens[1];
                BundleState state = bundles.get(bundle);
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
                        new String[]{"name", "version", "status", "location"},
                        new Object[]{name, version, status, state.getLocation()});
                table.put(data);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return table;
    }

}
