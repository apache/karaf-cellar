/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.cellar.cloud;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Hashtable;

/**
 * @author: iocanel
 */
public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(Constants.SERVICE_PID, "org.apache.karaf.cellar.cloud");
        BlobStoreDiscoveryServiceFactory blobStoreDiscoveryServiceFactory = new BlobStoreDiscoveryServiceFactory(context);
        context.registerService(ManagedServiceFactory.class.getName(), blobStoreDiscoveryServiceFactory, properties);
    }

    public void stop(BundleContext context) throws Exception {
    }
}