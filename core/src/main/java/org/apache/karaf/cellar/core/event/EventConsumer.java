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

import org.apache.karaf.cellar.core.Consumer;

/**
 * Event consumer.
 */
public interface EventConsumer<E extends Event> extends Consumer<E> {

    /**
     * Consume {@code Event}s to the cluster.
     *
     * @param event the cluster event to consume.
     */
    @Override
    public void consume(E event);

    /**
     * Start to consume cluster events.
     */
    public void start();

    /**
     * Stop to consume cluster events.
     */
    public void stop();

    /**
     * Check the current event consumer status.
     *
     * @return true if the consumer is consuming, false else.
     */
    public Boolean isConsuming();

}
