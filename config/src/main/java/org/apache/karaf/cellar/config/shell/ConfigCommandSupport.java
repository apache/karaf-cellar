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
package org.apache.karaf.cellar.config.shell;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;

/**
 * Abstract cluster config command support.
 */
public abstract class ConfigCommandSupport extends CellarCommandSupport {

    /**
     * Check if a configuration is allowed.
     *
     * @param group the cluster group.
     * @param category the configuration category constant.
     * @param pid the configuration PID.
     * @param type the cluster event type.
     * @return true if the cluster event type is allowed, false else.
     */
    public boolean isAllowed(Group group, String category, String pid, EventType type) {
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        return support.isAllowed(group, category, pid, type);
    }

}
