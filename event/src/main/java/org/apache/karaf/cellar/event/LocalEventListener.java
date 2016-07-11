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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class LocalEventListener extends EventSupport implements EventHandler {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(LocalEventListener.class);

    private EventProducer eventProducer;

    @Override
    public void handleEvent(Event event) {

        // ignore log entry event
        if (event.getTopic().startsWith("org/osgi/service/log/LogEntry"))
            return;

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR EVENT: cluster event producer is OFF");
            return;
        }

        try {
            if (event != null && event.getTopic() != null) {
                Set<Group> groups = null;
                try {
                    groups = groupManager.listLocalGroups();
                } catch (Exception e) {
                    LOGGER.warn("Failed to list local groups. Is Cellar uninstalling ?");
                    return;
                }

                // filter already processed events
                if (hasEventProperty(event, Constants.EVENT_PROCESSED_KEY)) {
                    if (event.getProperty(Constants.EVENT_PROCESSED_KEY).equals(Constants.EVENT_PROCESSED_VALUE)) {
                        LOGGER.debug("CELLAR EVENT: filtered out event {}", event.getTopic());
                        return;
                    }
                }

                if (groups != null && !groups.isEmpty()) {
                    for (Group group : groups) {
                        String topicName = event.getTopic();
                        Map<String, Serializable> properties = getEventProperties(event);
                        if (isAllowed(group, Constants.CATEGORY, topicName, EventType.OUTBOUND)) {
                            // broadcast the event
                            ClusterEvent clusterEvent = new ClusterEvent(topicName, properties);
                            clusterEvent.setSourceGroup(group);
                            clusterEvent.setSourceNode(clusterManager.getNode());
                            clusterEvent.setLocal(clusterManager.getNode());
                            eventProducer.produce(clusterEvent);
                        } else LOGGER.trace("CELLAR EVENT: event {} is marked as BLOCKED OUTBOUND for cluster group {}", topicName, group.getName());
                    }
                }
            }
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
     * Destruction method.
     */
    public void destroy() {

    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
