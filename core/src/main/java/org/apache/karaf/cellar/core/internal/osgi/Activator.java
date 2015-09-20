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
package org.apache.karaf.cellar.core.internal.osgi;

import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventHandlerRegistry;
import org.apache.karaf.cellar.core.event.EventHandlerServiceRegistry;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Services(
        provides = { @ProvideService(EventHandlerRegistry.class) }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ServiceTracker<EventHandler, EventHandler> eventHandlerServiceTracker;

    @Override
    public void doStart() throws Exception {

        LOGGER.debug("CELLAR CORE: register event handler service registry");
        final EventHandlerServiceRegistry registry = new EventHandlerServiceRegistry();
        register(EventHandlerRegistry.class, registry);

        LOGGER.debug("CELLAR CORE: starting event handler service tracker");
        eventHandlerServiceTracker = new ServiceTracker<EventHandler, EventHandler>(bundleContext, EventHandler.class, new ServiceTrackerCustomizer<EventHandler, EventHandler>() {
            @Override
            public EventHandler addingService(ServiceReference<EventHandler> serviceReference) {
                EventHandler eventHandler = bundleContext.getService(serviceReference);
                registry.bind(eventHandler);
                return eventHandler;
            }

            @Override
            public void modifiedService(ServiceReference<EventHandler> serviceReference, EventHandler eventHandler) {
                // nothing to do
            }

            @Override
            public void removedService(ServiceReference<EventHandler> serviceReference, EventHandler eventHandler) {
                registry.unbind(eventHandler);
                bundleContext.ungetService(serviceReference);
            }
        });
        eventHandlerServiceTracker.open();
    }

    @Override
    public void doStop() {
        super.doStop();

        if (eventHandlerServiceTracker != null) {
            eventHandlerServiceTracker.close();
            eventHandlerServiceTracker = null;
        }
    }

}
