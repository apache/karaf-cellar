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
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Set;

/**
 * Bootstrap synchronizer for the OBR URLs.
 */
public class ObrUrlSynchronizer extends ObrSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlSynchronizer.class);

    public void init() {
        super.init();
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else LOGGER.warn("CELLAR OBR: sync is disabled for group {}", group.getName());
            }
        }
    }

    public void destroy() {
        super.destroy();
    }

    /**
     * Pull the OBR URL from the cluster.
     *
     * @param group the cluster group.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                if (urls != null && !urls.isEmpty()) {
                    for (String url : urls) {
                        try {
                            obrService.addRepository(url);
                            LOGGER.debug("CELLAR OBR: add OBR repository URL {}", url);
                        } catch (Exception e) {
                            LOGGER.error("CELLAR OBR: unable to add the OBR repository URL {}", url, e);
                        }
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    /**
     * Push the local OBR URLs to the cluster
     *
     * @param group the cluster group.
     */
    public void push(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                Repository[] repositories = obrService.listRepositories();
                for (Repository repository : repositories) {
                    if (isAllowed(group, Constants.URLS_CONFIG_CATEGORY, repository.getURI().toString(), EventType.OUTBOUND)) {
                        urls.add(repository.getURI().toString());
                        // push the bundles in the OBR distributed set
                        Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
                        Resource[] resources = repository.getResources();
                        for (Resource resource : resources) {
                            ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                            bundles.add(info);
                            // TODO fire event to the other nodes ?
                        }
                    } else {
                        LOGGER.warn("CELLAR OBR: URL " + repository.getURI().toString() + " is blocked outbound");
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    @Override
    public Boolean isSyncEnabled(Group group) {
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
            Dictionary<String, String> properties = configuration.getProperties();
            String propertyKey = groupName + Configurations.SEPARATOR + Constants.URLS_CONFIG_CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
            String propertyValue = properties.get(propertyKey);
            result = Boolean.parseBoolean(propertyValue);
        } catch (IOException e) {
            LOGGER.error("CELLAR OBR: error while checking if sync is enabled", e);
        }
        return result;
    }

}
