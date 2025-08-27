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

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.ops4j.pax.web.service.spi.model.events.ServletEventData;
import org.ops4j.pax.web.service.spi.model.events.WebElementEvent;
import org.ops4j.pax.web.service.spi.model.events.WebElementEventListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.util.*;

/**
 * Listen local node servlet event, in order to update the cluster servlet map
 * and send a cluster event to the other nodes
 */
public class LocalServletListener implements WebElementEventListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(LocalServletListener.class);

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private ConfigurationAdmin configurationAdmin;
    private EventProducer eventProducer;

    @Override
    public synchronized void registrationEvent(WebElementEvent event) {

        // only listen for servlet event
        if (!(event.getData() instanceof ServletEventData)) {
            return;
        }

        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.warn("CELLAR HTTP BALANCER: cluster event producer is OFF");
            return;
        }

        ServletEventData data = (ServletEventData) event.getData();
        Servlet servlet = data.getServlet();

        if (servlet != null && servlet.getClass().getName().equals(CellarBalancerProxyServlet.class.getName())) {
            LOGGER.trace("CELLAR HTTP BALANCER: ignoring CellarBalancerProxyServlet servlet event");
            return;
        }

        Set<Group> localGroups = groupManager.listLocalGroups();

        BalancedServletUtil util = new BalancedServletUtil();
        util.setClusterManager(clusterManager);
        util.setConfigurationAdmin(configurationAdmin);
        String alias = data.getAlias();
        String location = util.constructLocation(alias);

        for (Group group : localGroups) {
            Map<String, List<String>> clusterServlets = clusterManager.getMap(Constants.BALANCER_MAP + Configurations.SEPARATOR + group.getName());

            if (event.getType() == WebElementEvent.State.DEPLOYED) {
                // update the cluster servlets
                List<String> locations = clusterServlets.get(alias);
                if (locations == null) {
                    locations = new ArrayList<String>();
                }

                if (!locations.contains(location)) {
                    LOGGER.debug("CELLAR HTTP BALANCER: adding location {} to servlet {} on cluster", location, alias);
                    locations.add(location);
                    clusterServlets.put(alias, locations);
                    // send cluster event
                    ClusterBalancerEvent cellarEvent = new ClusterBalancerEvent(alias, ClusterBalancerEvent.ADDING, locations);
                    cellarEvent.setSourceGroup(group);
                    cellarEvent.setSourceNode(clusterManager.getNode());
                    cellarEvent.setLocal(clusterManager.getNode());
                    eventProducer.produce(cellarEvent);
                } else {
                    LOGGER.debug("CELLAR HTTP BALANCER: location {} already defined for servlet {} on cluster", location, alias);
                }
            } else if (event.getType() == WebElementEvent.State.UNDEPLOYED) {
                List<String> locations = clusterServlets.get(alias);
                if (locations == null)
                    locations = new ArrayList<String>();
                if (locations.contains(location)) {
                    LOGGER.debug("CELLAR HTTP BALANCER: removing location {} for servlet {} on cluster", location, alias);
                    locations.remove(location);
                    // update cluster state
                    clusterServlets.put(alias, locations);
                    // send cluster event
                    ClusterBalancerEvent cellarEvent = new ClusterBalancerEvent(alias, ClusterBalancerEvent.REMOVING, locations);
                    cellarEvent.setSourceGroup(group);
                    cellarEvent.setSourceNode(clusterManager.getNode());
                    cellarEvent.setLocal(clusterManager.getNode());
                    eventProducer.produce(cellarEvent);
                }
                if (locations.isEmpty()) {
                    LOGGER.debug("CELLAR HTTP BALANCER: destroying servlet {} from cluster", alias);
                    // update the cluster servlets
                    clusterServlets.remove(alias);
                }
            }
        }
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

}
