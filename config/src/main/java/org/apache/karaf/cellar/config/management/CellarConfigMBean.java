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
package org.apache.karaf.cellar.config.management;

import javax.management.openmbean.TabularData;
import java.util.List;
import java.util.Map;

/**
 * Describe the operations and attributes available on the Cellar configuration MBean.
 */
public interface CellarConfigMBean {

    /**
     * Get the list of configuration PID in a cluster group.
     *
     * @param group the cluster group name.
     * @return the list of configuration PID.
     * @throws Exception in case of retrieval failure.
     */
    List<String> getConfigs(String group) throws Exception;

    /**
     * Delete a configuration from a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID to delete.
     * @throws Exception in case of deletion failure.
     */
    void delete(String group, String pid) throws Exception;

    /**
     * List the properties of a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @return the list of properties for the configuration.
     * @throws Exception in case of retrieval failure.
     */
    Map<String, String> listProperties(String group, String pid) throws Exception;

    /**
     * Set the value of a property for a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the property value.
     * @throws Exception in case of set failure.
     */
    void setProperty(String group, String pid, String key, String value) throws Exception;

    /**
     * Append String at the end of the value of a property for a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @param key the property key.
     * @param value the property value.
     * @throws Exception in case of append failure.
     */
    void appendProperty(String group, String pid, String key, String value) throws Exception;

    /**
     * Delete a property for a configuration in a cluster group.
     *
     * @param group the cluster group name.
     * @param pid the configuration PID.
     * @param key the property key.
     * @throws Exception in case of delete failure.
     */
    void deleteProperty(String group, String pid, String key) throws Exception;

    /**
     * Get the list of config properties excluded from the sync.
     *
     * @return the properties excluded from the sync.
     * @throws Exception
     */
    String getExcludedProperties() throws Exception;

    /**
     * Define the list of config properties excluded from the sync.
     *
     * @param excludedProperties the properties excluded from the sync.
     * @throws Exception
     */
    void setExcludedProperties(String excludedProperties) throws Exception;

    /**
     * Change the blocking policy for the given config PID pattern.
     *
     * @param groupName the cluster group name.
     * @param pid the config PID pattern.
     * @param whitelist true to allow the PID pattern by updating the whitelist.
     * @param blacklist true to block the PID pattern by updating the blacklist.
     * @param in true to change inbound blocking policy.
     * @param out true to change outbound blocking policy.
     * @throws Exception
     */
    void block(String groupName, String pid, boolean whitelist, boolean blacklist, boolean in, boolean out) throws Exception;

}
