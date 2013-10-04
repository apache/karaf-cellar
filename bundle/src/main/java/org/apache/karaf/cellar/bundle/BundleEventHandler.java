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
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.Feature;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The BundleEventHandler is responsible to process received cluster event for bundles.
 */
public class BundleEventHandler extends BundleSupport implements EventHandler<ClusterBundleEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    
    /**
     * Handle received bundle cluster events.
     *
     * @param event the received bundle cluster event.
     */
    @Override
    public void handle(ClusterBundleEvent event) {

        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR BUNDLE: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }
        
        if (groupManager == null) {
        	//in rare cases for example right after installation this happens!
        	LOGGER.error("CELLAR BUNDLE: retrieved event {} while groupManager is not available yet!", event);
        	return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR BUNDLE: node is not part of the event cluster group {}", event.getSourceGroup().getName());
            return;
        }

        try {
            // check if the pid is marked as local.
            if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, event.getLocation(), EventType.INBOUND)) {
            	// check the features first
            	List<Feature> matchingFeatures = retrieveFeature(event.getLocation());
            	for (Feature feature : matchingFeatures) {
					if (!isAllowed(event.getSourceGroup(), "features", feature.getName(), EventType.INBOUND)) {
						LOGGER.debug("CELLAR BUNDLE: bundle {} is contained in feature {} marked BLOCKED INBOUND for cluster group {}", event.getLocation(), feature.getName(), event.getSourceGroup().getName());
						return;
					}
				}
                if (event.getType() == BundleEvent.INSTALLED) {
                    installBundleFromLocation(event.getLocation());
                    LOGGER.debug("CELLAR BUNDLE: installing {}/{}", event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.UNINSTALLED) {
                    uninstallBundle(event.getSymbolicName(), event.getVersion());
                    LOGGER.debug("CELLAR BUNDLE: uninstalling {}/{}", event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.STARTED) {
                    startBundle(event.getSymbolicName(), event.getVersion());
                    LOGGER.debug("CELLAR BUNDLE: starting {}/{}", event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.STOPPED) {
                    stopBundle(event.getSymbolicName(), event.getVersion());
                    LOGGER.debug("CELLAR BUNDLE: stopping {}/{}", event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.UPDATED) {
                    updateBundle(event.getSymbolicName(), event.getVersion());
                    LOGGER.debug("CELLAR BUNDLE: updating {}/{}", event.getSymbolicName(), event.getVersion());
                }
            } else LOGGER.debug("CELLAR BUNDLE: bundle {} is marked BLOCKED INBOUND for cluster group {}", event.getSymbolicName(), event.getSourceGroup().getName());
        } catch (BundleException e) {
            LOGGER.error("CELLAR BUNDLE: failed to install bundle {}/{}.", new Object[]{event.getSymbolicName(), event.getVersion()}, e);
        } catch (Exception e) {
        	LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", e);
        }
    }

    public void init() {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    /**
     * Get the cluster bundle event handler switch.
     *
     * @return the cluster bundle event handler switch.
     */
    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.HANDLER + "." + this.getClass().getName()));
                if (status) {
                    eventSwitch.turnOn();
                } else {
                    eventSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return eventSwitch;
    }

    /**
     * Get the cluster event type.
     *
     * @return the cluster bundle event type.
     */
    @Override
    public Class<ClusterBundleEvent> getType() {
        return ClusterBundleEvent.class;
    }

}
