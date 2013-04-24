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

/**
 * Shell completer for cluster groups.
 */
public class AllGroupsCompleter extends GroupCompleterSupport {

    /**
     * Check if a cluster group should be accepted for completion.
     *
     * @param group the cluster group.
     * @return always return true as we want a completion for all cluster groups.
     */
    @Override
    protected boolean acceptsGroup(Group group) {
        return true;
    }

}
