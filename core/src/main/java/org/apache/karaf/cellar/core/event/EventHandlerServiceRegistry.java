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
package org.apache.karaf.cellar.core.event;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Event handler service registry.
 */
public class EventHandlerServiceRegistry<E extends Event> implements EventHandlerRegistry<E> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(EventHandlerRegistryDispatcher.class);

    private Map<Class,EventHandler> eventHandlerMap = new ConcurrentHashMap<Class,EventHandler>();

    /**
     * Returns the appropriate {@code EventHandler} found inside the {@code HandlerRegistry}.
     *
     * @param event
     * @return
     */
    public EventHandler<E> getHandler(E event) {
        if (event != null) {
            Class clazz = event.getClass();
            return eventHandlerMap.get(clazz);
        }
        return null;
    }

    public void bind(EventHandler handler) {
        if(handler != null && handler.getType() != null) {
            eventHandlerMap.put(handler.getType(),handler);
        }
    }

    public void unbind(EventHandler handler) {
         if(handler != null && handler.getType() != null) {
            eventHandlerMap.remove(handler.getType());
        }
    }

}
