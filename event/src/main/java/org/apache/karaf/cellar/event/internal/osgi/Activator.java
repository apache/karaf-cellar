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
package org.apache.karaf.cellar.event.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.event.ClusterEventHandler;
import org.apache.karaf.cellar.event.LocalEventListener;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(EventHandler.class),
                @ProvideService(org.osgi.service.event.EventHandler.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class),
                @RequireService(ConfigurationAdmin.class),
                @RequireService(EventAdmin.class),
                @RequireService(EventProducer.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private LocalEventListener localEventListener;
    private ClusterEventHandler clusterEventHandler;

    @Override
    public void doStart() throws Exception {

        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null)
            return;
        GroupManager groupManager = getTrackedService(GroupManager.class);
        if (groupManager == null)
            return;
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null)
            return;
        EventAdmin eventAdmin = getTrackedService(EventAdmin.class);
        if (eventAdmin == null)
            return;
        EventProducer eventProducer = getTrackedService(EventProducer.class);
        if (eventProducer == null)
            return;

        LOGGER.debug("CELLAR EVENT: init event handler");
        clusterEventHandler = new ClusterEventHandler();
        clusterEventHandler.setConfigurationAdmin(configurationAdmin);
        clusterEventHandler.setGroupManager(groupManager);
        clusterEventHandler.setClusterManager(clusterManager);
        clusterEventHandler.setEventAdmin(eventAdmin);
        clusterEventHandler.init();
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(EventHandler.class, clusterEventHandler, props);

        LOGGER.debug("CELLAR EVENT: init local event listener");
        localEventListener = new LocalEventListener();
        localEventListener.setClusterManager(clusterManager);
        localEventListener.setGroupManager(groupManager);
        localEventListener.setConfigurationAdmin(configurationAdmin);
        localEventListener.setEventProducer(eventProducer);
        localEventListener.init();
        props = new Hashtable();
        props.put("event.topics", "*");
        register(org.osgi.service.event.EventHandler.class, localEventListener, props);
    }

    @Override
    public void doStop() {
        super.doStop();

        if (localEventListener != null) {
            localEventListener.destroy();
            localEventListener = null;
        }
        if (clusterEventHandler != null) {
            clusterEventHandler.destroy();
            clusterEventHandler = null;
        }
    }

}
