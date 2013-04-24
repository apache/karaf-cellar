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

import java.util.Map;
import java.util.Set;

/**
 * Description of a cluster group manager.
 */
public interface GroupManager {

    /**
     * Get the local node.
     *
     * @return the local node.
     */
    public Node getNode();

    /**
     * Create a new cluster group.
     *
     * @param groupName the new cluster group name.
     * @return the create cluster group.
     */
    public Group createGroup(String groupName);

    /**
     * Delete an existing cluster group.
     *
     * @param groupName the cluster group name to delete.
     */
    public void deleteGroup(String groupName);

    /**
     * Get a cluster group identified by a name.
     *
     * @param groupName the cluster group name to look for.
     * @return the cluster group.
     */
    public Group findGroupByName(String groupName);

    /**
     * Get the cluster groups.
     *
     * @return a map with cluster groups name and group.
     */
    public Map<String, Group> listGroups();

    /**
     * Get the list of "local" cluster groups.
     * A "local" cluster group is a group "hosting" the local node.
     *
     * @return a set of local cluster groups.
     */
    public Set<Group> listLocalGroups();

    /**
     * Check if the local node is part of the given group.
     *
     * @param groupName the group name.
     * @return true if the local node is part of the group, false else.
     */
    public boolean isLocalGroup(String groupName);

    /**
     * Get all cluster groups.
     *
     * @return a set containing all cluster groups.
     */
    public Set<Group> listAllGroups();

    /**
     * Get the cluster groups "hosting" a given node.
     *
     * @param node the node.
     * @return a set with cluster groups "hosting" a node.
     */
    public Set<Group> listGroups(Node node);

    /**
     * Get the cluster groups names of the local node.
     *
     * @return a set of cluster groups names.
     */
    public Set<String> listGroupNames();

    /**
     * Get the cluster groups names of a given node.
     *
     * @param node the node.
     * @return a set of cluster groups names.
     */
    public Set<String> listGroupNames(Node node);

    /**
     * Register the local node in a cluster group.
     *
     * @param group the cluster group where to register the local node.
     */
    public void registerGroup(Group group);

    /**
     * Register the local node in a cluster group.
     *
     * @param groupName the cluster group name where to register the local node.
     */
    public void registerGroup(String groupName);

    /**
     * Un-register the local node from a cluster group.
     *
     * @param group the cluster group.
     */
    public void unRegisterGroup(Group group);

    /**
     * Un-register the local node from a cluster group.
     *
     * @param groupName the cluster group name.
     */
    public void unRegisterGroup(String groupName);

}
