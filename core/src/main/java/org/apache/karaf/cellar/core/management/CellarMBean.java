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
 * Describe the operations and attributes of the Cellar core MBean.
 */
public interface CellarMBean {

    /**
     * Shutdown the cluster on all nodes, removing the cellar feature.
     */
    void shutdown() throws Exception;

    /**
     * Shutdown the cluster on all nodes, optionally stopping the target instances.
     * @param poweroff true to poweroff the nodes, false else.
     */
    void shutdown(boolean poweroff) throws Exception;

    /**
     * Force the sync of the different nodes in the cluster.
     *
     * @throws Exception in case of sync failure.
     */
    void sync() throws Exception;

    /**
     * Get the status of the cluster event consumers.
     *
     * @return the status of the cluster event consumers.
     * @throws Exception in case of retrieval failure.
     */
    TabularData consumerStatus() throws Exception;

    /**
     * Start a cluster event consumer on a node.
     *
     * @param nodeIdOrAlias the node ID or alias.
     * @throws Exception in case of start failure.
     */
    void consumerStart(String nodeIdOrAlias) throws Exception;

    /**
     * Stop a cluster event consumer on a node.
     *
     * @param nodeIdOrAlias the node ID or alias.
     * @throws Exception in case of stop failure.
     */
    void consumerStop(String nodeIdOrAlias) throws Exception;

    /**
     * Get the status of the cluster event handlers.
     *
     * @return the status of the cluster event handlers.
     * @throws Exception in case of retrieval failure.
     */
    TabularData handlerStatus() throws Exception;

    /**
     * Start a cluster event handler on a node.
     *
     * @param handlerId the cluster event handler ID.
     * @param nodeIdOrAlias the node ID or alias.
     * @throws Exception in case of start failure.
     */
    void handlerStart(String handlerId, String nodeIdOrAlias) throws Exception;

    /**
     * Stop a cluster event handler on a node.
     *
     * @param handlerId the cluster event handler ID.
     * @param nodeIdOrAlias the node ID or alias.
     * @throws Exception in case of stop failure.
     */
    void handlerStop(String handlerId, String nodeIdOrAlias) throws Exception;

    /**
     * Get the status of the cluster event producers.
     *
     * @return the status of the cluster event producers.
     * @throws Exception in case of retrieval failure.
     */
    TabularData producerStatus() throws Exception;

    /**
     * Start a cluster event producer on a node.
     *
     * @param nodeIdOrAlias the node ID or alias.
     * @throws Exception in case of start failure.
     */
    void producerStart(String nodeIdOrAlias) throws Exception;

    /**
     * Stop a cluster event producer on a node.
     *
     * @param nodeIdOrAlias the node ID or alias.
     * @throws Exception in case of stop failure.
     */
    void producerStop(String nodeIdOrAlias) throws Exception;

}
