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
package org.apache.karaf.cellar.core.event;

import org.apache.karaf.cellar.core.Handler;

/**
 * Event handler interface.
 */
public interface EventHandler<E extends Event> extends Handler<E> {

    public static String MANAGED_FILTER = "(managed=true)";

    /**
     * Handle a cluster {@code Event}.
     *
     * @param event the cluster event to handle.
     */
    public void handle(E event);

}
