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

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;

/**
 * Handler for the cluster result events.
 */
public class ResultHandler<R extends Result> implements EventHandler<R> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.result.handler";

    private Switch handlerSwitch = new BasicSwitch(SWITCH_ID);
    private CommandStore commandStore;

    /**
     * Retrieve the correlated command from the store and set the result on the command object.
     *
     * @param result the cluster result event.
     */
    @Override
    public void handle(R result) {
        if (commandStore != null && commandStore.getPending() != null) {
            String id = result.getId();
            Command command = commandStore.getPending().get(id);

            if (command != null && handlerSwitch.getStatus().equals(SwitchStatus.ON)) {
                command.addResults(result);
            }
        }
    }

    /**
     * Get the type of result.
     *
     * @return the result type.
     */
    @Override
    public Class<R> getType() {
        return null;
    }

    /**
     * Get the result handler switch.
     *
     * @return the result handler switch.
     */
    @Override
    public Switch getSwitch() {
        return handlerSwitch;
    }

    public CommandStore getCommandStore() {
        return commandStore;
    }

    public void setCommandStore(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

}
