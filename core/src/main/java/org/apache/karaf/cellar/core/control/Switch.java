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

package org.apache.karaf.cellar.core.control;

/**
 * An interface that describes objects that can be turned on/off and act like a switch.
 *
 * @author iocanel
 */
public interface Switch {

    /**
     * Returns the name of the Switch.
     *
     * @return
     */
    public String getName();

    /**
     * Turns on.
     */
    public void turnOn();

    /**
     * Turns off
     */
    public void turnOff();

    /**
     * Returns the status of the switch.
     *
     * @return
     */
    public SwitchStatus getStatus();
}
