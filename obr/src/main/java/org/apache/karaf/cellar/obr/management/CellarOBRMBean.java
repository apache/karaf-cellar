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
package org.apache.karaf.cellar.obr.management;

import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * Describe the operations and attributes of the Cellar OBR MBean.
 */
public interface CellarOBRMBean {

    /**
     * List the OBR URLs in a cluster group.
     *
     * @param groupName the cluster group name.
     * @return the list of OBR URLs.
     * @throws Exception in case of retrieval failure.
     */
    List<String> getUrls(String groupName) throws Exception;

    /**
     * List the OBR bundles in a cluster group.
     *
     * @param groupName the cluster group name.
     * @return the list of OBR bundles.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getBundles(String groupName) throws Exception;

    /**
     * Add an OBR URL in a cluster group.
     *
     * @param groupName the cluster group name.
     * @param url the OBR URL.
     * @throws Exception in case of add failure.
     */
    void addUrl(String groupName, String url) throws Exception;

    /**
     * Remove an OBR URL from a cluster group.
     *
     * @param groupName the cluster group name.
     * @param url the OBR URL.
     * @throws Exception in case of remove failure.
     */
    void removeUrl(String groupName, String url) throws Exception;

    /**
     * Deploy an OBR bundle in a cluster group.
     *
     * @param groupName the cluster group name.
     * @param bundleId the bundle ID.
     * @throws Exception in case of deploy failure.
     */
    void deployBundle(String groupName, String bundleId) throws Exception;

    /**
     * Deploy an OBR bundle in a cluster group.
     *
     * @param groupName the cluster group name.
     * @param bundleId the bundle ID.
     * @param start true to start the bundle, false else.
     * @param deployOptional true to set the deployment as optional, false else.
     * @throws Exception
     */
    void deployBundle(String groupName, String bundleId, boolean start, boolean deployOptional) throws Exception;

}
