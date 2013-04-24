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
package org.apache.karaf.cellar.core.control;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Consumer;
import org.apache.karaf.cellar.core.command.CommandHandler;
import org.osgi.service.cm.Configuration;

import java.util.Dictionary;

/**
 * Consumer switch command handler.
 */
public class ConsumerSwitchCommandHandler extends CommandHandler<ConsumerSwitchCommand, ConsumerSwitchResult> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.producer.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    private Consumer consumer;

    /**
     * Handle the {@code ProducerSwitchCommand} command.
     *
     * @param command
     */
    public ConsumerSwitchResult execute(ConsumerSwitchCommand command) {
        // query
        if (command.getStatus() == null) {
            return new ConsumerSwitchResult(command.getId(), Boolean.TRUE, consumer.getSwitch().getStatus().getValue());
        } else if (command.getStatus().equals(SwitchStatus.ON)) {
            // turn on the switch
            consumer.getSwitch().turnOn();
            // persist the change
            persist(command.getStatus());
            return new ConsumerSwitchResult(command.getId(), Boolean.TRUE, Boolean.TRUE);
        } else if (command.getStatus().equals(SwitchStatus.OFF)) {
            // turn on the switch
            consumer.getSwitch().turnOff();
            // persist the change
            persist(command.getStatus());
            return new ConsumerSwitchResult(command.getId(), Boolean.TRUE, Boolean.FALSE);
        } else {
            return new ConsumerSwitchResult(command.getId(), Boolean.FALSE, consumer.getSwitch().getStatus().getValue());
        }
    }

    /**
     * Store the consumer current status in ConfigurationAdmin.
     *
     * @param switchStatus the producer switch status to store.
     */
    private void persist(SwitchStatus switchStatus) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE);
            if (configuration != null) {
                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties != null) {
                    properties.put(Configurations.CONSUMER, switchStatus.getValue().toString());
                    configuration.update(properties);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Can't persist the consumer status", e);
        }
    }

    @Override
    public Class<ConsumerSwitchCommand> getType() {
        return ConsumerSwitchCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

}
