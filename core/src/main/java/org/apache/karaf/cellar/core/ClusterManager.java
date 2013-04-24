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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Description of a cluster manager.
 */
public interface ClusterManager {

    /**
     * Get a map from the cluster.
     *
     * @param mapName the map name.
     * @return the map.
     */
    public Map getMap(String mapName);

    /**
     * Get a list from the cluster.
     *
     * @param listName the list name.
     * @return the list.
     */
    public List getList(String listName);

    /**
     * Get a set from the cluster.
     *
     * @param setName the set name.
     * @return the set.
     */
    public Set getSet(String setName);

    /**
     * Get the nodes in the cluster.
     *
     * @return a set of nodes in the cluster.
     */
    public Set<Node> listNodes();

    /**
     * Get the nodes with given IDs.
     *
     * @param ids the node IDs.
     * @return a set of nodes in the cluster matching the given IDs.
     */
    public Set<Node> listNodes(Collection<String> ids);

    /**
     * Get the nodes member of a given cluster group.
     *
     * @param group the cluster group.
     * @return a set of nodes member of the cluster group.
     */
    public Set<Node> listNodesByGroup(Group group);

    /**
     * Get a node with a given ID.
     *
     * @param id the node ID.
     * @return the node.
     */
    public Node findNodeById(String id);

    /**
     * Get the local node.
     *
     * @return the local node.
     */
    public Node getNode();

    /**
     * Generate an unique ID across the cluster.
     *
     * @return the generated unique ID.
     */
    public String generateId();

    /**
     * Start the local node.
     */
    public void start();

    /**
     * Stop the local node.
     */
    public void stop();

    /**
     * Restart the local node.
     */
    public void restart();

}
