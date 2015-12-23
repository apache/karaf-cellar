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
package org.apache.karaf.cellar.dns.internal.osgi;

import org.apache.karaf.cellar.dns.DnsDiscoveryServiceFactory;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
    provides = {
        @ProvideService(ManagedServiceFactory.class)
    }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    @Override
    public void doStart() throws Exception {
        LOGGER.debug("CELLAR DNS: init discovery service factory");
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.cellar.dns");
        DnsDiscoveryServiceFactory factory = new DnsDiscoveryServiceFactory(bundleContext);
        register(ManagedServiceFactory.class, factory, props);
    }

    @Override
    public void doStop() {
        super.doStop();
    }
}
