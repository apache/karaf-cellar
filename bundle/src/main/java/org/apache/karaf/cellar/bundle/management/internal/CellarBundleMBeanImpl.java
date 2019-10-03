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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.net.URL;
import java.util.*;
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
    private BundleContext bundleContext;

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

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public void install(String groupName, String location) throws Exception {
        this.install(groupName, location, null,false);
    }

    @Override
    public void install(String groupName, String location, boolean start) throws Exception {
        this.install(groupName, location, null, start);
    }

    @Override
    public void install(String groupName, String location, Integer level, boolean start) throws Exception {
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
        if (manifest == null) {
            throw new IllegalArgumentException("Bundle location " + location + " doesn't seem correct");
        }
        String name = manifest.getMainAttributes().getValue("Bundle-Name");
        String symbolicName = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
        if (name == null) {
            name = symbolicName;
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
            state.setSymbolicName(symbolicName);
            state.setVersion(version);
            state.setId(clusterBundles.size());
            state.setStartLevel(level);
            state.setLocation(location);
            if (start) {
                state.setStatus(Bundle.ACTIVE);
            } else {
                state.setStatus(Bundle.INSTALLED);
            }
            clusterBundles.put(symbolicName + "/" + version, state);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the event
        ClusterBundleEvent event = new ClusterBundleEvent(symbolicName, version, location, level, Bundle.INSTALLED);
        event.setSourceGroup(group);
        event.setSourceNode(clusterManager.getNode());
        if (start) {
            event = new ClusterBundleEvent(symbolicName, version, location, level, Bundle.ACTIVE);
            event.setSourceGroup(group);
            event.setSourceNode(clusterManager.getNode());
        }
        eventProducer.produce(event);
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

        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, gatherBundles(groupName));

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();

                // check if the bundle location is allowed outbound
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                clusterBundles.remove(bundle);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, null, Bundle.UNINSTALLED);
                event.setSourceGroup(group);
                event.setSourceNode(clusterManager.getNode());
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

        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {

            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, gatherBundles(groupName));

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();

                // check if the bundle location is allowed
                support.setConfigurationAdmin(this.configurationAdmin);
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                state.setStatus(Bundle.ACTIVE);
                clusterBundles.put(bundle, state);

                // broadcast the cluster event
                String[] split = bundle.split("/");

                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, state.getStartLevel(), Bundle.ACTIVE);
                event.setSourceGroup(group);
                event.setSourceNode(clusterManager.getNode());
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

        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, gatherBundles(groupName));

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();

                // check if the bundle location is allowed outbound
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                state.setStatus(Bundle.RESOLVED);
                clusterBundles.put(bundle, state);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, state.getStartLevel(), Bundle.RESOLVED);
                event.setSourceGroup(group);
                event.setSourceNode(clusterManager.getNode());
                eventProducer.produce(event);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void update(String groupName, String id, String updateLocation) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);

        // update the cluster group
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            List<String> bundles = selector(id, gatherBundles(groupName));

            for (String bundle : bundles) {
                BundleState state = clusterBundles.get(bundle);
                if (state == null) {
                    continue;
                }
                String location = state.getLocation();
                if (updateLocation != null) {
                    location = updateLocation;
                }

                // check if the bundle location is allowed outbound
                if (!support.isAllowed(group, Constants.CATEGORY, location, EventType.OUTBOUND)) {
                    continue;
                }

                // update the cluster state
                state.setLocation(location);
                clusterBundles.put(bundle, state);

                // broadcast the cluster event
                String[] split = bundle.split("/");
                ClusterBundleEvent event = new ClusterBundleEvent(split[0], split[1], location, null, BundleState.UPDATE);
                event.setSourceGroup(group);
                event.setSourceNode(clusterManager.getNode());
                eventProducer.produce(event);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void update(String groupName, String id) throws Exception {
        update(groupName, id, null);
    }

    @Override
    public void block(String groupName, String bundlePattern, boolean whitelist, boolean blacklist, boolean in, boolean out) throws Exception {
        List<String> patterns = new ArrayList<String>();

        Map<String, ExtendedBundleState> bundles = gatherBundles(groupName);
        List<String> selectedBundles = selector(bundlePattern, bundles);
        for (String selectedBundle : selectedBundles) {
            patterns.add(bundles.get(selectedBundle).getLocation());
        }

        if (patterns.isEmpty()) {
            patterns.add(bundlePattern);
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(clusterManager);
        support.setGroupManager(groupManager);
        support.setConfigurationAdmin(configurationAdmin);

        for (String pattern : patterns) {
            if (in) {
                if (whitelist)
                    support.switchListEntry(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.INBOUND, pattern);
                if (blacklist)
                    support.switchListEntry(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.INBOUND, pattern);
            }
            if (out) {
                if (whitelist)
                    support.switchListEntry(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.OUTBOUND, pattern);
                if (blacklist)
                    support.switchListEntry(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.OUTBOUND, pattern);
            }
        }
    }

    @Override
    public TabularData getBundles(String groupName) throws Exception {
        CompositeType compositeType = new CompositeType("Bundle", "Karaf Cellar bundle",
                new String[]{"id", "name", "symbolic_name", "version", "status", "location", "located", "blocked"},
                new String[]{"ID of the bundle", "Name of the bundle", "Symbolic name of the bundle", "Version of the bundle", "Current status of the bundle", "Location of the bundle", "Where the bundle is located (cluster or local node)", "The bundle blocked policy"},
                new OpenType[]{SimpleType.LONG, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING});
        TabularType tableType = new TabularType("Bundles", "Table of all Karaf Cellar bundles", compositeType,
                new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tableType);

        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(clusterManager);
        support.setConfigurationAdmin(configurationAdmin);
        support.setGroupManager(groupManager);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, ExtendedBundleState> allBundles = gatherBundles(groupName);

            List<ExtendedBundleState> bundles = new ArrayList<ExtendedBundleState>(allBundles.values());
            Collections.sort(bundles, new BundleStateComparator());

            for (ExtendedBundleState bundle : bundles) {
                String status;
                switch (bundle.getStatus()) {
                    case Bundle.INSTALLED:
                        status = "Installed";
                        break;
                    case Bundle.RESOLVED:
                        status = "Resolved";
                        break;
                    case Bundle.ACTIVE:
                        status = "Active";
                        break;
                    case Bundle.STARTING:
                        status = "Starting";
                        break;
                    case Bundle.STOPPING:
                        status = "Stopping";
                        break;
                    case Bundle.UNINSTALLED:
                        status = "Uninstalled";
                        break;
                    default:
                        status = "";
                        break;
                }

                String located = "";
                boolean local = bundle.isLocal();
                boolean cluster = bundle.isCluster();
                if (local && cluster)
                    located = "cluster/local";
                if (local && !cluster)
                    located = "local";
                if (cluster && !local)
                    located = "cluster";

                String blocked = "";
                boolean inbound = support.isAllowed(group, Constants.CATEGORY, bundle.getLocation(), EventType.INBOUND);
                boolean outbound = support.isAllowed(group, Constants.CATEGORY, bundle.getLocation(), EventType.OUTBOUND);
                if (!inbound && !outbound)
                    blocked = "in/out";
                if (!inbound && outbound)
                    blocked = "in";
                if (outbound && !inbound)
                    blocked = "out";

                CompositeData data = new CompositeDataSupport(compositeType,
                        new String[]{"id", "name", "symbolic_name", "version", "status", "location", "located", "blocked"},
                        new Object[]{bundle.getId(), bundle.getName(), bundle.getSymbolicName(), bundle.getVersion(), status, bundle.getLocation(), located, blocked});
                table.put(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    protected List<String> selector(String id, Map<String, ExtendedBundleState> clusterBundles) {
        List<String> bundles = new ArrayList<String>();

        addMatchingBundles(id, bundles, clusterBundles);

        return bundles;
    }

    protected void addMatchingBundles(String nameId, List<String> bundles, Map<String, ExtendedBundleState> clusterBundles) {
        // id is a number
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(nameId);
        if (matcher.matches()) {
            int id = Integer.parseInt(nameId);
            for (String bundle : clusterBundles.keySet()) {
                if (clusterBundles.get(bundle).getId() == id) {
                    bundles.add(bundle);
                    break;
                }
            }
            return;
        }

        // id as a number range
        pattern = Pattern.compile("^(\\d+)-(\\d+)$");
        matcher = pattern.matcher(nameId);
        if (matcher.matches()) {
            int index = nameId.indexOf('-');
            long startId = Long.parseLong(nameId.substring(0, index));
            long endId = Long.parseLong(nameId.substring(index + 1));
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

        int index = nameId.indexOf('/');
        if (index != -1) {
            // id is name/version
            String[] idSplit = nameId.split("/");
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
                        if (matcher.matches()) {
                            bundles.add(bundle);
                        } else {
                            // no match on bundle name, fall back to id and check if it matches the regex
                            matcher = namePattern.matcher(bundleSplit[0]);
                            if (matcher.matches()) {
                                bundles.add(bundle);
                            }
                        }
                    } else if (state.getSymbolicName() != null) {
                        // bundle symbolic name is populated, check if it matches the regex
                        matcher = namePattern.matcher(state.getSymbolicName());
                        if (matcher.matches()) {
                            bundles.add(bundle);
                        } else {
                            // no match on bundle symbolic name, fall back to id and check if it matches the regex
                            matcher = namePattern.matcher(bundleSplit[0]);
                            if (matcher.matches()) {
                                bundles.add(bundle);
                            }
                        }
                    } else {
                        // no bundle name, fall back to id and check if it matches the regex
                        matcher = namePattern.matcher(bundleSplit[0]);
                        if (matcher.matches()) {
                            bundles.add(bundle);
                        }
                    }
                }
            }
            return;
        }

        // id is just name
        // regex support on the name
        Pattern namePattern = Pattern.compile(nameId);
        // looking for bundle using only the name
        for (String bundle : clusterBundles.keySet()) {
            BundleState state = clusterBundles.get(bundle);
            if (state.getName() != null) {
                // bundle name is populated, check if it matches the regex
                matcher = namePattern.matcher(state.getName());
                if (matcher.matches()) {
                    bundles.add(bundle);
                } else {
                    // no match on bundle name, fall back to id and check if it matches the regex
                    String[] idSplit = bundle.split("/");
                    matcher = namePattern.matcher(idSplit[0]);
                    if (matcher.matches()) {
                        bundles.add(bundle);
                    }
                }
            } else if (state.getSymbolicName() != null) {
                // bundle symbolic name is populated, check if it matches the regex
                matcher = namePattern.matcher(state.getSymbolicName());
                if (matcher.matches()) {
                    bundles.add(bundle);
                } else {
                    // no match on bundle symbolic name, fall back to id and check if it matches the regex
                    String[] idSplit = bundle.split("/");
                    matcher = namePattern.matcher(idSplit[0]);
                    if (matcher.matches()) {
                        bundles.add(bundle);
                    }
                }
            } else {
                // no bundle name, fall back to id and check if it matches the regex
                String[] idSplit = bundle.split("/");
                matcher = namePattern.matcher(idSplit[0]);
                if (matcher.matches()) {
                    bundles.add(bundle);
                }
            }
        }
    }

    private Map<String, ExtendedBundleState> gatherBundles(String groupName) {
        Map<String, ExtendedBundleState> bundles = new HashMap<String, ExtendedBundleState>();

        // retrieve bundles from the cluster
        Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
        for (String key : clusterBundles.keySet()) {
            BundleState state = clusterBundles.get(key);
            ExtendedBundleState extendedState = new ExtendedBundleState();
            extendedState.setId(state.getId());
            extendedState.setName(state.getName());
            extendedState.setVersion(state.getVersion());
            extendedState.setSymbolicName(state.getSymbolicName());
            extendedState.setStatus(state.getStatus());
            extendedState.setLocation(state.getLocation());
            // extendedState.setData(state.getData());
            extendedState.setCluster(true);
            extendedState.setLocal(false);
            bundles.put(key, extendedState);
        }

        // retrieve local bundles
        for (Bundle bundle : bundleContext.getBundles()) {
            String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
            String symbolicName = bundle.getSymbolicName();
            String key = symbolicName + "/" + version;
            if (bundles.containsKey(key)) {
                ExtendedBundleState extendedState = bundles.get(key);
                extendedState.setLocal(true);
            } else {
                ExtendedBundleState extendedState = new ExtendedBundleState();

                // get the bundle name or location.
                String name = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                // if there is no name, then default to symbolic name.
                name = (name == null) ? symbolicName : name;
                // if there is no symbolic name, resort to location.
                name = (name == null) ? bundle.getLocation() : name;
                extendedState.setId(bundle.getBundleId());
                extendedState.setName(name);
                extendedState.setVersion(bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION));
                extendedState.setSymbolicName(symbolicName);
                extendedState.setLocation(bundle.getLocation());
                extendedState.setStatus(bundle.getState());
                extendedState.setCluster(false);
                extendedState.setLocal(true);
                bundles.put(key, extendedState);
            }
        }

        return bundles;
    }

    class ExtendedBundleState extends BundleState {

        private boolean cluster;
        private boolean local;

        public boolean isCluster() {
            return cluster;
        }

        public void setCluster(boolean cluster) {
            this.cluster = cluster;
        }

        public boolean isLocal() {
            return local;
        }

        public void setLocal(boolean local) {
            this.local = local;
        }
    }

    class BundleStateComparator implements Comparator<BundleState> {
        public int compare(BundleState b1, BundleState b2) {
            return (int) (b1.getId() - b2.getId());
        }
    }

}
