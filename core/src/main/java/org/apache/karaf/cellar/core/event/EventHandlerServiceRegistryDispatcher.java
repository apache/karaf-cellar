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

/**
 * Default implementation of the event dispatcher.
 * This dispatcher relays on the handlers service registry to look for the handler
 * which is able to handle a given cluster event.
 */
public class EventHandlerServiceRegistryDispatcher<E extends Event> implements EventDispatcher<E> {

    private ExecutorService threadPool;
    private EventHandlerServiceRegistry handlerRegistry;

    public void init() {
        if (threadPool == null) {
            threadPool = Executors.newCachedThreadPool();
        }
    }

    @Override
    public void dispatch(E event) {
        EventDispatchTask task = new EventDispatchTask(event, handlerRegistry);
        threadPool.execute(task);
    }

    public EventHandlerServiceRegistry getHandlerRegistry() {
        return handlerRegistry;
    }

    public void setHandlerRegistry(EventHandlerServiceRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

}
