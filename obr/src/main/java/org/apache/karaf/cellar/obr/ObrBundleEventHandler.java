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
 *  Bundles event handler.
 */
public class ObrBundleEventHandler extends ObrSupport implements EventHandler<ObrBundleEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrBundleEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.bundle";

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
     * Process an OBR bundle event.
     *
     * @param event the OBR bundle event.
     */
    @Override
    public void handle(ObrBundleEvent event) {
        String bundleId = event.getBundleId();
        if (isAllowed(event.getSourceGroup(), Constants.OBR_BUNDLE_CATEGORY, bundleId, EventType.INBOUND)) {
            LOGGER.debug("Received OBR bundle event {}", bundleId);
            EventType eventType = event.getEventType();
            System.out.println("OBR event received.");
            System.out.println("Bundle ID: " + bundleId);
            System.out.println("Type: " + eventType);
        } else LOGGER.debug("OBR bundle event {} is marked as BLOCKED INBOUND", bundleId);
    }

    public Class<ObrBundleEvent> getType() {
        return ObrBundleEvent.class;
    }

    public Switch getSwitch() {
        return this.eventSwitch;
    }

}
