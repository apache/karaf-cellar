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

/**
 * A Runnable task that is used for scheduling command timeout events.
 */
public class TimeoutTask implements Runnable {

    private Command command;
    private CommandStore store;

    public TimeoutTask(Command command, CommandStore store) {
        this.command = command;
        this.store = store;
    }

    /**
     * Runs the timeout task.
     */
    @Override
    public void run() {
        // check if command is still pending
        Boolean pending = store.getPending().containsKey(command.getId());
        if (pending) {
            store.getPending().remove(command.getId());
        }
    }

}
