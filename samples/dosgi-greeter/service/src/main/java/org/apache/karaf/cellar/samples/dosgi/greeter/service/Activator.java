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
package org.apache.karaf.cellar.samples.dosgi.greeter.service;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.samples.dosgi.greeter.api.Greeter;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;

import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(Greeter.class)
        },
        requires = {
                @RequireService(ClusterManager.class)
        }
)
public class Activator extends BaseActivator {

        @Override
        public void doStart() throws Exception {

                ClusterManager clusterManager = getTrackedService(ClusterManager.class);
                if (clusterManager == null)
                        return;

                String nodeId = clusterManager.getNode().getId();
                GreeterImpl greeter = new GreeterImpl(nodeId);
                Hashtable props = new Hashtable();
                props.put("service.exported.interfaces", "*");
                register(Greeter.class, greeter, props);

        }

}
