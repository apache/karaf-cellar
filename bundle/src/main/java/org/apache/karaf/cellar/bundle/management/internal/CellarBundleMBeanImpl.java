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
package org.apache.karaf.cellar.bundle.management.internal;

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.ClusterBundleEvent;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.bundle.management.CellarBundleMBean;
import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
        this.install(groupName, location, false);
    }

    @Override
    public void install(String groupName, String location, boolean start) throws Exception {
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
            throw new IllegalArgumentException("Bundle location " + location + " is blocked outbound for cluster group " + groupName);
        }

        // get the name and version in the location MANIFEST
        JarInputStream jarInputStream = new JarInputStream(new URL(location).openStream());
        Manifest manifest = jarInputStream.getManifest();
        String name = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        if (name == null) {
            name = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        }
        if (name == null) {
            name = location;
        }
        String version = manifest.getMainAttributes().getValue("Bundle-Version");
        jarInputStream.close();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // update the cluster group
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            BundleState state = new BundleState();
            state.setName(name);
            state.setLocation(location);
            if (start) {
                state.setStatus(BundleEvent.STARTED);
            } else {
                state.setStatus(BundleEvent.INSTALLED);
            }
            clusterBundles.put(name + "/" + version, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the event
        ClusterBundleEvent event = new ClusterBundleEvent(name, version, location, BundleEvent.INSTALLED);
        event.setSourceGroup(group);
        eventProducer.produce(event);
        if (start) {
            event = new ClusterBundleEvent(name, version, location, BundleEvent.STARTED);
            event.setSourceGroup(group);
            eventProducer.produce(event);
        }
    }

    @Override
    public void uninstall(String groupName, String id) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, clusterBundles);

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();

                // check if the bundle location is allowed outbound
                CellarSupport support = new CellarSupport();
                support.setClusterManager(this.clusterManager);
                support.setGroupManager(this.groupManager);
                support.setConfigurationAdmin(this.configurationAdmin);
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                clusterBundles.remove(bundle);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.UNINSTALLED);
                event.setSourceGroup(group);
                eventProducer.produce(event);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }


    }

    @Override
    public void start(String groupName, String id) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {

            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, clusterBundles);

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();

                // check if the bundle location is allowed
                CellarSupport support = new CellarSupport();
                support.setClusterManager(this.clusterManager);
                support.setGroupManager(this.groupManager);
                support.setConfigurationAdmin(this.configurationAdmin);
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                state.setStatus(BundleEvent.STARTED);
                clusterBundles.put(bundle, state);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.STARTED);
                event.setSourceGroup(group);
                eventProducer.produce(event);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void stop(String groupName, String id) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, clusterBundles);

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();

                // check if the bundle location is allowed outbound
                CellarSupport support = new CellarSupport();
                support.setClusterManager(this.clusterManager);
                support.setGroupManager(this.groupManager);
                support.setConfigurationAdmin(this.configurationAdmin);
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                state.setStatus(BundleEvent.STOPPED);
                clusterBundles.put(bundle, state);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, BundleEvent.STOPPED);
                event.setSourceGroup(group);
                eventProducer.produce(event);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
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
     * Bundle selector on the cluster.
     *
     * @return the bundle key is the distributed bundle map.
     */
    protected List<String> selector(String id, Map<String, BundleState> clusterBundles) {
        List<String> bundles = new ArrayList<String>();

        addMatchingBundles(id, bundles, clusterBundles);

        return bundles;
    }

    protected void addMatchingBundles(String id, List<String> bundles, Map<String, BundleState> clusterBundles) {

        // id is a number
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(id);
        if (matcher.find()) {
            int idInt = Integer.parseInt(id);
            int index = 0;
            for (String bundle : clusterBundles.keySet()) {
                if (index == idInt) {
                    bundles.add(bundle);
                    break;
                }
                index++;
            }
            return;
        }

        // id as a number range
        pattern = Pattern.compile("^(\\d+)-(\\d+)$");
        matcher = pattern.matcher(id);
        if (matcher.find()) {
            int index = id.indexOf('-');
            long startId = Long.parseLong(id.substring(0, index));
            long endId = Long.parseLong(id.substring(index + 1));
            if (startId < endId) {
                int bundleIndex = 0;
                for (String bundle : clusterBundles.keySet()) {
                    if (bundleIndex >= startId && bundleIndex <= endId) {
                        bundles.add(bundle);
                    }
                    bundleIndex++;
                }
            }
            return;
        }

        int index = id.indexOf('/');
        if (index != -1) {
            // id is name/version
            String[] idSplit = id.split("/");
            String name = idSplit[0];
            String version = idSplit[1];
            for (String bundle : clusterBundles.keySet()) {
                String[] bundleSplit = bundle.split("/");
                if (bundleSplit[1].equals(version)) {
                    // regex on the name
                    Pattern namePattern = Pattern.compile(name);
                    BundleState state = clusterBundles.get(bundle);
                    if (state.getName() != null) {
                        // bundle name is populated, check if it matches the regex
                        matcher = namePattern.matcher(state.getName());
                        if (matcher.find()) {
                            bundles.add(bundle);
                        } else {
                            // no match on bundle name, fall back to symbolic name and check if it matches the regex
                            matcher = namePattern.matcher(name);
                            if (matcher.find()) {
                                bundles.add(bundle);
                            }
                        }
                    } else {
                        // no bundle name, fall back to symbolic name and check if it matches the regex
                        matcher = namePattern.matcher(name);
                        if (matcher.find()) {
                            bundles.add(bundle);
                        }
                    }
                }
            }
            return;
        }

        // id is just name
        // regex support on the name
        Pattern namePattern = Pattern.compile(id);
        // looking for bundle using only the name
        for (String bundle : clusterBundles.keySet()) {
            BundleState state = clusterBundles.get(bundle);
            if (state.getName() != null) {
                // bundle name is populated, check if it matches the regex
                matcher = namePattern.matcher(state.getName());
                if (matcher.find()) {
                    bundles.add(bundle);
                } else {
                    // no match on bundle name, fall back to symbolic name and check if it matches the regex
                    matcher = namePattern.matcher(bundle);
                    if (matcher.find()) {
                        bundles.add(bundle);
                    }
                }
            } else {
                // no bundle name, fall back to symbolic name and check if it matches the regex
                matcher = namePattern.matcher(bundle);
                if (matcher.find()) {
                    bundles.add(bundle);
                }
            }
        }
    }

}
