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

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for remote service.
 */
public class RemoteServiceFactory implements ServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteServiceFactory.class);

    private final ClusterManager clusterManager;
    private final ExecutionContext executionContext;
    private final EndpointDescription endpointDescription;

    public RemoteServiceFactory(ClusterManager clusterManager, ExecutionContext executionContext, EndpointDescription endpointDescription) {
        this.clusterManager = clusterManager;
        this.executionContext = executionContext;
        this.endpointDescription = endpointDescription;
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        ClassLoader classLoader = new RemoteServiceProxyClassLoader(bundle);
        List<Class> interfaces = new ArrayList();
        String endpointId = endpointDescription.getId();
        String filter = endpointDescription.getFilter();
        String version = endpointDescription.getVersion();
        String serviceClass = endpointDescription.getServiceClass();
        try {
            interfaces.add(classLoader.loadClass(serviceClass));
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        LOGGER.trace("CELLAR DOSGI: Creating remote service invocation handler for service {} with filter {} having endpoint Id", serviceClass, filter, endpointId);
        RemoteServiceInvocationHandler handler = new RemoteServiceInvocationHandler(endpointId, filter, version, serviceClass, clusterManager, executionContext);
        return Proxy.newProxyInstance(classLoader, interfaces.toArray(new Class[interfaces.size()]), handler);
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        // nothing to do
    }

}
