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

/**
 * Track a cluster event.
 */
public interface EventTracker<E extends Event> {

    /**
     * Start to track the occurrence of a cluster {@code Event}.
     *
     * @param event the cluster event to track.
     */
    public void start(E event);

    /**
     * Stop to track the occurrence of a cluster {@code Event}.
     *
     * @param event the cluster event to track.
     */
    public void stop(E event);

    /**
     * Check if a cluster event is pending.
     *
     * @param event the cluster event to check.
     * @return true if the cluster event is pending, false else.
     */
    public Boolean isPending(E event);

}
