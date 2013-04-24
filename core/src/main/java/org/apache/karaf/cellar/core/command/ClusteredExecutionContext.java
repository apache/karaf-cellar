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

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.exception.ProducerNotFoundException;
import org.apache.karaf.cellar.core.exception.StoreNotFoundException;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Clustered execution context.
 */
public class ClusteredExecutionContext implements ExecutionContext {

    private Producer producer;
    private CommandStore commandStore;

    private ScheduledExecutorService timeoutScheduler = new ScheduledThreadPoolExecutor(10);

    public ClusteredExecutionContext() {
        // nothing to do
    }

    public ClusteredExecutionContext(Producer producer, CommandStore commandStore) {
        this.producer = producer;
        this.commandStore = commandStore;
    }

    @Override
    public <R extends Result, C extends Command<R>> Map<Node, R> execute(C command) throws StoreNotFoundException, ProducerNotFoundException, InterruptedException {
        if (command == null) {
            throw new StoreNotFoundException("Command store not found");
        } else {
            commandStore.getPending().put(command.getId(), command);
            TimeoutTask timeoutTask = new TimeoutTask(command, commandStore);
            timeoutScheduler.schedule(timeoutTask, command.getTimeout(), TimeUnit.MILLISECONDS);
        }

        if (producer != null) {
            producer.produce(command);
            return command.getResult();
        } else {
            throw new ProducerNotFoundException("Command producer not found");
        }
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }

    public CommandStore getCommandStore() {
        return commandStore;
    }

    public void setCommandStore(CommandStore commandStore) {
        this.commandStore = commandStore;
    }

}
