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

import org.apache.commons.lang3.ClassUtils;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.core.exception.RemoteServiceInvocationException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Handler for cluster remote service call event.
 */
public class RemoteServiceCallHandler extends CellarSupport implements EventHandler<RemoteServiceCall> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.dosgi.switch";

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteServiceCallHandler.class);

    private final Switch dosgiSwitch = new BasicSwitch(SWITCH_ID);

    private BundleContext bundleContext;

    private EventTransportFactory eventTransportFactory;

    /**
     * Handle a cluster remote service call event.
     *
     * @param remoteServiceCall the cluster event to handle.
     */
    @Override
    public void handle(RemoteServiceCall remoteServiceCall) {

        // check if the handler switch is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR DOSGI: {} switch is OFF, cluster event is not handled", SWITCH_ID);
            return;
        }

        long dosgiBundleId = bundleContext.getBundle().getBundleId();

        Object targetService = null;

        if (remoteServiceCall != null) {
            ServiceReference[] serviceReferences;
            try {
                // get all service references and versions for this class and filter
                serviceReferences = bundleContext.getAllServiceReferences(remoteServiceCall.getServiceClass(), remoteServiceCall.getFilter());
                LOGGER.trace("CELLAR DOSGI: handling remote call for service class {}", remoteServiceCall.getServiceClass());
                if (serviceReferences != null) {
                    Version versionMin = remoteServiceCall.getVersion() == null ? Version.emptyVersion : new Version(remoteServiceCall.getVersion());
                    Version versionMax = new Version(versionMin.getMajor() + 1, 0, 0);
                    VersionRange versionRange = new VersionRange(VersionRange.LEFT_CLOSED, versionMin, versionMax, VersionRange.RIGHT_OPEN);
                    for (ServiceReference serviceReference : serviceReferences) {
                        // avoid remote DOSGi registered services
                        if (serviceReference.getBundle().getBundleId() != dosgiBundleId) {
                            // match version range
                            if (versionRange.includes(serviceReference.getBundle().getVersion())) {
                                LOGGER.trace("CELLAR DOSGI: found local provider {} for service class {} for version {} and filter {}",
                                        serviceReference.getBundle().getBundleId(), remoteServiceCall.getServiceClass(), versionRange, remoteServiceCall.getFilter());
                                targetService = bundleContext.getService(serviceReference);
                                bundleContext.ungetService(serviceReference);
                                break;
                            }
                        }
                    }
                }
            } catch (InvalidSyntaxException e) {
                LOGGER.error("CELLAR DOSGI: failed to lookup service", e);
            }

            if (targetService != null) {
                Class[] classes = new Class[0];
                if (remoteServiceCall.getArguments() != null && remoteServiceCall.getArguments().size() > 0) {
                    classes = new Class[remoteServiceCall.getArguments().size()];
                    int i = 0;
                    for (Object obj : remoteServiceCall.getArguments()) {
                        classes[i++] = obj.getClass();
                    }
                }

                RemoteServiceResult result = new RemoteServiceResult(remoteServiceCall.getId());
                EventProducer producer = eventTransportFactory.getEventProducer(Constants.RESULT_PREFIX + Constants.SEPARATOR + remoteServiceCall.getSourceNode().getId() + remoteServiceCall.getEndpointId(), false);
                try {
                    Method method = getMethod(classes, targetService, remoteServiceCall);
                    Object obj = method.invoke(targetService, remoteServiceCall.getArguments().toArray());
                    result.setResult(obj);
                    producer.produce(result);

                } catch (NoSuchMethodException e) {
                    LOGGER.error("CELLAR DOSGI: unable to find remote method for service", e);
                    result.setResult(new RemoteServiceInvocationException(e));
                    producer.produce(result);
                } catch (InvocationTargetException e) {
                    LOGGER.error("CELLAR DOSGI: unable to invoke remote method for service", e);
                    result.setResult(new RemoteServiceInvocationException(e.getCause()));
                    producer.produce(result);
                } catch (IllegalAccessException e) {
                    LOGGER.error("CELLAR DOSGI: unable to access remote method for service", e);
                    result.setResult(new RemoteServiceInvocationException(e));
                    producer.produce(result);
                }
            }
        }
    }

    /**
     * <p>Gets a matching method in the <code>Object targetService<code/>.<br/>
     * Inheritance is supported.</p>
     *
     * @param eventParamTypes
     * @param targetService
     * @param remoteServiceCall
     * @return a method instance from the <code>Object targetService<code/>
     * @throws NoSuchMethodException
     */
    private Method getMethod(Class[] eventParamTypes, Object targetService, RemoteServiceCall remoteServiceCall) throws NoSuchMethodException {

        Method result = null;
        if (eventParamTypes.length > 0) {
            for (Method remoteMethod : targetService.getClass().getMethods()) {
                //need to find a method with a matching name and with the same number of parameters
                if (remoteMethod.getName().equals(remoteServiceCall.getMethod()) && remoteMethod.getParameterTypes().length == eventParamTypes.length) {
                    boolean allParamsFound = true;
                    for (int i = 0; i < remoteMethod.getParameterTypes().length; i++) {
                        allParamsFound = allParamsFound && ClassUtils.isAssignable(eventParamTypes[i], remoteMethod.getParameterTypes()[i]);
                    }

                    // if already found a matching method, no need to continue looking for one
                    if (allParamsFound) {
                        result = remoteMethod;
                        break;
                    }
                }
            }
        } else {
            result = targetService.getClass().getMethod(remoteServiceCall.getMethod());
        }

        //if method was not found go out with a bang
        if (result == null) {
            throw new NoSuchMethodException(String.format("No match for method [%s] %s", remoteServiceCall.getMethod(), Arrays.toString(eventParamTypes)));
        }

        return result;
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
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE, null);
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
