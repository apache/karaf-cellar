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
package org.apache.karaf.cellar.obr;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Cellar OBR support.
 */
public class ObrSupport extends CellarSupport {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrSupport.class);

    protected RepositoryAdmin obrService;

    public void init() { }

    public void destroy() { }

    /**
     * Push an OBR bundle ID in the distribution map.
     * @param bundleId
     * @param group
     */
    public void pushBundle(String bundleId, Group group) {
        if (bundleId != null) {
            String groupName = group.getName();
            Set<String> bundles = clusterManager.getSet(Constants.OBR_BUNDLE + Configurations.SEPARATOR + groupName);

            if (isAllowed(group, Constants.OBR_BUNDLE_CATEGORY, bundleId, EventType.OUTBOUND)) {
                if (obrService != null && bundles != null) {
                    bundles.add(bundleId);
                }
            } else LOGGER.debug("OBR bundle ID {} event is marked as BLOCKED OUTBOUND", bundleId);
        } else LOGGER.debug("OBR bundle ID is null");
    }

    public RepositoryAdmin getObrService() {
        return this.obrService;
    }

    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }

}
