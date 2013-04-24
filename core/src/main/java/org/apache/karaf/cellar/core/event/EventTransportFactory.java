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
 * A factory to create producer and consumer of cluster events.
 */
public interface EventTransportFactory {

    /**
     * Return a cluster {@link EventProducer} that produces cluster {@link Event}s for a specific cluster {@link org.apache.karaf.cellar.core.Group}.
     *
     * @param name the event producer name.
     * @param pubsub true to enable pubsub mode, false else.
     * @return the cluster event producer.
     */
    public EventProducer getEventProducer(String name, Boolean pubsub);

    /**
     * Return a cluster {@link EventConsumer} that consumes cluster {@link Event}s for a specific cluster {@link org.apache.karaf.cellar.core.Group}.
     *
     * @param name the event consumer name.
     * @param pubsub true to enable pubsub mode, false else.
     * @return the cluster event consumer.
     */
    public EventConsumer getEventConsumer(String name, Boolean pubsub);

}
