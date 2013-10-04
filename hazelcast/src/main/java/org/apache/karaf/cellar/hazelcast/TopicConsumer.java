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
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumes messages from the Hazelcast {@code ITopic} and calls the {@code EventDispatcher}.
 */
public class TopicConsumer<E extends Event> implements EventConsumer<E>, MessageListener<E> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(TopicConsumer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.topic.consumer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private HazelcastInstance instance;
    private ITopic topic;
    private Dispatcher dispatcher;
    private Node node;
    private ConfigurationAdmin configurationAdmin;

    private boolean isConsuming;

    public void init() {
        if (topic == null) {
            topic = instance.getTopic(Constants.TOPIC);
        }
        start();
    }

    public void destroy() {
        stop();
    }

    @Override
    public void consume(E event) {
        // check if event has a specified destination.
        if ((event.getDestination() == null || event.getDestination().contains(node)) && (this.getSwitch().getStatus().equals(SwitchStatus.ON) || event.getForce())) {
            dispatcher.dispatch(event);
        } else {
            if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
                LOGGER.debug("CELLAR HAZELCAST: {} switch is OFF, cluster event is not consumed", SWITCH_ID);
            }
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

    @Override
    public void onMessage(Message<E> message) {
        consume(message.getMessageObject());
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

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.CONSUMER));
                if (status) {
                    eventSwitch.turnOn();
                } else {
                    eventSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return eventSwitch;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
