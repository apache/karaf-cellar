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

/**
 * Description of a synchronizer.
 */
public interface Synchronizer {

    /**
     * Push local resources states to a cluster group.
     *
     * @param group the cluster group where to push the resources states.
     */
    public void push(Group group);

    /**
     * Pull the resources states from a cluster group, and update the local resources states.
     *
     * @param group the cluster group where to get the resources states.
     */
    public void pull(Group group);

    /**
     * Sync the node and the cluster, depending of the sync policy.
     *
     * @param group the target cluster group.
     */
    public void sync(Group group);

    /**
     * Get the sync policy for a given cluster group.
     *
     * @param group the cluster group.
     * @return the current sync policy for the given cluster group.
     */
    public String getSyncPolicy(Group group);

}
