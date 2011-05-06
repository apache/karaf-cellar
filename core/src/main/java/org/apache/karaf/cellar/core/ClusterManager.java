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

package org.apache.karaf.cellar.core;

import org.apache.karaf.cellar.core.event.EventProducer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author iocanel
 */
public interface ClusterManager {

    /**
     * Returns a named distributed map.
     *
     * @param mapName
     * @return
     */
    public Map getMap(String mapName);

    /**
     * Returns a named distributed list.
     *
     * @param listName
     * @return
     */
    public List getList(String listName);

    /**
     * Returns an Event Producer that produces {@link Event}s for a specific {@link Group}.
     *
     * @param groupName
     * @return
     */
    public EventProducer getEventProducer(String groupName);


    /**
     * Returns a list of {@code Node}s.
     *
     * @return
     */
    public Set<Node> listNodes();

    /**
     * Returns the {@link Node} with specified id.
     *
     * @return
     */
    public Set<Node> listNodes(Collection<String> ids);

    /**
     * Returns the {@link Node} with specified {@link Group}.
     *
     * @return
     */
    public Set<Node> listNodesByGroup(Group group);


    /**
     * Returns the {@link Node} with specified id.
     *
     * @return
     */
    public Node findNodeById(String id);


    /**
     * Returns the {@link Node} on which the command was executed.
     *
     * @return
     */
    public Node getNode();

    /**
     * Generate an Id unique across the cluster.
     *
     * @return
     */
    public String generateId();

    /**
     * Start the clustering support on the {@link Node}.
     */
    public void start();

    /**
     * Stops the node.
     */
    public void stop();

    /**
     * Restarts the clustering support on the {@link Node}.
     */
    public void restart();
}
