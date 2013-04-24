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

import org.apache.karaf.cellar.core.Producer;

/**
 * Broadcast cluster events to the cluster.
 */
public interface EventProducer<E extends Event> extends Producer<E> {

    /**
     * Produce and broadcast cluster {@code Event}s to the cluster.
     *
     * @param event the cluster event to produce and broadcast.
     */
    @Override
    public void produce(E event);

}
