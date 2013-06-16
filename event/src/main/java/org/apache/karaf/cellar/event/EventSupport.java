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

import org.apache.karaf.cellar.core.CellarSupport;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic Cellar OSGi event support.
 */
public class EventSupport extends CellarSupport {

    protected EventAdmin eventAdmin;

    /**
     * Read a local {@code Event} and create a map object out of it.
     *
     * @param event the local event to read.
     * @return the map
     */
    public Map<String, Serializable> getEventProperties(Event event) {
        String[] propertyNames = event.getPropertyNames();

        Map<String, Serializable> properties = new HashMap<String, Serializable>();

        for (String propertyName : propertyNames) {
            // event property (org.osgi.framework.ServiceEvent for instance) contains non serializable objects (like source or service reference)
            if (!propertyName.equals("event")) {
                Object property = event.getProperty(propertyName);
                if (property instanceof Serializable)
                    properties.put(propertyName, (Serializable) property);
            }
        }

        return properties;
    }

    /**
     * Read a local {@code Event} and check if a property exists.
     *
     * @param event the local event to read.
     * @param name  the property name to check.
     * @return true if the property exists in the event, false else.
     */

    public boolean hasEventProperty(Event event, String name) {
        String[] propertyNames = event.getPropertyNames();

        for (String propertyName : propertyNames) {
            if (propertyName.equals(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Post events via {@link EventAdmin}.
     *
     * @param topicName  the topic name.
     * @param properties the event properties.
     */
    public void postEvent(String topicName, Map<String, Serializable> properties) {
        if (topicName == null) {
            LOGGER.error("CELLAR EVENT: failed to post event");
            return;
        }

        final Event event = new Event(topicName, properties);
        eventAdmin.postEvent(event);
    }

    /**
     * Send events via {@link EventAdmin}.
     *
     * @param topicName  the topic name.
     * @param properties the event properties.
     */
    public void sendEvent(String topicName, Map<String, Serializable> properties) {
        if (topicName == null) {
            LOGGER.error("CELLAR EVENT: failed to send event");
            return;
        }

        final Event event = new Event(topicName, properties);
        eventAdmin.sendEvent(event);
    }

    public EventAdmin getEventAdmin() {
        return this.eventAdmin;
    }

    public void setEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

}
