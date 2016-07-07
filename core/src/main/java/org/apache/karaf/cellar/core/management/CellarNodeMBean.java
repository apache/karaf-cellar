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
package org.apache.karaf.cellar.core.management;

import javax.management.openmbean.TabularData;

/**
 * Describe the operations and attributes on the Cellar Node MBean.
 */
public interface CellarNodeMBean {

    /**
     * Ping a node.
     *
     * @param nodeIdOrAlias the node ID or alias.
     * @return the time (in milliseconds) to reach the node.
     * @throws Exception in case of ping failure.
     */
    long pingNode(String nodeIdOrAlias) throws Exception;

    /**
     * Set the alias of the local node.
     *
     * @param alias The node alias.
     * @throws Exception in case of failure.
     */
    void setAlias(String alias) throws Exception;

    /**
     * Get the alias for a given node ID.
     *
     * @param id the node ID or null for the local node.
     * @return the corresponding alias (or null).
     * @throws Exception in case of failure.
     */
    String getAlias(String id) throws Exception;

    /**
     * Get the node ID for a given alias.
     * @param alias The node alias.
     * @return The node ID.
     * @throws Exception in case of failure.
     */
    String getId(String alias) throws Exception;

    /**
     * Get the list of nodes.
     *
     * @return the list of nodes.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getNodes() throws Exception;

}
