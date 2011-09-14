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
package org.apache.karaf.cellar.obr.shell;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.RepositoryAdminImpl;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.cellar.obr.ObrBundleInfo;
import org.apache.karaf.cellar.obr.ObrUrlEvent;

import java.util.Set;

/**
 * cluster:obr-addurl command.
 */
@Command(scope = "cluster", name = "obr-addurl", description = "Register a repository URL in the distributed OBR service")
public class ObrAddUrlCommand extends ObrCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "url", description = "The repository URL to register in the OBR service", required = true, multiValued = false)
    String url;

    public Object doExecute() throws Exception {
        // find group for the given name
        Group group = groupManager.findGroupByName(groupName);
        // create an event and produce it
        EventProducer producer = eventTransportFactory.getEventProducer(groupName, true);
        ObrUrlEvent event = new ObrUrlEvent(url, Constants.OBR_URL_ADD_TYPE);
        event.setForce(true);
        event.setSourceGroup(group);
        producer.produce(event);
        // push the OBR URL in the distributed set
        Set<String> urls = clusterManager.getSet(Constants.OBR_URL + Configurations.SEPARATOR + groupName);
        urls.add(url);
        // push the bundles in the OBR distributed set
        Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.OBR_BUNDLE + Configurations.SEPARATOR + groupName);
        synchronized(obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(),resource.getSymbolicName(), resource.getVersion().toString());
                bundles.add(info);
            }
            obrService.removeRepository(url);
        }
        return null;
    }

}
