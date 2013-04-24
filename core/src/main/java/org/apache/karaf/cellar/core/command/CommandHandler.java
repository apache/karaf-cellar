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
 * Cluster command event handler.
 */
public abstract class CommandHandler<C extends Command<R>, R extends Result> extends CellarSupport implements EventHandler<C> {

    protected Producer producer;

    /**
     * Handles a received cluster command event.
     *
     * @param command the received cluster command event.
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
     * Execute a command.
     *
     * @param command the command to execute.
     * @return the command execution result.
     */
    public abstract R execute(C command);

    /**
     * Get the cluster event type handled by this handler.
     *
     * @return the cluster event type handled by this handler.
     */
    @Override
    public abstract Class<C> getType();

    /**
     * Get the handler switch.
     *
     * @return the handler switch.
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
