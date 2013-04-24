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

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;

import java.util.HashSet;
import java.util.Set;

/**
 * Command handler.
 */
public abstract class CommandHandler<C extends Command<R>, R extends Result> extends CellarSupport implements EventHandler<C> {

    protected Producer producer;

    /**
     * Handle a {@code Command}.
     *
     * @param command the command to handle.
     */
    @Override
    public void handle(C command) {
        if (producer != null) {
            R result = execute(command);

            Set<Node> destination = new HashSet<Node>();
            destination.add(command.getSourceNode());

            result.setDestination(destination);
            producer.produce(result);
        }
    }

    /**
     * Execute a {@code Command} and return a {@code Result}.
     *
     * @param command the command to execute.
     * @return the result of the execution.
     */
    public abstract R execute(C command);

    /**
     * Get the type of the command.
     *
     * @return the type class.
     */
    @Override
    public abstract Class<C> getType();

    /**
     * Get the switch of the command handler.
     *
     * @return the command handler switch.
     */
    @Override
    public abstract Switch getSwitch();

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

}
