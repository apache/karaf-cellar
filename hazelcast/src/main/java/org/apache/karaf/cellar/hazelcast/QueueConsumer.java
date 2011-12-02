/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemListener;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumes messages from the distributed {@code ITopic} and calls the {@code EventDispatcher}.
 */
public class QueueConsumer<E extends Event> implements EventConsumer<E>, ItemListener<E>, Runnable {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueueConsumer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.queue.consumer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Boolean isConsuming = Boolean.TRUE;

    private HazelcastInstance instance;
    private IQueue queue;
    private Dispatcher dispatcher;
    private Node node;
    private CombinedClassLoader combinedClassLoader;


    public QueueConsumer() {
    }

    public QueueConsumer(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }

    /**
     * Initialization method.
     */
    public void init() {
        if (queue != null) {
            queue.addItemListener(this,true);
        } else {
            queue = instance.getQueue(Constants.QUEUE);
            queue.addItemListener(this,true);
        }
        executorService.execute(this);
    }

    /**
     * Destruction method.
     */
    public void destroy() {
        isConsuming = false;
        if (queue != null) {
            queue.removeItemListener(this);
        }
        executorService.shutdown();
    }

    @Override
    public void run() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            while (isConsuming) {
                if(combinedClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(combinedClassLoader);
                } else Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                E e = null;
                try {
                        e = getQueue().poll(10, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("CELLAR HAZELCAST: consume task interrupted");
                }
                if (e != null) {
                    consume(e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR HAZELCAST: error while consuming from queue",ex);
        }
        finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Consumes an event form the topic.
     *
     * @param event
     */
    public void consume(E event) {
        if (event != null  && (eventSwitch.getStatus().equals(SwitchStatus.ON) || event.getForce())) {
                dispatcher.dispatch(event);
        }
    }

    @Override
    public void start()
    {
        isConsuming = true;
        executorService.execute(this);
    }

    @Override
    public void stop() {
      isConsuming = false;
    }

    public Boolean isConsuming() {
        return isConsuming;
    }

    @Override
    public void itemAdded(E event) {

    }

    @Override
    public void itemRemoved(E event) {

    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public IQueue<E> getQueue() {
        return queue;
    }

    public void setQueue(IQueue<E> queue) {
        this.queue = queue;
    }

    public Switch getSwitch() {
        return eventSwitch;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }
}
