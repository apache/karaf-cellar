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
 * Description of a switch, that it can be turned on or off.
 */
public interface Switch {

    /**
     * Get the name of the switch.
     *
     * @return the switch name.
     */
    public String getName();

    /**
     * Turn on the switch.
     */
    public void turnOn();

    /**
     * Turn off the switch.
     */
    public void turnOff();

    /**
     * Get the current status of the switch.
     *
     * @return the current switch status.
     */
    public SwitchStatus getStatus();

}
