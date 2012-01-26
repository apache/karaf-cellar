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
package org.apache.karaf.cellar.bundle.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.bundle.RemoteBundleEvent;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.osgi.framework.BundleEvent;

@Command(scope = "cluster", name = "bundle-start", description = "Start a bundle on a given cluster group")
public class StartBundleCommand extends CellarCommandSupport {
    
    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;
    
    @Argument(index = 1, name = "name", description = "The bundle symbolic name", required = true, multiValued = false)
    String name;

    @Argument(index = 2, name = "version", description = "The bundle version", required = true, multiValued = false)
    String version;
    
    @Override
    protected Object doExecute() throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        EventProducer producer = eventTransportFactory.getEventProducer(groupName, true);
        RemoteBundleEvent event = new RemoteBundleEvent(name, version, null, BundleEvent.STARTED);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);
        
        return null;
    }
    
}
