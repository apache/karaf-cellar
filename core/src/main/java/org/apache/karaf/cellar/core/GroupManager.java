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
 * Generic group manager interface.
 */
public interface GroupManager {

    /**
     * Returns the local {@link Node}.
     *
     * @return
     */
    public Node getNode();

    /**
     * Creates {@link Group}
     *
     * @param groupName
     * @return
     */
    public Group createGroup(String groupName);

    /**
     * Deletes {@link Group}
     *
     * @param groupName
     */
    public void deleteGroup(String groupName);

    /**
     * Return the {@link Group} by name.
     *
     * @param groupName
     * @return
     */
    public Group findGroupByName(String groupName);

    /**
     * Returns a map of {@link Node}s.
     *
     * @return
     */
    public Map<String, Group> listGroups();

    /**
     * Returns the {@link Group}s of the specified {@link Node}.
     *
     * @return
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
     * Returns the {@link Group}s of the specified {@link Node}.
     *
     * @return
     */
    public Set<Group> listAllGroups();


    /**
     * Returns the {@link Group}s of the specified {@link Node}.
     *
     * @param node
     * @return
     */
    public Set<Group> listGroups(Node node);

    /**
     * Retrurns the {@link Group} names of the current {@Node}.
     *
     * @return
     */
    public Set<String> listGroupNames();

    /**
     * Returns the {@link Group} names of the specified {@link Node}.
     *
     * @param node
     * @return
     */
    public Set<String> listGroupNames(Node node);


    /**
     * Registers current {@link Node} to the {@link Group}.
     *
     * @param group
     */
    public void registerGroup(Group group);

    /**
     * Registers current {@link Node} to the {@link Group}.
     *
     * @param groupName
     */
    public void registerGroup(String groupName);


    /**
     * UnRegisters current {@link Node} to the {@link Group}.
     *
     * @param group
     */
    public void unRegisterGroup(Group group);

    /**
     * UnRegisters current {@link Node} to the {@link Group}.
     *
     * @param groupName
     */
    public void unRegisterGroup(String groupName);

}
