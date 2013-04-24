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
 * Basic switch.
 */
public class BasicSwitch implements Switch {

    private SwitchStatus status = SwitchStatus.ON;
    private String name;

    public BasicSwitch(String name) {
        this.name = name;
    }

    public BasicSwitch(String name, SwitchStatus status) {
        this.status = status;
        this.name = name;
    }

    /**
     * Turn on the switch.
     */
    @Override
    public void turnOn() {
        this.status = SwitchStatus.ON;
    }

    /**
     * Turn off the switch.
     */
    @Override
    public void turnOff() {
        this.status = SwitchStatus.OFF;
    }

    /**
     * Return the current status of the {@code Switch}.
     *
     * @return
     */
    @Override
    public SwitchStatus getStatus() {
        return status;
    }

    /**
     * Return the name of the {@code Switch}.
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

}
