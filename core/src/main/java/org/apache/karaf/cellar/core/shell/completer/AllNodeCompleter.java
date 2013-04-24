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
package org.apache.karaf.cellar.core.shell.completer;

import org.apache.karaf.cellar.core.Node;

/**
 * Shell completer for all nodes.
 */
public class AllNodeCompleter extends NodeCompleterSupport {

    /**
     * Check if the node should be accepted for completion.
     *
     * @param node the node to check.
     * @return always return true as we accept all nodes.
     */
    @Override
    protected boolean acceptsNode(Node node) {
        return true;
    }

}
