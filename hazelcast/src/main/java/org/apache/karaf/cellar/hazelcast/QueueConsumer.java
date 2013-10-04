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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumes cluster events from the Hazelcast {@code IQueue} and calls the {@code EventDispatcher}.
 */
public class QueueConsumer<E extends Event> implements EventConsumer<E>, ItemListener<E>, Runnable {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(QueueConsumer.class);

    public static final String SWITCH_ID = "org.apache.karaf.cellar.queue.consumer";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Boolean isConsuming = Boolean.TRUE;

    private HazelcastInstance instance;
    private IQueue queue;
    private Dispatcher dispatcher;
    private Node node;
    private CombinedClassLoader combinedClassLoader;
    private ConfigurationAdmin configurationAdmin;

    public QueueConsumer() {
        // nothing to do
    }

    public QueueConsumer(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }

    public void init() {
        if (queue != null) {
            queue.addItemListener(this, true);
        } else {
            queue = instance.getQueue(Constants.QUEUE);
            queue.addItemListener(this, true);
        }
        executorService.execute(this);
    }

    public void destroy() {
        isConsuming = false;
        if (queue != null) {
            queue.removeItemListener(this);
        }
        executorService.shutdown();
    }

    @Override
    public void run() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            while (isConsuming) {
                if (combinedClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(combinedClassLoader);
                } else Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                E e = null;
                try {
                    e = getQueue().poll(10, TimeUnit.SECONDS);
                } catch (InterruptedException e1) {
                    LOGGER.warn("CELLAR HAZELCAST: consume task interrupted");
                }
                if (e != null) {
                    consume(e);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR HAZELCAST: failed to consume from queue", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Consume a cluster event.
     *
     * @param event the cluster event.
     */
    @Override
    public void consume(E event) {
        if (event != null && (this.getSwitch().getStatus().equals(SwitchStatus.ON) || event.getForce())) {
            dispatcher.dispatch(event);
        } else {
            if (eventSwitch.getStatus().equals(SwitchStatus.OFF)) {
                LOGGER.debug("CELLAR HAZELCAST: {} switch is OFF, cluster event is not consumed", SWITCH_ID);
            }
        }
    }

    @Override
    public void start() {
        isConsuming = true;
        executorService.execute(this);
    }

    @Override
    public void stop() {
        isConsuming = false;
    }

    @Override
    public Boolean isConsuming() {
        return isConsuming;
    }

    @Override
    public void itemAdded(ItemEvent<E> event) {
        // nothing to do
    }

    @Override
    public void itemRemoved(ItemEvent<E> event) {
        // nothing to do
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public IQueue<E> getQueue() {
        return queue;
    }

    public void setQueue(IQueue<E> queue) {
        this.queue = queue;
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.CONSUMER));
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
