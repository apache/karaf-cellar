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

import java.io.Serializable;
import java.util.Set;

/**
 * Multiple nodes container (group) interface.
 */
public interface MultiNode extends Serializable {

    /**
     * Set the nodes hosted in the multi-node container.
     *
     * @param nodes the set of nodes to store in this container.
     */
    public void setNodes(Set<Node> nodes);

    /**
     * Get the list of nodes in the multi-node container.
     *
     * @return the set of nodes in the multi-node container.
     */
    public Set<Node> getNodes();

}
