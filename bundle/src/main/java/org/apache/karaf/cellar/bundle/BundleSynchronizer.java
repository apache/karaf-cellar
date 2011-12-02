/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.bundle;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BundleSynchronizer extends BundleSupport implements Synchronizer {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleSynchronizer.class);

    private List<EventProducer> producerList;

    /**
     * Registration method
     */
    public void init() {
        Set<Group> groups = groupManager.listLocalGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                if (isSyncEnabled(group)) {
                    pull(group);
                    push(group);
                } else LOGGER.warn("CELLAR BUNDLE: sync is disabled for group {}", group.getName());
            }
        }
    }

    /**
     * Destruction method
     */
    public void destroy() {

    }

    /**
     * Reads the configuration from the remote map.
     */
    public void pull(Group group) {
        if (group != null) {
            String groupName = group.getName();
            Map<String, BundleState> bundleTable = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

            for (Map.Entry<String,BundleState> entry : bundleTable.entrySet()) {
                String id = entry.getKey();
                BundleState state = entry.getValue();

                //Check if the pid is marked as local.
                String[] tokens = id.split("/");
                String symbolicName = tokens[0];
                String version = tokens[1];
                if (tokens != null && tokens.length > 2) {
                    if (state != null) {
                        String bundleLocation = state.getLocation();
                        if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.INBOUND)) {
                            try {
                                if (state.getStatus() == BundleEvent.INSTALLED) {
                                    installBundleFromLocation(state.getLocation());
                                    startBundle(symbolicName, version);
                                } else if (state.getStatus() == BundleEvent.STARTED) {
                                    installBundleFromLocation(state.getLocation());
                                }
                            } catch (BundleException e) {
                                LOGGER.error("CELLAR BUNDLE: failed to pull bundle {}", id, e);
                            }
                        } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked as BLOCKED INBOUND", bundleLocation);
                    }
                }
            }
        }
    }

    /**
     * Publishes local configuration to the cluster.
     */
    public void push(Group group) {

        if (group != null) {
            String groupName = group.getName();
            Map<String, BundleState> bundleTable = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
            Bundle[] bundles;
            BundleContext bundleContext = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();

            bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles) {
                String symbolicName = bundle.getSymbolicName();
                String version = bundle.getVersion().toString();
                String bundleLocation = bundle.getLocation();
                int status = bundle.getState();
                String id = symbolicName + "/" + version;

                //Check if the pid is marked as local.
                if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {

                    BundleState bundleState = new BundleState();
                    bundleState.setLocation(bundleLocation);
                    bundleState.setStatus(status);

                    BundleState existingState = bundleTable.get(id);
                    RemoteBundleEvent event = null;
                    if (existingState == null) {
                        event = new RemoteBundleEvent(symbolicName, version, bundleLocation, status);
                    } else if (bundleState.getStatus() == BundleEvent.STARTED) {
                        event = new RemoteBundleEvent(symbolicName, version, bundleLocation, status);
                    }
                    if (producerList != null && !producerList.isEmpty() && event != null) {
                        for (EventProducer producer : producerList) {
                            producer.produce(event);
                        }
                    }
                } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked as BLOCKED OUTBOUND", bundleLocation);
            }
        }
    }

    public Boolean isSyncEnabled(Group group) {
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP);
            Dictionary<String, String> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                String propertyValue = properties.get(propertyKey);
                result = Boolean.parseBoolean(propertyValue);
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR BUNDLE: failed to check if sync is enabled", e);
        }
        return result;
    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }

}
