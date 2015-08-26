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
package org.apache.karaf.cellar.utils.ping.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.command.CommandStore;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.utils.ping.PingHandler;
import org.apache.karaf.cellar.utils.ping.PongHandler;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;

@Services(
        requires = { @RequireService(ClusterManager.class), @RequireService(CommandStore.class), @RequireService(EventProducer.class), @RequireService(ConfigurationAdmin.class)},
        provides = { @ProvideService(EventHandler.class)}
)
public class Activator extends BaseActivator {

    @Override
    public void doStart() throws Exception {
        // retrieving services
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        EventProducer eventProducer = getTrackedService(EventProducer.class);
        CommandStore commandStore = getTrackedService(CommandStore.class);

        // registering ping event handler
        PingHandler pingHandler = new PingHandler();
        pingHandler.setClusterManager(clusterManager);
        pingHandler.setConfigurationAdmin(configurationAdmin);
        pingHandler.setProducer(eventProducer);
        register(EventHandler.class, pingHandler);

        // registering pong event handler
        PongHandler pongHandler = new PongHandler();
        pongHandler.setCommandStore(commandStore);
        register(EventHandler.class, pongHandler);
    }

}
