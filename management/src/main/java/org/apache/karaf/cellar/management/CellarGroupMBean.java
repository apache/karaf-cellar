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
package org.apache.karaf.cellar.management;

import javax.management.openmbean.TabularData;

/**
 * Describe the operations and attributes of the Cellar Cluster Group MBean.
 */
public interface CellarGroupMBean {

    /**
     * Create a cluster group.
     *
     * @param name the cluster group name.
     * @throws Exception in case of create failure.
     */
    void create(String name) throws Exception;

    /**
     * Delete a cluster group.
     *
     * @param name the cluster group name.
     * @throws Exception in case of delete failure.
     */
    void delete(String name) throws Exception;

    /**
     * Join a node in a cluster group.
     *
     * @param name the cluster group name.
     * @param nodeId the node ID.
     * @throws Exception in case of join failure.
     */
    void join(String name, String nodeId) throws Exception;

    /**
     * Quit a node from a cluster group.
     *
     * @param name the cluster group name.
     * @param nodeId the node ID.
     * @throws Exception in case of quit failure.
     */
    void quit(String name, String nodeId) throws Exception;

    /**
     * Get the list of cluster groups.
     *
     * @return the list of cluster groups.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getGroups() throws Exception;

}
