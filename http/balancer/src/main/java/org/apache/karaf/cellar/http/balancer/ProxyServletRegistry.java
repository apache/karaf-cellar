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
package org.apache.karaf.cellar.http.balancer;

import org.osgi.framework.ServiceRegistration;

import java.util.HashMap;
import java.util.Map;

public class ProxyServletRegistry {

    private Map<String, ServiceRegistration> proxyRegistrations;

    public void register(String alias, ServiceRegistration registration) {
        proxyRegistrations.put(alias, registration);
    }

    public void unregister(String alias) {
        ServiceRegistration registration = proxyRegistrations.remove(alias);
        if (registration != null) {
            registration.unregister();
        }
    }

    public boolean contain(String alias) {
        return proxyRegistrations.containsKey(alias);
    }

    public void init() {
        proxyRegistrations = new HashMap<String, ServiceRegistration>();
    }

    public void destroy() {
        for (ServiceRegistration registration : proxyRegistrations.values()) {
            registration.unregister();
        }
        proxyRegistrations = new HashMap<String, ServiceRegistration>();
    }

}
