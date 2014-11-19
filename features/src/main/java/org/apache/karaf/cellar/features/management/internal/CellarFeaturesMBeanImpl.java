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
package org.apache.karaf.cellar.features.management.internal;

// import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.features.ClusterFeaturesEvent;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.features.ClusterRepositoryEvent;
import org.apache.karaf.cellar.features.management.CellarFeaturesMBean;
import org.apache.karaf.features.*;
// import org.osgi.framework.BundleEvent;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Cellar Features MBean.
 */
public class CellarFeaturesMBeanImpl extends StandardMBean implements CellarFeaturesMBean {

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private EventProducer eventProducer;
    private FeaturesService featuresService;
    private ConfigurationAdmin configurationAdmin;

    public CellarFeaturesMBeanImpl() throws NotCompliantMBeanException {
        super(CellarFeaturesMBean.class);
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

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Override
    public void installFeature(String groupName, String name, String version, boolean noClean, boolean noRefresh) throws Exception {
        this.installFeature(groupName, name, version, noClean, noRefresh, false);
    }

    @Override
    public void installFeature(String groupName, String name, String version, boolean noClean, boolean noRefresh, boolean noStart) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the feature is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.CATEGORY, name, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Feature " + name + " is blocked outbound for cluster group " + groupName);
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // get the features in the cluster group
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            // check if the feature exist
            FeatureInfo feature = null;
            for (FeatureInfo info : clusterFeatures.keySet()) {
                if (version == null) {
                    if (info.getName().equals(name)) {
                        feature = info;
                        break;
                    }
                } else {
                    if (info.getName().equals(name) && info.getVersion().equals(version)) {
                        feature = info;
                        break;
                    }
                }
            }

            if (feature == null) {
                if (version == null)
                    throw new IllegalArgumentException("Feature " + name + " doesn't exist in cluster group " + groupName);
                else
                    throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist in cluster group " + groupName);
            }

            // update the cluster group
            clusterFeatures.put(feature, true);
            try {
                // TODO does it make sense ?
                /*
                List<BundleInfo> bundles = featuresService.getFeature(feature.getName(), version).getBundles();
                Map<String, BundleState> clusterBundles = clusterManager.getMap(org.apache.karaf.cellar.bundle.Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
                for (BundleInfo bundle : bundles) {
                    BundleState state = new BundleState();
                    state.setLocation(bundle.getLocation());
                    state.setStatus(BundleEvent.STARTED);
                    clusterBundles.put(bundle.toString(), state);
                }
                */
            } catch (Exception e) {
                // ignore
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        ClusterFeaturesEvent event = new ClusterFeaturesEvent(name, version, noClean, noRefresh, noStart, FeatureEvent.EventType.FeatureInstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    @Override
    public void installFeature(String groupName, String name, String version) throws Exception {
        this.installFeature(groupName, name, version, false, false);
    }

    @Override
    public void installFeature(String groupName, String name) throws Exception {
        this.installFeature(groupName, name, null);
    }

    @Override
    public void installFeature(String groupName, String name, boolean noClean, boolean noRefresh) throws Exception {
        this.installFeature(groupName, name, null, noClean, noRefresh, false);
    }

    @Override
    public void installFeature(String groupName, String name, boolean noClean, boolean noRefresh, boolean noStart) throws Exception {
        this.installFeature(groupName, name, null, noClean, noRefresh, noStart);
    }

    @Override
    public void uninstallFeature(String groupName, String name, String version) throws Exception {
        this.uninstallFeature(groupName, name, version, false);
    }

    @Override
    public void uninstallFeature(String groupName, String name, String version, boolean noRefresh) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the feature is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.CATEGORY, name, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Feature " + name + " is blocked outbound for cluster group " + groupName);
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // get the features in the cluster group
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            // check if the feature exist
            FeatureInfo feature = null;
            for (FeatureInfo info : clusterFeatures.keySet()) {
                if (version == null) {
                    if (info.getName().equals(name)) {
                        feature = info;
                        break;
                    }
                } else {
                    if (info.getName().equals(name) && info.getVersion().equals(version)) {
                        feature = info;
                        break;
                    }
                }
            }

            if (feature == null) {
                if (version == null)
                    throw new IllegalArgumentException("Feature " + name + " doesn't exist in cluster group " + groupName);
                else
                    throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist in cluster group " + groupName);
            }

            // update the cluster group
            clusterFeatures.put(feature, false);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        ClusterFeaturesEvent event = new ClusterFeaturesEvent(name, version, false, noRefresh, false, FeatureEvent.EventType.FeatureUninstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    @Override
    public void uninstallFeature(String groupName, String name) throws Exception {
        this.uninstallFeature(groupName, name, null, false);
    }

    @Override
    public void uninstallFeature(String groupName, String name, boolean noRefresh) throws Exception {
        this.uninstallFeature(groupName, name, null, noRefresh);
    }

    @Override
    public TabularData getFeatures(String group) throws Exception {
        CompositeType featuresType = new CompositeType("Feature", "Karaf Cellar feature",
                new String[]{"name", "version", "installed"},
                new String[]{"Name of the feature", "Version of the feature", "Whether the feature is installed or not"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN});

        TabularType tabularType = new TabularType("Features", "Table of all Karaf Cellar features",
                featuresType, new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tabularType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + group);
            if (clusterFeatures != null && !clusterFeatures.isEmpty()) {
                for (FeatureInfo feature : clusterFeatures.keySet()) {
                    boolean installed = clusterFeatures.get(feature);
                    CompositeData data = new CompositeDataSupport(featuresType,
                            new String[]{"name", "version", "installed"},
                            new Object[]{feature.getName(), feature.getVersion(), installed});
                    table.put(data);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return table;
    }

    @Override
    public List<String> getRepositories(String groupName) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // get the features repositories in the cluster group
        List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);

        List<String> result = new ArrayList<String>();
        for (String clusterRepository : clusterRepositories) {
            result.add(clusterRepository);
        }

        return result;
    }

    @Override
    public void addRepository(String groupName, String url) throws Exception {
        this.addRepository(groupName, url, false);
    }

    @Override
    public void addRepository(String groupName, String url, boolean install) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the event producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // get the features repositories in the cluster group
            List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);
            // get the features in the cluster group
            Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

            // check if the URL is already registered
            boolean found = false;
            for (String repository : clusterRepositories) {
                if (repository.equals(url)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                // update the repository temporary locally
                Repository repository = null;
                boolean localRegistered = false;
                // local lookup
                for (Repository registeredRepository : featuresService.listRepositories()) {
                    if (registeredRepository.getURI().equals(new URI(url))) {
                        repository = registeredRepository;
                        break;
                    }
                }
                if (repository == null) {
                    // registered locally
                    try {
                        featuresService.addRepository(new URI(url));
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Features repository URL " + url + " is not valid: " + e.getMessage());
                    }
                    // get the repository
                    for (Repository registeredRepository : featuresService.listRepositories()) {
                        if (registeredRepository.getURI().equals(new URI(url))) {
                            repository = registeredRepository;
                            break;
                        }
                    }
                } else {
                    localRegistered = true;
                }

                // update the cluster group
                clusterRepositories.add(url);

                for (Feature feature : repository.getFeatures()) {
                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                    clusterFeatures.put(info, false);
                }

                // un-register the repository if it's not local registered
                if (!localRegistered)
                    featuresService.removeRepository(new URI(url));

                // broadcast the cluster event
                ClusterRepositoryEvent event = new ClusterRepositoryEvent(url, RepositoryEvent.EventType.RepositoryAdded);
                event.setInstall(install);
                event.setSourceGroup(group);
                eventProducer.produce(event);
            } else {
                throw new IllegalArgumentException("Features repository URL " + url + " already registered");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void removeRepository(String groupName, String url) throws Exception {
        this.removeRepository(groupName, url, false);
    }

    @Override
    public void removeRepository(String groupName, String url, boolean uninstall) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the event producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // get the features repositories in the cluster group
        List<String> clusterRepositories = clusterManager.getList(Constants.REPOSITORIES_MAP + Configurations.SEPARATOR + groupName);
        // get the features in the cluster group
        Map<FeatureInfo, Boolean> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);

        // looking for the URL in the list
        boolean found = false;
        for (String clusterRepository : clusterRepositories) {
            if (clusterRepository.equals(url)) {
                found = true;
                break;
            }
        }
        if (found) {
            // update the repository temporary locally
            Repository repository = null;
            boolean localRegistered = false;
            // local lookup
            for (Repository registeredRepository : featuresService.listRepositories()) {
                if (registeredRepository.getURI().equals(new URI(url))) {
                    repository = registeredRepository;
                    break;
                }
            }
            if (repository == null) {
                // registered locally
                try {
                    featuresService.addRepository(new URI(url));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Features repository URL " + url + " is not valid: " + e.getMessage());
                }
                // get the repository
                for (Repository registeredRepository : featuresService.listRepositories()) {
                    if (registeredRepository.getURI().equals(new URI(url))) {
                        repository = registeredRepository;
                        break;
                    }
                }
            } else {
                localRegistered = true;
            }

            // update the cluster group
            clusterRepositories.remove(url);

            for (Feature feature : repository.getFeatures()) {
                FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                clusterFeatures.remove(info);
            }

            // un-register the repository if it's not local registered
            if (!localRegistered)
                featuresService.removeRepository(new URI(url));

            // broadcast a cluster event
            ClusterRepositoryEvent event = new ClusterRepositoryEvent(url, RepositoryEvent.EventType.RepositoryRemoved);
            event.setUninstall(uninstall);
            event.setSourceGroup(group);
            eventProducer.produce(event);
        } else {
            throw new IllegalArgumentException("Features repository URL " + url + " not found in cluster group " + groupName);
        }
    }

}
