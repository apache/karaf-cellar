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
package org.apache.karaf.cellar.kar.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.kar.KarEventHandler;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

@Services(
    provides = {
        @ProvideService(EventHandler.class)
    },
    requires = {
        @RequireService(ClusterManager.class),
        @RequireService(GroupManager.class),
        @RequireService(EventProducer.class),
        @RequireService(ConfigurationAdmin.class),
        @RequireService(KarService.class)
    }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private KarEventHandler karEventHandler;

    @Override
    public void doStart() throws Exception {

        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null)
            return;
        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null)
            return;
        GroupManager groupManager = getTrackedService(GroupManager.class);
        if (groupManager == null)
            return;
        EventProducer eventProducer = getTrackedService(EventProducer.class);
        if (eventProducer == null)
            return;
        KarService karService = getTrackedService(KarService.class);
        if (karService == null)
            return;

        LOGGER.debug("CELLAR KAR: init event handler");
        karEventHandler = new KarEventHandler();
        karEventHandler.setConfigurationAdmin(configurationAdmin);
        karEventHandler.setClusterManager(clusterManager);
        karEventHandler.setGroupManager(groupManager);
        karEventHandler.setKarService(karService);
        Hashtable props = new Hashtable();
        props.put("managed", "true");
        register(new Class[]{ EventHandler.class }, karEventHandler, props);

    }

}
