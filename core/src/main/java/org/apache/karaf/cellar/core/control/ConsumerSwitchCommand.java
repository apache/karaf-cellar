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

import org.apache.karaf.cellar.core.command.Command;

/**
 * Consumer switch command.
 */
public class ConsumerSwitchCommand extends Command<ConsumerSwitchResult> {

    private SwitchStatus status = null;

    public ConsumerSwitchCommand(String id) {
        super(id);
    }

    public ConsumerSwitchCommand(String id, SwitchStatus status) {
        super(id);
        this.status = status;
    }

    /**
     * Return the {@code SwitchStatus}.
     *
     * @return the current switch status.
     */
    public SwitchStatus getStatus() {
        return status;
    }

    /**
     * Set the {@code SwitchStatus}.
     *
     * @param status the new switch status.
     */
    public void setStatus(SwitchStatus status) {
        this.status = status;
    }

}
