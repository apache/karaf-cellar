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

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for cluster OBR URL event.
 */
public class ObrUrlEventHandler extends ObrSupport implements EventHandler<ClusterObrUrlEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.urls.handler";

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
     * Handle a received cluster OBR URL event.
     *
     * @param clusterObrUrlEvent the cluster OBR URL event received.
     */
    @Override
    public void handle(ClusterObrUrlEvent clusterObrUrlEvent) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR OBR: {} switch is OFF", SWITCH_ID);
            return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(clusterObrUrlEvent.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR OBR: node is not part of the event cluster group {}", clusterObrUrlEvent.getSourceGroup().getName());
            return;
        }

        // check if it's not a "local" event
        if (clusterObrUrlEvent.getSourceNode() != null && clusterObrUrlEvent.getSourceNode().getId().equalsIgnoreCase(clusterManager.getNode().getId())) {
            LOGGER.trace("CELLAR OBR: cluster event is local (coming from local synchronizer or listener)");
            return;
        }

        String url = clusterObrUrlEvent.getUrl();
        String groupName = clusterObrUrlEvent.getSourceGroup().getName();
        try {
            if (isAllowed(clusterObrUrlEvent.getSourceGroup(), Constants.URLS_CONFIG_CATEGORY, url, EventType.INBOUND) || clusterObrUrlEvent.getForce()) {
                LOGGER.debug("CELLAR OBR: received OBR URL {}", url);
                if (clusterObrUrlEvent.getType() == Constants.URL_ADD_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: add OBR URL {}", url);
                    obrService.addRepository(url);
                }
                if (clusterObrUrlEvent.getType() == Constants.URL_REMOVE_EVENT_TYPE) {
                    LOGGER.debug("CELLAR OBR: remove OBR URL {}", url);
                    boolean removed = obrService.removeRepository(url);
                    if (!removed) {
                        LOGGER.warn("CELLAR OBR: the repository URL hasn't been removed from the OBR service");
                    }
                }
            } else
                LOGGER.debug("CELLAR OBR: repository URL {} is marked BLOCKED INBOUND for cluster group {}", url, groupName);
        } catch (Exception e) {
            LOGGER.error("CELLAR OBR: failed to register URL {}", url, e);
        }
    }

    @Override
    public Class<ClusterObrUrlEvent> getType() {
        return ClusterObrUrlEvent.class;
    }

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
        return this.eventSwitch;
    }

}
