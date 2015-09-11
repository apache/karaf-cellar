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
package org.apache.karaf.cellar.webconsole.internal.osgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.webconsole.CellarPlugin;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.Hashtable;

@Services(
        provides = {
                @ProvideService(Servlet.class)
        },
        requires = {
                @RequireService(ClusterManager.class),
                @RequireService(GroupManager.class)
        }
)
public class Activator extends BaseActivator {

    private final static Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private CellarPlugin plugin;

    @Override
    public void doStart() throws Exception {

        ClusterManager clusterManager = getTrackedService(ClusterManager.class);
        if (clusterManager == null)
            return;
        GroupManager groupManager = getTrackedService(GroupManager.class);
        if (groupManager == null)
            return;

        LOGGER.debug("CELLAR WEBCONSOLE: init plugin");
        plugin = new CellarPlugin();
        plugin.setClusterManager(clusterManager);
        plugin.setGroupManager(groupManager);
        plugin.setBundleContext(bundleContext);
        plugin.start();
        Hashtable props = new Hashtable();
        props.put("felix.webconsole.label", "cellar");
        register(Servlet.class, plugin, props);

    }

    @Override
    public void doStop() {
        super.doStop();

        if (plugin != null) {
            plugin.stop();
            plugin = null;
        }
    }

}
