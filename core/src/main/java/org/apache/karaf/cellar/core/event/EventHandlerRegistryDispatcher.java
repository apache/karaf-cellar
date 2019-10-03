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
package org.apache.karaf.cellar.core.event;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event handler service registry dispatcher.
 */
public class EventHandlerRegistryDispatcher<E extends Event> implements EventDispatcher<E> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(EventHandlerRegistryDispatcher.class);
    private ExecutorService threadPool;
    private EventHandlerRegistry handlerRegistry;

    public void init() {
        if (threadPool == null) {
            if (Boolean.getBoolean(this.getClass().getName() + ".threadPool.singleThreadExecutor")) {
                LOGGER.info("Will use an Executor that uses a single worker thread");
                threadPool = Executors.newSingleThreadExecutor();
            } else {
                LOGGER.info("Will use an Executor with a pool of threads");
                threadPool = Executors.newCachedThreadPool();
            }
        }
    }

    /**
     * Dispatch a cluster {@code Event} to the appropriate cluster {@code EventHandler}.
     *
     * @param event the cluster event to dispatch.
     */
    public void dispatch(E event) {
        EventDispatchTask task = new EventDispatchTask(event, handlerRegistry);
        threadPool.execute(task);
    }

    public EventHandlerRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public void setHandlerRegistry(EventHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    public void destroy() {
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

}
