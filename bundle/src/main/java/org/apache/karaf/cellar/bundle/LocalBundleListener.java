/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.karaf.cellar.bundle;


import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * @author iocanel
 */
public class LocalBundleListener extends BundleSupport implements BundleListener {

    private static final Logger logger = LoggerFactory.getLogger(LocalBundleListener.class);

    private List<EventProducer> producerList;

    private Node node;

    /**
     * Process {@link BundleEvent}s.
     *
     * @param event
     */
    public void bundleChanged(BundleEvent event) {
        if (event != null && event.getBundle() != null) {
            Set<Group> groups = groupManager.listLocalGroups();

            if (groups != null && !groups.isEmpty()) {
                for (Group group : groups) {

                    String symbolicName = event.getBundle().getSymbolicName();
                    String version = event.getBundle().getVersion().toString();
                    String bundleLocation = event.getBundle().getLocation();
                    int type = event.getType();
                    if (isAllowed(group, Constants.CATEGORY, bundleLocation, EventType.OUTBOUND)) {
                        RemoteBundleEvent remoteBundleEvent = new RemoteBundleEvent(symbolicName, version, bundleLocation, type);
                        remoteBundleEvent.setSourceGroup(group);
                        remoteBundleEvent.setSourceNode(node);
                        if (producerList != null && !producerList.isEmpty()) {
                            for (EventProducer producer : producerList) {
                                producer.produce(remoteBundleEvent);
                            }
                        }
                    } else logger.debug("Bundle with symbolicName {} is marked as BLOCKED OUTBOUND");
                }
            }
        }
    }

    /**
     * Initialization Method.
     */
    public void init() {
        if (clusterManager != null) {
            node = clusterManager.getNode();
        }
    }

    /**
     * Destruction Method.
     */
    public void destroy() {

    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }

}
