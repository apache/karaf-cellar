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

import org.apache.karaf.cellar.core.Consumer;
import org.apache.karaf.cellar.core.command.CommandHandler;

/**
 * @author iocanel
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
        //Query
        if (command.getStatus() == null) {
            ConsumerSwitchResult result = new ConsumerSwitchResult(command.getId(), Boolean.TRUE, consumer.getSwitch().getStatus().getValue());
            return result;
        }
        //Turn on the switch
        if (command.getStatus().equals(SwitchStatus.ON)) {
            consumer.getSwitch().turnOn();
            ConsumerSwitchResult result = new ConsumerSwitchResult(command.getId(), Boolean.TRUE, Boolean.TRUE);
            return result;
        }
        //Turn on the switch
        else if (command.getStatus().equals(SwitchStatus.OFF)) {
            consumer.getSwitch().turnOff();
            ConsumerSwitchResult result = new ConsumerSwitchResult(command.getId(), Boolean.TRUE, Boolean.FALSE);
            return result;
        } else {
            ConsumerSwitchResult result = new ConsumerSwitchResult(command.getId(), Boolean.FALSE, consumer.getSwitch().getStatus().getValue());
            return result;
        }
    }

    public Class<ConsumerSwitchCommand> getType() {
        return ConsumerSwitchCommand.class;
    }

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
