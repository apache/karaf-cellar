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

import org.apache.karaf.cellar.core.control.Switch;

import java.io.Serializable;

/**
 * Description of a cluster event handler.
 */
public interface Handler<T extends Serializable> {

    /**
     * Get the event type that the handler can handle.
     *
     * @return the event type class.
     */
    public Class<T> getType();

    /**
     * Get the handler switch.
     *
     * @return the handler switch.
     */
    public Switch getSwitch();

}
