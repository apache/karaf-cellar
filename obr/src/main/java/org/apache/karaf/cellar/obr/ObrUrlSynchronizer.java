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
package org.apache.karaf.cellar.obr;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.BootFinished;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

/**
 * OBR URL Synchronizer.
 */
public class ObrUrlSynchronizer extends ObrSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlSynchronizer.class);

    private EventProducer eventProducer;

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @Override
    public void init(BundleContext bundleContext) {
        // wait the end of Karaf boot process
        ServiceTracker tracker = new ServiceTracker(bundleContext, BootFinished.class, null);
        try {
            tracker.waitForService(120000);
        } catch (Exception e) {
            LOGGER.warn("Can't start BootFinished service tracker", e);
        }
        if (groupManager == null)
            return;
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                sync(group);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Sync node and cluster states, depending of the sync policy.
     *
     * @param group the target cluster group.
     */
    @Override
    public void sync(Group group) {
        String policy = getSyncPolicy(group);
        if (policy == null) {
            LOGGER.warn("CELLAR OBR: sync policy is not defined for cluster group {}", group.getName());
        } else if (policy.equalsIgnoreCase("cluster")) {
            LOGGER.debug("CELLAR OBR: sync policy set as 'cluster' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR OBR: updating node from the cluster (pull first)");
            pull(group);
            LOGGER.debug("CELLAR OBR: updating cluster from the local node (push after)");
            push(group);
        } else if (policy.equalsIgnoreCase("node")) {
            LOGGER.debug("CELLAR OBR: sync policy set as 'node' for cluster group {}", group.getName());
            LOGGER.debug("CELLAR OBR: updating cluster from the local node (push first)");
            push(group);
            LOGGER.debug("CELLAR OBR: updating node from the cluster (pull after)");
            pull(group);
        } else if (policy.equalsIgnoreCase("clusterOnly")) {
            LOGGER.debug("CELLAR OBR: sync policy set as 'clusterOnly' for cluster group " + group.getName());
            LOGGER.debug("CELLAR OBR: updating node from the cluster (pull only)");
            pull(group);
        } else if (policy.equalsIgnoreCase("nodeOnly")) {
            LOGGER.debug("CELLAR OBR: sync policy set as 'nodeOnly' for cluster group " + group.getName());
            LOGGER.debug("CELLAR OBR: updating cluster from the local node (push only)");
            push(group);
        } else {
            LOGGER.debug("CELLAR OBR: sync policy set as 'disabled' for cluster group " + group.getName());
            LOGGER.debug("CELLAR OBR: no sync");
        }
    }

    /**
     * Pull the OBR URLs from a cluster group to update the local state.
     *
     * @param group the cluster group.
     */
    @Override
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Set<String> clusterUrls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                if (clusterUrls != null && !clusterUrls.isEmpty()) {
                    for (String url : clusterUrls) {
                        try {
                            LOGGER.debug("CELLAR OBR: adding repository URL {}", url);
                            obrService.addRepository(url);
                        } catch (Exception e) {
                            LOGGER.error("CELLAR OBR: failed to add repository URL {}", url, e);
                        }
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push the local OBR URLs to a cluster group.
     *
     * @param group the cluster group.
     */
    @Override
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Set<String> clusterUrls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                Repository[] repositories = obrService.listRepositories();
                for (Repository repository : repositories) {
                    if (isAllowed(group, Constants.URLS_CONFIG_CATEGORY, repository.getURI().toString(), EventType.OUTBOUND)) {
                        LOGGER.debug("CELLAR OBR: adding repository {} to the cluster", repository.getURI().toString());
                        // update cluster state
                        clusterUrls.add(repository.getURI().toString());
                        // send cluster event
                        ClusterObrUrlEvent urlEvent = new ClusterObrUrlEvent(repository.getURI().toString(), Constants.URL_ADD_EVENT_TYPE);
                        urlEvent.setSourceGroup(group);
                        urlEvent.setSourceNode(clusterManager.getNode());
                        urlEvent.setLocal(clusterManager.getNode());
                        eventProducer.produce(urlEvent);
                        // update OBR bundles in the cluster group
                        Set<ObrBundleInfo> clusterBundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
                        Resource[] resources = repository.getResources();
                        for (Resource resource : resources) {
                            LOGGER.debug("CELLAR OBR: adding bundle {} to the cluster", resource.getPresentationName());
                            ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                            clusterBundles.add(info);
                        }
                    } else {
                        LOGGER.trace("CELLAR OBR: URL {} is marked BLOCKED OUTBOUND for cluster group {}", repository.getURI().toString(), groupName);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Get the OBR sync policy for the given cluster group.
     *
     * @param group the cluster group.
     * @return the current OBR sync policy for the given cluster group.
     */
    @Override
    public String getSyncPolicy(Group group) {
        String groupName = group.getName();
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.URLS_CONFIG_CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                return properties.get(propertyKey).toString();
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR OBR: error while retrieving the sync policy", e);
        }

        return null;
    }

}
