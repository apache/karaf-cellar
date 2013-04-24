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
 * An interface that describes switch that can be turned on/off.
 */
public interface Switch {

    /**
     * Returns the name of the switch.
     *
     * @return the switch name.
     */
    public String getName();

    /**
     * Turns on the switch.
     */
    public void turnOn();

    /**
     * Turns off the switch.
     */
    public void turnOff();

    /**
     * Returns the current status of the switch.
     *
     * @return the current status of the switch.
     */
    public SwitchStatus getStatus();

}
