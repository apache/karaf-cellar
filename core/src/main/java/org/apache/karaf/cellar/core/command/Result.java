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
package org.apache.karaf.cellar.core.command;

import org.apache.karaf.cellar.core.event.Event;

/**
 * Cluster command execution result event.
 */
public class Result extends Event {

    public Result(String id) {
        super(id);
        this.force = true;
    }

    /**
     * Get the force flag in the cluster result event.
     *
     * @return true if the force flag is set on the cluster result event, false else.
     */
    @Override
    public Boolean getForce() {
        return true;
    }

}
