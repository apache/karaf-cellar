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

import org.apache.karaf.cellar.core.Producer;
import org.apache.karaf.cellar.core.command.CommandHandler;

/**
 * @author iocanel
 */
public class ProducerSwitchCommandHandler extends CommandHandler<ProducerSwitchCommand, ProducerSwitchResult> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.producer.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);


    /**
     * Handle the {@code ProducerSwitchCommand} command.
     *
     * @param command
     */
    public ProducerSwitchResult execute(ProducerSwitchCommand command) {
        //Query
        if (command.getStatus() == null) {
            ProducerSwitchResult result = new ProducerSwitchResult(command.getId(), Boolean.TRUE, producer.getSwitch().getStatus().getValue());
            return result;
        }
        //Turn on the switch
        else if (command.getStatus().equals(SwitchStatus.ON)) {
            producer.getSwitch().turnOn();
            ProducerSwitchResult result = new ProducerSwitchResult(command.getId(), Boolean.TRUE, Boolean.TRUE);
            return result;
        }
        //Turn on the switch
        else if (command.getStatus().equals(SwitchStatus.OFF)) {
            producer.getSwitch().turnOff();
            ProducerSwitchResult result = new ProducerSwitchResult(command.getId(), Boolean.TRUE, Boolean.FALSE);
            return result;
        } else {
            ProducerSwitchResult result = new ProducerSwitchResult(command.getId(), Boolean.FALSE, producer.getSwitch().getStatus().getValue());
            return result;
        }
    }

    public Class<ProducerSwitchCommand> getType() {
        return ProducerSwitchCommand.class;
    }

    public Switch getSwitch() {
        return commandSwitch;
    }

    public Producer getProducer() {
        return producer;
    }

    public void setProducer(Producer producer) {
        this.producer = producer;
    }
}
