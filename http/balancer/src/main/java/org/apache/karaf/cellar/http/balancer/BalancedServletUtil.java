/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.http.balancer;

import org.apache.karaf.cellar.core.ClusterManager;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public class BalancedServletUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(BalancedServletUtil.class);

    private ClusterManager clusterManager;
    private ConfigurationAdmin configurationAdmin;

    public String constructLocation(String alias) {
        String httpHost = clusterManager.getNode().getHost();
        String httpPort = null;
        try {
            Configuration configuration = configurationAdmin.getConfiguration("org.ops4j.pax.web", null);
            if (configuration != null) {
                Dictionary properties = configuration.getProperties();
                if (properties != null) {
                    httpPort = (String) properties.get("org.osgi.service.http.port");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR HTTP BALANCER: can't get HTTP port number from configuration", e);
        }
        if (httpPort == null)
            httpPort = "8181";
        String location = "http://" + httpHost + ":" + httpPort + alias;
        return location;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
