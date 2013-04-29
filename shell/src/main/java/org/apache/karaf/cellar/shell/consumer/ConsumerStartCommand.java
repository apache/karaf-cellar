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
package org.apache.karaf.cellar.shell.consumer;

import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;

import java.util.List;

@Command(scope = "cluster", name = "consumer-start", description = "Start a cluster event consumer")
public class ConsumerStartCommand extends ConsumerSupport {

    @Argument(index = 0, name = "node", description = "The node(s) ID", required = false, multiValued = true)
    List<String> nodes;

    @Override
    protected Object doExecute() throws Exception {
        return doExecute(nodes, SwitchStatus.ON);
    }

}
