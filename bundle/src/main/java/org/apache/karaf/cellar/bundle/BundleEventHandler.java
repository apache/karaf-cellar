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

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleEventHandler extends BundleSupport implements EventHandler<RemoteBundleEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.bundle.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    
    private FeaturesService featureService;

    /**
     * Handles remote bundle events.
     *
     * @param event
     */
    public void handle(RemoteBundleEvent event) {
        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR BUNDLE: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        // check if the node is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR BUNDLE: node is not part of the event cluster group");
            return;
        }

        try {
            //Check if the pid is marked as local.
            if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, event.getLocation(), EventType.INBOUND)) {
            	//check the features first
            	List<Feature> matchingFeatures = retrieveFeature(event);
            	for (Feature feature : matchingFeatures) {
					if (!isAllowed(event.getSourceGroup(), "features", feature.getName(), EventType.INBOUND)) {
						LOGGER.warn("CELLAR BUNDLE: bundle {} is contained in a feature marked as BLOCKED INBOUND", event.getLocation());
					}
				}
                if (event.getType() == BundleEvent.INSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: installing bundle {} from {}", event.getId(), event.getLocation());
                    installBundleFromLocation(event.getLocation());
                } else if (event.getType() == BundleEvent.UNINSTALLED) {
                    LOGGER.debug("CELLAR BUNDLE: un-installing bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    uninstallBundle(event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.STARTED) {
                    LOGGER.debug("CELLAR BUNDLE: starting bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    startBundle(event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.STOPPED) {
                    LOGGER.debug("CELLAR BUNDLE: stopping bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    stopBundle(event.getSymbolicName(), event.getVersion());
                } else if (event.getType() == BundleEvent.UPDATED) {
                    LOGGER.debug("CELLAR BUNDLE: updating bundle {}/{}", event.getSymbolicName(), event.getVersion());
                    updateBundle(event.getSymbolicName(), event.getVersion());
                }
            } else LOGGER.warn("CELLAR BUNDLE: bundle {} is marked as BLOCKED INBOUND", event.getLocation());
        } catch (BundleException e) {
            LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", e);
        } catch (Exception e) {
        	LOGGER.error("CELLAR BUNDLE: failed to handle bundle event", e);
		}
    }

    private List<Feature> retrieveFeature(RemoteBundleEvent event) throws Exception {
    	Feature[] features = featureService.listFeatures();
    	List<Feature> matchingFeatures = new ArrayList<Feature>();
    	for (Feature feature : features) {
			List<BundleInfo> bundles = feature.getBundles();
			for (BundleInfo bundleInfo : bundles) {
				String location = bundleInfo.getLocation();
				if (location.equalsIgnoreCase(event.getLocation())) {
					matchingFeatures.add(feature);
				}
			}
		}
		return matchingFeatures;
	}

	/**
     * Initialization Method.
     */
    public void init() {

    }

    /**
     * Destruction Method.
     */
    public void destroy() {

    }

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

    public Class<RemoteBundleEvent> getType() {
        return RemoteBundleEvent.class;
    }

	/**
	 * @return the featureService
	 */
	public FeaturesService getFeatureService() {
		return featureService;
	}

	/**
	 * @param featureService the featureService to set
	 */
	public void setFeatureService(FeaturesService featureService) {
		this.featureService = featureService;
	}

}
