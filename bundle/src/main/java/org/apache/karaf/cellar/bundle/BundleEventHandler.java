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
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class BundleEventHandler extends BundleSupport implements EventHandler<RemoteBundleEvent> {

	private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

	public static final String SWITCH_ID = "com.upstreamsystems.curry.cluster.bundle.handler";

	private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private Node node;

	/**
	 * Handles remote bundle events.
	 * @param event
	 */
	public void handle(RemoteBundleEvent event) {

        if (event == null || event.getSourceGroup() == null || node == null || node.equals(event.getSourceNode()))
            return;

        Group group = event.getSourceGroup();
        String groupName = group.getName();
        String bundleLocation = event.getLocation();

        Map<String, BundleState> bundleTable = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);

		try {
             //Check if the pid is marked as local.
            if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, bundleLocation, EventType.INBOUND)) {
				BundleState state = new BundleState();
				state.setLocation(event.getLocation());
				state.setStatus(event.getType());

				if(event.getType() == BundleEvent.INSTALLED) {
					installBundleFromLocation(event.getLocation());
					bundleTable.put(event.getId(),state);
                    LOGGER.info("CELLAR BUNDLE EVENT: installed {}/{}", event.getSymbolicName(), event.getVersion());
				} else if(event.getType() == BundleEvent.UNINSTALLED) {
					uninstallBundle(event.getSymbolicName(), event.getVersion());
					bundleTable.remove(event.getId());
                    LOGGER.info("CELLAR BUNDLE EVENT: uninstalled {}/{}", event.getSymbolicName(), event.getVersion());
				} else if(event.getType() == BundleEvent.STARTED) {
					startBundle(event.getSymbolicName(),event.getVersion());
					bundleTable.put(event.getId(),state);
                    LOGGER.info("CELLAR BUNDLE EVENT: started {}/{}", event.getSymbolicName(), event.getVersion());
				} else if(event.getType() == BundleEvent.STOPPED) {
					stopBundle(event.getSymbolicName(), event.getVersion());
					state.setStatus(BundleEvent.INSTALLED);
					bundleTable.put(event.getId(),state);
                    LOGGER.info("CELLAR BUNDLE EVENT: stopped {}/{}", event.getSymbolicName(), event.getVersion());
				} else if(event.getType() == BundleEvent.UPDATED) {
					updateBundle(event.getSymbolicName(), event.getVersion());
                    LOGGER.info("CELLAR BUNDLE EVENT: updated {}/{}", event.getSymbolicName(), event.getVersion());
				}
			} else LOGGER.debug("Bundle with symbolicName {} is marked as BLOCKED INBOUND", event.getSymbolicName());
		} catch (BundleException e) {
			LOGGER.info("Failed to install bundle.", e);
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
