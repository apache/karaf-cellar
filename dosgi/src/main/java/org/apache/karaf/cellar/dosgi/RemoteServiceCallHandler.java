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
package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Handler for cluster remote service call event.
 */
public class RemoteServiceCallHandler extends CellarSupport implements EventHandler<RemoteServiceCall> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.dosgi.switch";

    private static final transient Logger LOGGER = LoggerFactory.getLogger(RemoteServiceCallHandler.class);

    private final Switch dosgiSwitch = new BasicSwitch(SWITCH_ID);

    private BundleContext bundleContext;

    private EventTransportFactory eventTransportFactory;

    /**
     * Handle a cluster remote service call event.
     *
     * @param event the cluster event to handle.
     */
    @Override
    public void handle(RemoteServiceCall event) {

        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR DOSGI: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        Object targetService = null;

        if (event != null) {
            ServiceReference[] serviceReferences = null;
            try {
                serviceReferences = bundleContext.getServiceReferences(event.getServiceClass(), null);
                if (serviceReferences != null && serviceReferences.length > 0) {
                    targetService = bundleContext.getService(serviceReferences[0]);
                    bundleContext.ungetService(serviceReferences[0]);
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.error("CELLAR DOSGI: failed to lookup service", e);
            }

            if (targetService != null) {
                Class[] classes = new Class[0];
                if (event.getArguments() != null && event.getArguments().size() > 0) {
                    classes = new Class[event.getArguments().size()];
                    int i = 0;
                    for (Object obj : event.getArguments()) {
                        classes[i++] = obj.getClass();
                    }
                }

                try {
                    Method method;
                    if (classes.length > 0) {
                        method = targetService.getClass().getMethod(event.getMethod(), classes);
                    } else {
                        method = targetService.getClass().getMethod(event.getMethod());
                    }

                    Object obj = method.invoke(targetService, event.getArguments().toArray());
                    RemoteServiceResult result = new RemoteServiceResult(event.getId());
                    result.setResult(obj);

                    EventProducer producer = eventTransportFactory.getEventProducer(Constants.RESULT_PREFIX + Constants.SEPARATOR + event.getSourceNode().getId() + event.getEndpointId(), false);
                    producer.produce(result);

                } catch (NoSuchMethodException e) {
                    LOGGER.error("CELLAR DOSGI: unable to find remote method for service", e);
                } catch (InvocationTargetException e) {
                    LOGGER.error("CELLAR DOSGI: unable to invoke remote method for service", e);
                } catch (IllegalAccessException e) {
                    LOGGER.error("CELLAR DOSGI: unable to access remote method for service", e);
                }
            }
        }
    }

    /**
     * Get the event type that this handler can handle.
     *
     * @return the remote service call event type.
     */
    @Override
    public Class<RemoteServiceCall> getType() {
        return RemoteServiceCall.class;
    }

    /**
     * Get the handler switch.
     *
     * @return the handler switch.
     */
    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.HANDLER + "." + this.getClass().getName()));
                if (status) {
                    dosgiSwitch.turnOn();
                } else {
                    dosgiSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return dosgiSwitch;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public EventTransportFactory getEventTransportFactory() {
        return eventTransportFactory;
    }

    public void setEventTransportFactory(EventTransportFactory eventTransportFactory) {
        this.eventTransportFactory = eventTransportFactory;
    }

}
