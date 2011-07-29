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
import javax.management.openmbean.*;
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

    public String[] listConfig(String group) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            ArrayType arrayType = new ArrayType(1, SimpleType.STRING);
            Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
            return configurationTable.keySet().toArray(new String[configurationTable.keySet().size()]);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public TabularData listProperties(String group, String pid) throws Exception {

        CompositeType compositeType = new CompositeType("Property", "Cellar Config Property",
                new String[]{ "key", "value" },
                new String[]{ "Property key", "Property value" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING });
        TabularType tableType = new TabularType("Properties", "Table of all properties in the Config PID",
                compositeType, new String[]{ "key" });
        TabularData table = new TabularDataSupport(tableType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
            Properties properties = configurationTable.get(pid);
            if (properties != null) {
                Enumeration propertyNames = properties.propertyNames();
                while (propertyNames.hasMoreElements()) {
                    String key = (String) propertyNames.nextElement();
                    String value = (String) properties.get(key);
                    CompositeDataSupport data = new CompositeDataSupport(compositeType,
                            new String[]{ "key", "value" },
                            new String[]{ key, value });
                    table.put(data);
                }
            }
            return table;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public void setProperty(String group, String pid, String key, String value) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<String, Properties> configurationTable = clusterManager.getMap(Constants.CONFIGURATION_MAP + Configurations.SEPARATOR + group);
            Properties properties = configurationTable.get(pid);
            if (properties == null) {
                properties = new Properties();
            }
            properties.put(key, value);
            configurationTable.put(pid, properties);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

}
