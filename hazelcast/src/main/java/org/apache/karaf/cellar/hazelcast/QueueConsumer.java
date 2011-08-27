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
import com.hazelcast.core.ITopic;
import com.hazelcast.core.ItemListener;
import com.hazelcast.core.MessageListener;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Consumes messages from the distributed {@code ITopic} and calls the {@code EventDispatcher}.
 */
public class QueueConsumer<E extends Event> implements EventConsumer<E>, ItemListener<E> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.queue.consumer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private HazelcastInstance instance;
    private IQueue queue;
    private Dispatcher dispatcher;
    private Node node;

    private QueueConsumeTask queueConsumeTask = new QueueConsumeTask(this);
    private ExecutorService executorService = Executors.newSingleThreadExecutor();


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

    }

    /**
     * Destruction method.
     */
    public void destroy() {
        if (queue != null) {
            queue.removeItemListener(this);
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
        queueConsumeTask.activate();
        executorService.execute(queueConsumeTask);
    }


    @Override
    public void stop() {
      queueConsumeTask.deactivate();
    }

    @Override
    public void itemAdded(E event) {

    }

    @Override
    public void itemRemoved(E event) {

    }

    public QueueConsumeTask getQueueConsumeTask() {
        return queueConsumeTask;
    }

    public void setQueueConsumeTask(QueueConsumeTask queueConsumeTask) {
        this.queueConsumeTask = queueConsumeTask;
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
