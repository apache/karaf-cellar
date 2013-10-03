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

import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventTransportFactory;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * An event transport factory powered by Hazelcast.
 */
public class HazelcastEventTransportFactory extends HazelcastInstanceAware implements EventTransportFactory {

    private Dispatcher dispatcher;
    private CombinedClassLoader combinedClassLoader;
    private ConfigurationAdmin configurationAdmin;

    @Override
    public EventProducer getEventProducer(String name, Boolean pubsub) {
        if (pubsub) {
            ITopic topic = instance.getTopic(Constants.TOPIC + Constants.SEPARATOR + name);
            TopicProducer producer = new TopicProducer();
            producer.setInstance(instance);
            producer.setTopic(topic);
            producer.setNode(getNode());
            producer.setConfigurationAdmin(configurationAdmin);
            producer.init();
            return producer;
        } else {
            IQueue queue = instance.getQueue(Constants.QUEUE + Constants.SEPARATOR + name);
            QueueProducer producer = new QueueProducer();
            producer.setQueue(queue);
            producer.setNode(getNode());
            producer.setConfigurationAdmin(configurationAdmin);
            producer.init();
            return producer;
        }
    }

    @Override
    public EventConsumer getEventConsumer(String name, Boolean pubsub) {
        if (pubsub) {
            ITopic topic = instance.getTopic(Constants.TOPIC + Constants.SEPARATOR + name);
            TopicConsumer consumer = new TopicConsumer();
            consumer.setTopic(topic);
            consumer.setInstance(instance);
            consumer.setNode(getNode());
            consumer.setDispatcher(dispatcher);
            consumer.setConfigurationAdmin(configurationAdmin);
            consumer.init();
            return consumer;
        } else {

            IQueue queue = instance.getQueue(Constants.QUEUE + Constants.SEPARATOR + name);
            QueueConsumer consumer = new QueueConsumer(combinedClassLoader);
            consumer.setQueue(queue);
            consumer.setNode(getNode());
            consumer.setDispatcher(dispatcher);
            consumer.setConfigurationAdmin(configurationAdmin);
            consumer.init();
            return consumer;
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public CombinedClassLoader getCombinedClassLoader() {
        return combinedClassLoader;
    }

    public void setCombinedClassLoader(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
