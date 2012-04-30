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
package org.apache.karaf.cellar.event;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

public class RemoteEventHandler extends EventSupport implements EventHandler<RemoteEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RemoteEventHandler.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.handler";
    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    public void handle(RemoteEvent event) {

        // check if the handler is ON
        if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR EVENT: {} is OFF, cluster event not handled", SWITCH_ID);
            return;
        }

        // check if the group is a local one
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.warn("CELLAR EVENT: node is not part of the event cluster group");
            return;
        }

        try {
            if (isAllowed(event.getSourceGroup(), Constants.CATEGORY, event.getTopicName(), EventType.INBOUND)) {
                Map<String, Serializable> properties = event.getProperties();
                properties.put(Constants.EVENT_PROCESSED_KEY, Constants.EVENT_PROCESSED_VALUE);
                properties.put(Constants.EVENT_SOURCE_GROUP_KEY, event.getSourceGroup());
                properties.put(Constants.EVENT_SOURCE_NODE_KEY, event.getSourceNode());
                postEvent(event.getTopicName(), properties);
            } else LOGGER.warn("CELLAR EVENT: event {} is marked as BLOCKED INBOUND", event.getTopicName());
        } catch (Exception e) {
            LOGGER.error("CELLAR EVENT: failed to handle event", e);
        }
    }

    /**
     * Initialization method.
     */
    public void init() {

    }

    /**
     * Destroy method.
     */
    public void destroy() {

    }

    public Switch getSwitch() {
        return eventSwitch;
    }
    
    public Class<RemoteEvent> getType() {
        return RemoteEvent.class;
    }

}
