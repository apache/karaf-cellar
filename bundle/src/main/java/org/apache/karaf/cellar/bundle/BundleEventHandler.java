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
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BundleEventHandler extends BundleSupport implements EventHandler<RemoteBundleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private Node node;

    /**
     * Handles remote bundle events.
     *
     * @param event
     */
    public void handle(RemoteBundleEvent event) {

        Group group = event.getSourceGroup();
        String groupName = "";
        if (group != null) {
            groupName = group.getName();
        }
        String bundleLocation = event.getLocation();

        Map<String, BundleState> bundleTable = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

        try {
            //Check if the pid is marked as local.
            if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, bundleLocation, EventType.INBOUND)) {
                BundleState state = new BundleState();
                state.setStatus(event.getType());

                if (event.getType() == BundleEvent.INSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: installing bundle {} from {}", event.getId(), event.getLocation());
                    Bundle bundle = installBundleFromLocation(event.getLocation());
                    state.setLocation(event.getLocation());
                    event.setSymbolicName(bundle.getSymbolicName());
                    event.setVersion(bundle.getVersion().toString());
                    event.setId(bundle.getSymbolicName() + "/" + bundle.getVersion());
                    bundleTable.put(event.getId(), state);
                } else if (event.getType() == BundleEvent.UNINSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: un-installing bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    uninstallBundle(event.getSymbolicName(), event.getVersion());
                    bundleTable.remove(event.getId());
                } else if (event.getType() == BundleEvent.STARTED) {
                    LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    Bundle bundle = startBundle(event.getSymbolicName(), event.getVersion());
                    state.setLocation(bundle.getLocation());
                    bundleTable.put(event.getId(), state);
                } else if (event.getType() == BundleEvent.STOPPED) {
                    LOGGER.debug("CELLAR BUNDLE: stopping bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    Bundle bundle = stopBundle(event.getSymbolicName(), event.getVersion());
                    state.setStatus(BundleEvent.INSTALLED);
                    state.setLocation(bundle.getLocation());
                    bundleTable.put(event.getId(), state);
                } else if (event.getType() == BundleEvent.UPDATED) {
                    LOGGER.debug("CELLAR BUNDLE: updating bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    updateBundle(event.getSymbolicName(), event.getVersion());
                }
            } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked as BLOCKED INBOUND", bundleLocation);
        } catch (BundleException e) {
            LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", e);
        }
    }

    /**
     * Initialization Method.
     */
    public void init() {
        if (clusterManager != null) {
            node = clusterManager.getNode();
        }
    }

    /**
     * Destruction Method.
     */
    public void destroy() {

    }

    public Switch getSwitch() {
        return eventSwitch;
    }

    public Class<RemoteBundleEvent> getType() {
        return RemoteBundleEvent.class;
    }

}
