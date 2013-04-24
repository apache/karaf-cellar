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
import org.apache.karaf.cellar.core.HandlerRegistry;

/**
 * Description of a cluster event handlers registry.
 */
public interface EventHandlerRegistry<E extends Event> extends HandlerRegistry<E, Handler<E>> {

    /**
     * Get the handler which is able to handle a given cluster event.
     *
     * @param event the cluster event to handle.
     * @return the handler which is able to handle the cluster event.
     */
    @Override
    public EventHandler<E> getHandler(E event);

}
