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
 * Describe the operations and attributes on the Cellar Node MBean.
 */
public interface CellarNodeMBean {

    /**
     * Ping a node.
     *
     * @param nodeId the node ID.
     * @return the time (in milliseconds) to reach the node.
     * @throws Exception in case of ping failure.
     */
    long pingNode(String nodeId) throws Exception;

    /**
     * Get the list of nodes.
     *
     * @return the list of nodes.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getNodes() throws Exception;

}
