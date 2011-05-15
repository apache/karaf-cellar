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

/**
 * Event handler service registry.
 */
public class EventHandlerServiceRegistry<E extends Event> implements EventHandlerRegistry<E> {

    private static final Logger logger = LoggerFactory.getLogger(EventHandlerRegistryDispatcher.class);

    private BundleContext bundleContext;

    /**
     * Returns the appropriate {@code EventHandler} found inside the {@code HandlerRegistry}.
     *
     * @param event
     * @return
     */
    public EventHandler<E> getHandler(E event) {

        ServiceReference[] references = new ServiceReference[0];
        try {
            references = bundleContext.getServiceReferences("org.apache.karaf.cellar.core.event.EventHandler", null);
            if (references != null && references.length > 0) {
                for (int i = 0; i < references.length; i++) {
                    ServiceReference ref = references[i];
                    try {
                        EventHandler handler = (EventHandler) bundleContext.getService(ref);
                        if (handler.getType().equals(event.getClass())) {
                            return handler;
                        }
                    } catch (Exception ex) {
                        logger.error("Failed to get handler from Service Reference.", ex);
                    } finally {
                        bundleContext.ungetService(ref);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Failed to lookup Service Registry for Event Hanlders.", e);
        }
        return null;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
