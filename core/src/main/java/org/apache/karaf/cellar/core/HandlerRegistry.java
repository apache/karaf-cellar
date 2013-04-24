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

/**
 * Handler registry interface.
 */
public interface HandlerRegistry<T extends Serializable, H extends Handler<T>> {

    /**
     * Get the handler that can handle a given cluster event.
     *
     * @param obj the cluster event to handle.
     * @return the handler in the registry that can handle the given cluster event.
     */
    public H getHandler(T obj);

}
