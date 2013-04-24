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
 * Describe a cluster event consumer.
 */
public interface EventConsumer<E extends Event> extends Consumer<E> {

    /**
     * Consume a cluster {@code Event} from the cluster.
     *
     * @param event
     */
    @Override
    public void consume(E event);

    /**
     * Start the consumer.
     */
    public void start();

    /**
     * Stop the consumer.
     */
    public void stop();

    /**
     * Check if the consumer is active.
     *
     * @return true if the consumer is active, false else.
     */
    public Boolean isConsuming();

}
