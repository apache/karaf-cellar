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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.config.Constants;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.management.CellarConfigMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.*;

/**
 * Implementation of the Cellar Config MBean allowing to manipulate Cellar config admin layer.
 */
public class CellarConfigMBeanImpl extends StandardMBean implements CellarConfigMBean {

    private ClusterManager clusterManager;

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public CellarConfigMBeanImpl() throws NotCompliantMBeanException {
        super(CellarConfigMBean.class);
    }

    public Set<String> list(String group) throws Exception {
        Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
        return configurationTable.keySet();
    }

    public Properties listProperties(String group, String pid) throws Exception {
        Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
        return configurationTable.get(pid);
    }

    public void setProperty(String group, String pid, String key, String value) throws Exception {
        Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
        Properties properties = configurationTable.get(pid);
        if (properties == null) {
            properties = new Properties();
        }
        properties.put(key, value);
        configurationTable.put(pid, properties);
    }

}
