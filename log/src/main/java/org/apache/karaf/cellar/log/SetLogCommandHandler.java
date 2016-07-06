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
package org.apache.karaf.cellar.log;

import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.log.core.LogService;

import java.util.Map;

public class SetLogCommandHandler extends CommandHandler<SetLogCommand, SetLogResult> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.log.set.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    private LogService logService;

    @Override
    public SetLogResult execute(SetLogCommand command) {
        logService.setLevel(command.getLogger(), command.getLevel());
        SetLogResult result = new SetLogResult(command.getId());
        return result;
    }

    @Override
    public Class<SetLogCommand> getType() {
        return SetLogCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

    public LogService getLogService() {
        return logService;
    }

    public void setLogService(LogService logService) {
        this.logService = logService;
    }

}
