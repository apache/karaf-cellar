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

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OBR URL Event handler.
 */
public class ObrUrlEventHandler extends ObrSupport implements EventHandler<ObrUrlEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrUrlEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.url";

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
     * Process an OBR URL event.
     *
     * @param obrUrlEvent the OBR URL Event.
     */
    @Override
    public void handle(ObrUrlEvent obrUrlEvent) {
        String url = obrUrlEvent.getUrl();
        String action = obrUrlEvent.getAction();
        try {
            if (isAllowed(obrUrlEvent.getSourceGroup(), Constants.OBR_URL_CATEGORY, url, EventType.INBOUND) || obrUrlEvent.getForce()) {
                if (action.equals("ADD")) {
                    LOGGER.debug("Received OBR ADD URL {} event with type {}", url, obrUrlEvent.getType());
                    obrService.addRepository(url);
                }
                if (action.equals("REMOVE")) {
                    LOGGER.debug("Received OBR REMOVE URL {} event with type {}", url, obrUrlEvent.getType());
                    obrService.removeRepository(url);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to register URL {}", url, e);
        }
    }

    public Class<ObrUrlEvent> getType() {
        return ObrUrlEvent.class;
    }

    public Switch getSwitch() {
        return this.eventSwitch;
    }

}
