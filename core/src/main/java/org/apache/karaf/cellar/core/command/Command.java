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
import org.apache.karaf.cellar.core.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Command.
 */
public class Command<R extends Result> extends Event {

    protected static final Logger logger = LoggerFactory.getLogger(Command.class);

    protected long timeout = 10000;
    protected final BlockingQueue<Map<Node, R>> resultQueue = new LinkedBlockingQueue<Map<Node, R>>();
    protected final Map<Node, R> nodeResults = new HashMap<Node, R>();

    public Command(String id) {
        super(id);
        this.force = true;
    }

    /**
     * Set the force flag for the execution of this command.
     *
     * @return true if the force flag is set, false else.
     */
    @Override
    public Boolean getForce() {
        return true;
    }

    /**
     * Handle a command execution timeout.
     */
    public void onTimeout() {
        try {
            resultQueue.put(nodeResults);
        } catch (InterruptedException e) {
            logger.error("Error adding result to result queue", e);
        }
    }

    /**
     * Add command execution results into the results queue.
     *
     * @param results the command execution results.
     */
    public void addResults(R... results) {
        if (results != null && results.length > 0) {
            for (R result : results) {
                nodeResults.put(result.getSourceNode(), result);
            }

            if (getDestination() == null || (nodeResults.size() == getDestination().size())) {
                try {
                    resultQueue.put(nodeResults);
                } catch (InterruptedException e) {
                    logger.error("Error adding result to result queue", e);
                }
            }
        }
    }

    /**
     * Get the command execution results from the results queue.
     *
     * @return the map of command execution results.
     * @throws Exception if the command execution has timed out.
     */
    public Map<Node, R> getResult() throws InterruptedException {
        Map<Node, R> nodeResults = null;
        if (this.resultQueue != null) {
            nodeResults = resultQueue.poll(timeout, TimeUnit.MILLISECONDS);
        }
        return nodeResults;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

}
