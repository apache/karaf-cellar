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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: iocanel
 */
public class HazelcastEventTransportFactory extends HazelcastInstanceAware implements EventTransportFactory {

    private Dispatcher dispatcher;

    private Map<String, QueueConsumer> queueConsumerMap = new HashMap<String, QueueConsumer>();
    private Map<String, QueueProducer> queueProducerMap = new HashMap<String, QueueProducer>();

    private Map<String, TopicConsumer> topicConsumerMap = new HashMap<String, TopicConsumer>();
    private Map<String, TopicProducer> topicProducerMap = new HashMap<String, TopicProducer>();


    @Override
    public EventProducer getEventProducer(String name, Boolean pubsub) {
        if (pubsub) {
            TopicProducer producer = topicProducerMap.get(name);
            if (producer != null) {
                return producer;
            } else {
                ITopic topic = instance.getTopic(Constants.TOPIC + Constants.SEPARATOR + name);
                producer = new TopicProducer();
                producer.setTopic(topic);
                producer.setNode(getNode());
                producer.init();
                topicProducerMap.put(name, producer);
                return producer;
            }
        } else {
            QueueProducer producer = queueProducerMap.get(name);
            if (producer != null) {
                return producer;
            } else {
                IQueue queue = instance.getQueue(Constants.QUEUE + Constants.SEPARATOR + name);
                producer = new QueueProducer();
                producer.setQueue(queue);
                producer.setNode(getNode());
                producer.init();
                queueProducerMap.put(name, producer);
                return producer;
            }
        }
    }

    @Override
    public EventConsumer getEventConsumer(String name, Boolean pubsub) {
        if (pubsub) {
            TopicConsumer consumer = topicConsumerMap.get(name);
            if (consumer != null) {
                return consumer;
            } else {

                ITopic topic = instance.getTopic(Constants.TOPIC + Constants.SEPARATOR + name);
                consumer = new TopicConsumer();
                consumer.setTopic(topic);
                consumer.setNode(getNode());
                consumer.setDispatcher(dispatcher);
                consumer.init();
                topicConsumerMap.put(name, consumer);
                return consumer;
            }
        } else {
            QueueConsumer consumer = queueConsumerMap.get(name);
            if (consumer != null) {
                return consumer;
            } else {
                IQueue queue = instance.getQueue(Constants.QUEUE + Constants.SEPARATOR + name);
                consumer = new QueueConsumer();
                consumer.setQueue(queue);
                consumer.setNode(getNode());
                consumer.setDispatcher(dispatcher);
                consumer.init();
                consumer.start();
                queueConsumerMap.put(name, consumer);
                return consumer;
            }
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
