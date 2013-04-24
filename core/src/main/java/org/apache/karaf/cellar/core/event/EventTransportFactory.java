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
package org.apache.karaf.cellar.core.event;

/**
 * Description of a factory of transport, to get cluster event producers and consumers.
 */
public interface EventTransportFactory {

    /**
     * Get a cluster event producer.
     *
     * @param name the name of the producer.
     * @param pubsub true to set in pubsub mode, false else.
     * @return the cluster event producer.
     */
    public EventProducer getEventProducer(String name, Boolean pubsub);

    /**
     * Get a cluster event consumer.
     *
     * @param name the name of the consumer.
     * @param pubsub true to set in pubsub mode, false else.
     * @return the cluster event consumer.
     */
    public EventConsumer getEventConsumer(String name, Boolean pubsub);

}
