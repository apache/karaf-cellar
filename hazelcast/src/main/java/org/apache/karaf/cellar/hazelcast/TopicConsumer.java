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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;

/**
 * Consumes messages from the distributed {@code ITopic} and calls the {@code EventDispatcher}.
 */
public class TopicConsumer<E extends Event> implements EventConsumer<E>, MessageListener<E> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.topic.consumer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private HazelcastInstance instance;
    private ITopic topic;
    private Dispatcher dispatcher;
    private Node node;

    private boolean isConsuming;

    /**
     * Initialization method.
     */
    public void init() {
        if (topic == null) {
            topic = instance.getTopic(Constants.TOPIC);
        }
        start();
    }

    /**
     * Destruction method.
     */
    public void destroy() {
        stop();
    }

    /**
     * Consumes an event form the topic.
     *
     * @param event
     */
    public void consume(E event) {
        //Check if event has a specified destination.
        if ((event.getDestination() == null || event.getDestination().contains(node)) && (eventSwitch.getStatus().equals(SwitchStatus.ON) || event.getForce())) {
            dispatcher.dispatch(event);
        }
    }

    @Override
    public void start() {
        isConsuming = true;
        if (topic != null) {
            topic.addMessageListener(this);
        } else {
            topic = instance.getTopic(Constants.TOPIC);
            topic.addMessageListener(this);
        }

    }

    @Override
    public void stop() {
        isConsuming = false;
        if (topic != null) {
            topic.removeMessageListener(this);
        }
    }

    @Override
    public Boolean isConsuming() {
        return isConsuming;
    }

    public void onMessage(E message) {
        consume(message);
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

    public ITopic getTopic() {
        return topic;
    }

    public void setTopic(ITopic topic) {
        this.topic = topic;
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
