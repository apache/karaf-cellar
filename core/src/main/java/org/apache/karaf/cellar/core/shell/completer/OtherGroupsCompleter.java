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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;

/**
 * Shell completer on the cluster groups which are not local.
 */
public class OtherGroupsCompleter extends GroupCompleterSupport {

    /**
     * Check a cluster group for completion.
     * Only non local cluster groups will be accepted.
     *
     * @param group the cluster group to check.
     * @return true if the cluster group is not local, false else.
     */
    @Override
    protected boolean acceptsGroup(Group group) {
        Node node = groupManager.getNode();
        if (group.getNodes().contains(node))
            return false;
        else return true;
    }

}
