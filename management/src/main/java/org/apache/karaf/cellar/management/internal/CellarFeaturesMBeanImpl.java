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

import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureInfo;
import org.apache.karaf.cellar.features.RemoteFeaturesEvent;
import org.apache.karaf.cellar.features.RemoteRepositoryEvent;
import org.apache.karaf.cellar.management.CellarFeaturesMBean;
import org.apache.karaf.features.*;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Cellar features MBean to manipulate Cellar features.
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

    public void install(String groupName, String name, String version) throws Exception {
        // check if the cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // check if the feature is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Feature " + name + " is blocked outbound");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // get the features distributed map
            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            // check if the feature exist
            FeatureInfo feature = null;
            for (FeatureInfo info : distributedFeatures.keySet()) {
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
                    throw new IllegalArgumentException("Feature " + name + " doesn't exist for cluster group " + groupName);
                else
                    throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist for cluster group " + groupName);
            }

            // update the distributed map
            distributedFeatures.put(feature, true);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // broadcast the cluster event
        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureInstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void install(String groupName, String name) throws Exception {
        this.install(groupName, name, null);
    }

    public void uninstall(String groupName, String name, String version) throws Exception {
        // check if cluster group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF for this node");
        }

        // check if the feature is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.FEATURES_CATEGORY, name, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("Feature " + name + " is blocked outbound");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // get the features distributed map
            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            // check if the feature exist
            FeatureInfo feature = null;
            for (FeatureInfo info : distributedFeatures.keySet()) {
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
                    throw new IllegalArgumentException("Feature " + name + " doesn't exist for cluster group " + groupName);
                else
                    throw new IllegalArgumentException("Feature " + name + "/" + version + " doesn't exist for cluster group " + groupName);
            }

            // update the distributed map
            distributedFeatures.put(feature, false);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        RemoteFeaturesEvent event = new RemoteFeaturesEvent(name, version, FeatureEvent.EventType.FeatureUninstalled);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void uninstall(String groupName, String name) throws Exception {
        this.uninstall(groupName, name, null);
    }

    public TabularData getFeatures(String group) throws Exception {
        CompositeType compositeType = new CompositeType("Feature", "Karaf Cellar feature",
                new String[]{"name", "version", "installed"},
                new String[]{"Name of the feature", "Version of the feature", "Whether the feature is installed or not"},
                new OpenType[]{SimpleType.STRING, SimpleType.STRING, SimpleType.BOOLEAN});
        TabularType tableType = new TabularType("Features", "Table of all Karaf Cellar features",
                compositeType, new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tableType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<FeatureInfo, Boolean> allFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + group);
            if (allFeatures != null && !allFeatures.isEmpty()) {
                for (FeatureInfo feature : allFeatures.keySet()) {
                    boolean installed = allFeatures.get(feature);
                    CompositeData data = new CompositeDataSupport(compositeType,
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

    public List<String> getUrls(String groupName) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // get the distributed URLs list
        List<String> urls = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);

        List<String> result = new ArrayList<String>();
        for (String url : urls) {
            result.add(url);
        }

        return result;
    }

    public void addUrl(String groupName, String url) throws Exception {
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
            // get the repositories distributed list
            List<String> distributedRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
            // get the features distributed map
            Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

            // check if the URL is already registered
            boolean found = false;
            for (String repository : distributedRepositories) {
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
                        throw new IllegalArgumentException("Repository URL " + url + " is not valid: " + e.getMessage());
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

                // update the distributed list
                distributedRepositories.add(url);

                // update the distributed feature map
                for (Feature feature : repository.getFeatures()) {
                    FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                    distributedFeatures.put(info, false);
                }

                // unregister the repository if it's not local registered
                if (!localRegistered)
                    featuresService.removeRepository(new URI(url));

                // broadcast the cluster event
                RemoteRepositoryEvent event = new RemoteRepositoryEvent(url, RepositoryEvent.EventType.RepositoryAdded);
                event.setSourceGroup(group);
                eventProducer.produce(event);
            } else {
                throw new IllegalArgumentException("Repository URL " + url + " already registered");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public void removeUrl(String groupName, String url) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the event producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // get the distributed list
        List<String> distributedRepositories = clusterManager.getList(Constants.REPOSITORIES + Configurations.SEPARATOR + groupName);
        // get the features distributed map
        Map<FeatureInfo, Boolean> distributedFeatures = clusterManager.getMap(Constants.FEATURES + Configurations.SEPARATOR + groupName);

        // looking for the URL in the list
        boolean found = false;
        for (String repository : distributedRepositories) {
            if (repository.equals(url)) {
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
                    throw new IllegalArgumentException("Repository URL " + url + " is not valid: " + e.getMessage());
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

            // update the list
            distributedRepositories.remove(url);

            // update the distributed feature map
            for (Feature feature : repository.getFeatures()) {
                FeatureInfo info = new FeatureInfo(feature.getName(), feature.getVersion());
                distributedFeatures.remove(info);
            }

            // unregister the repository if it's not local registered
            if (!localRegistered)
                featuresService.removeRepository(new URI(url));

            // broadcast a cluster event
            RemoteRepositoryEvent event = new RemoteRepositoryEvent(url, RepositoryEvent.EventType.RepositoryRemoved);
            event.setSourceGroup(group);
            eventProducer.produce(event);
        } else {
            throw new IllegalArgumentException("Repository URL " + url + " not found");
        }
    }

}
