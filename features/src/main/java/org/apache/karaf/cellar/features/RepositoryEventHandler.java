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
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Handler for cluster features repository event.
 */
public class RepositoryEventHandler extends FeaturesSupport implements EventHandler<ClusterRepositoryEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RepositoryEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.repository.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Handle cluster features repository event.
     *
     * @param event the cluster event to handle.
     */
    @Override
    public void handle(ClusterRepositoryEvent event) {
    	
    	// check if the handler is ON
        if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR FEATURES: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }
        
        if (groupManager == null) {
        	//in rare cases for example right after installation this happens!
        	LOGGER.error("CELLAR FEATURES: retrieved event {} while groupManager is not available yet!", event);
        	return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR FEATURES: node is not part of the event cluster group");
            return;
        }

        String uri = event.getId();
        RepositoryEvent.EventType type = event.getType();
        try {
            // TODO check if isAllowed
            if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                if (!isRepositoryRegisteredLocally(uri)) {
                    LOGGER.debug("CELLAR FEATURES: adding repository URI {}", uri);
                    featuresService.addRepository(new URI(uri), event.getInstall());
                } else {
                    LOGGER.debug("CELLAR FEATURES: repository URI {} is already registered locally");
                }
            } else {
                if (isRepositoryRegisteredLocally(uri)) {
                    LOGGER.debug("CELLAR FEATURES: removing repository URI {}", uri);
                    featuresService.removeRepository(new URI(uri), event.getUninstall());
                } else {
                    LOGGER.debug("CELLAR FEATURES: repository URI {} is not registered locally");
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR FEATURES: failed to add/remove repository URL {}", uri, e);
        }
    }

    @Override
    public Class<ClusterRepositoryEvent> getType() {
        return ClusterRepositoryEvent.class;
    }

    @Override
    public Switch getSwitch() {
        return eventSwitch;
    }

}
