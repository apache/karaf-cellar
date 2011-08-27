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

import org.apache.karaf.cellar.core.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author: iocanel
 */
public class QueueConsumeTask<E extends Event> implements Runnable {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueueConsumeTask.class);

    private QueueConsumer<E> consumer;
    private Boolean keepConsuming = Boolean.TRUE;

    /**
     * Constructor
     *
     * @param consumer
     */
    public QueueConsumeTask(QueueConsumer<E> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void run() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            while (keepConsuming) {
                E e = null;
                try {
                    if (consumer != null) {
                        e = consumer.getQueue().poll(10, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e1) {
                    LOGGER.warn("Consume task interrupted");
                }
                if (e != null) {
                    consumer.consume(e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error while consuming from queue",ex);
        }
        finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public void activate() {
        keepConsuming = Boolean.TRUE;
    }

    public void deactivate() {
        keepConsuming = Boolean.FALSE;
    }

    public QueueConsumer<E> getConsumer() {
        return consumer;
    }

    public void setConsumer(QueueConsumer<E> consumer) {
        this.consumer = consumer;
    }

}
