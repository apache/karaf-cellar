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
 * Multiple node container. A cluster group is an implementation of a multiple nodes container.
 */
public interface MultiNode extends Serializable {

    /**
     * Set the nodes in the multiple nodes container.
     *
     * @param nodes a set with the nodes in the container.
     */
    public void setNodes(Set<Node> nodes);

    /**
     * Get the nodes in the multiple nodes container.
     *
     * @return a set of the nodes in the container.
     */
    public Set<Node> getNodes();

}
