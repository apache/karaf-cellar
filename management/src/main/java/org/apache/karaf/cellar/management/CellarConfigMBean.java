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
package org.apache.karaf.cellar.management;

import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * Describe the operations and attributes of the Cellar Config MBean.
 */
public interface CellarConfigMBean {

    /**
     * Get the configurations in a cluster group.
     *
     * @param group the cluster group name.
     * @return the list of configurations.
     * @throws Exception in case of retrieval failure.
     */
    List<String> listConfig(String group) throws Exception;

    /**
     * Delete a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @throws Exception in case of delete failure.
     */
    void deleteConfig(String group, String pid) throws Exception;

    /**
     * Get the properties of a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @return the list of properties.
     * @throws Exception in case of retrieval failure.
     */
    TabularData listProperties(String group, String pid) throws Exception;

    /**
     * Set the value of a property in a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the property value.
     * @throws Exception in case of set failure.
     */
    void setProperty(String group, String pid, String key, String value) throws Exception;

    /**
     * Append at the end of a value of a property in a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the value to append at the end of the property value.
     * @throws Exception in case of append failure.
     */
    void appendProperty(String group, String pid, String key, String value) throws Exception;

    /**
     * Delete a property in a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @param key the property key.
     * @throws Exception in case of delete failure.
     */
    void deleteProperty(String group, String pid, String key) throws Exception;

}
