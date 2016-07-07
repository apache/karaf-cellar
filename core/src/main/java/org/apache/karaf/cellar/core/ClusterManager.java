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
 * Cluster manager interface.
 */
public interface ClusterManager {

    /**
     * Get a map in the cluster.
     *
     * @param mapName the map name in the cluster.
     * @return the map in the cluster.
     */
    public Map getMap(String mapName);

    /**
     * Get a list in the cluster.
     *
     * @param listName the list name in the cluster.
     * @return the list in the cluster.
     */
    public List getList(String listName);

    /**
     * Get a set in the cluster.
     *
     * @param setName the set name in the cluster.
     * @return the set in the cluster.
     */
    public Set getSet(String setName);

    /**
     * Get the nodes in the cluster.
     *
     * @return the set of nodes in the cluster.
     */
    public Set<Node> listNodes();

    /**
     * Get the nodes with a given ID.
     *
     * @param ids the collection of ID to look for.
     * @return the set of nodes.
     */
    public Set<Node> listNodes(Collection<String> ids);

    /**
     * Get the nodes in a given cluster group.
     *
     * @param group the cluster group.
     * @return the set of nodes in the cluster group.
     */
    public Set<Node> listNodesByGroup(Group group);

    /**
     * Get a node identified by a given ID.
     *
     * @param id the id of the node to look for.
     * @return the node.
     */
    public Node findNodeById(String id);

    /**
     * Get a node identified by a given alias.
     *
     * @param alias the alias of the node to look for.
     * @return the node.
     */
    public Node findNodeByAlias(String alias);

    /**
     * Get a node identified by a given ID or alias.
     *
     * @param idOrAlias the ID or alias of the node to look for.
     * @return the node.
     */
    public Node findNodeByIdOrAlias(String idOrAlias);

    /**
     * Get the local node.
     *
     * @return the local node.
     */
    public Node getNode();

    /**
     * Set an alias for the local node.
     *
     * @param alias The node alias.
     */
    public void setNodeAlias(String alias);

    /**
     * Generate an unique ID across the cluster.
     *
     * @return a unique ID across the cluster.
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

}
