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

import java.util.concurrent.ConcurrentMap;

/**
 * Command store interface.
 */
public interface CommandStore {

    /**
     * Get the list of pending commands from the store.
     *
     * @return the map of pending commands in the store.
     */
    public ConcurrentMap<String, Command> getPending();

    /**
     * Set the list of pending commands in the store.
     *
     * @param pending the map of pending commands in the store.
     */
    public void setPending(ConcurrentMap<String, Command> pending);

}
