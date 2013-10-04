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
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.Result;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Produces cluster {@code Event}s into the distributed {@code ITopic}.
 */
public class TopicProducer<E extends Event> implements EventProducer<E> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(TopicProducer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.topic.producer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    private HazelcastInstance instance;
    private ITopic topic;
    private Node node;
    private ConfigurationAdmin configurationAdmin;

    public void init() {
        if (topic == null) {
            topic = instance.getTopic(Constants.TOPIC);
        }
    }

    public void destroy() {
        // nothing to do
    }

    @Override
    public void produce(E event) {
        if (this.getSwitch().getStatus().equals(SwitchStatus.ON) || event.getForce() || event instanceof Result) {
            event.setSourceNode(node);
            topic.publish(event);
        } else {
            if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
                LOGGER.debug("CELLAR HAZELCAST: {} switch is OFF, don't produce the cluster event", SWITCH_ID);
            }
        }
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.PRODUCER));
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

    public ITopic<? extends Event> getTopic() {
        return topic;
    }

    public void setTopic(ITopic<Event> topic) {
        this.topic = topic;
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
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
