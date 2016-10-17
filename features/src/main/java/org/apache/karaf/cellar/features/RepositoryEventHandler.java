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
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;

/**
 * Handler for cluster features repository event.
 */
public class RepositoryEventHandler extends FeaturesSupport implements EventHandler<ClusterRepositoryEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RepositoryEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.repository.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void init(BundleContext bundleContext) {
        super.init(bundleContext);
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
            LOGGER.debug("CELLAR FEATURE: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }
        
        if (groupManager == null) {
        	//in rare cases for example right after installation this happens!
        	LOGGER.error("CELLAR FEATURE: retrieved event {} while groupManager is not available yet!", event);
        	return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR FEATURE: node is not part of the event cluster group");
            return;
        }

        // check if the event is not "local"
        if (event.getLocal() != null && event.getLocal().getId().equals(clusterManager.getNode().getId())) {
            LOGGER.trace("CELLAR FEATURE: event is local (coming from synchronizer or listener)");
            return;
        }

        String uri = event.getId();
        RepositoryEvent.EventType type = event.getType();
        try {
            if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                if (event.getRefresh() != null && event.getRefresh()) {
                    if (uri == null) {
                        Repository[] repositories = featuresService.listRepositories();
                        for (Repository repository : repositories) {
                            LOGGER.debug("CELLAR FEATURE: refresh repository {}", repository.getURI().toString());
                            featuresService.refreshRepository(repository.getURI());
                        }
                    } else {
                        LOGGER.debug("CELLAR FEATURE: refresh repository {}", uri);
                        featuresService.refreshRepository(new URI(uri));
                    }
                } else {
                    if (!isRepositoryRegisteredLocally(uri)) {
                        LOGGER.debug("CELLAR FEATURE: adding repository URI {}", uri);
                        featuresService.addRepository(new URI(uri), event.getInstall());
                    } else {
                        LOGGER.debug("CELLAR FEATURE: repository URI {} is already registered locally", uri);
                    }
                }
            } else {
                if (isRepositoryRegisteredLocally(uri)) {
                    LOGGER.debug("CELLAR FEATURE: removing repository URI {}", uri);
                    featuresService.removeRepository(new URI(uri), event.getUninstall());
                } else {
                    LOGGER.debug("CELLAR FEATURE: repository URI {} is not registered locally");
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR FEATURE: failed to add/remove repository URL {}", uri, e);
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
