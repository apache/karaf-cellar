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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Dictionary;
import java.util.Properties;

@Command(scope = "cluster", name = "config-property-excluded", description = "Display or set the config properties excluded from the cluster sync")
public class PropExcludedCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "excluded-properties", description = "A list of comma separated properties excluded from the cluster sync", required = false, multiValued = false)
    String excludedProperties;

    private ConfigurationAdmin configurationAdmin;

    @Override
    protected Object doExecute() throws Exception {
        Configuration nodeConfiguration = configurationAdmin.getConfiguration(Configurations.NODE, null);
        if (excludedProperties == null || excludedProperties.isEmpty()) {
            // display mode
            if (nodeConfiguration != null) {
                Dictionary properties = nodeConfiguration.getProperties();
                if (properties != null) {
                    System.out.println(properties.get("config.filtered.properties"));
                }
            }
        } else {
            // set mode
            if (nodeConfiguration != null) {
                Dictionary properties = nodeConfiguration.getProperties();
                if (properties == null)
                    properties = new Properties();
                properties.put("config.filtered.properties", excludedProperties);
                nodeConfiguration.update(properties);
            }
        }
        return null;
    }

    @Override
    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
