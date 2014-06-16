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
package org.apache.karaf.cellar.bundle.management;

import javax.management.openmbean.TabularData;

/**
 * Describe the operations and attributes on the Cellar bundle MBean.
 */
public interface CellarBundleMBean {

    /**
     * Install a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param location the bundle location.
     * @throws Exception in case of install failure.
     */
    void install(String group, String location) throws Exception;

    /**
     * Uninstall a bundle from a cluster group.
     *
     * @param group the cluster group name.
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws Exception in case of uninstall failure.
     */
    void uninstall(String group, String symbolicName, String version) throws Exception;

    /**
     * Start a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws Exception in case of start failure.
     */
    void start(String group, String symbolicName, String version) throws Exception;

    /**
     * Stop a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws Exception in case of stop failure.
     */
    void stop(String group, String symbolicName, String version) throws Exception;

    /**
     * Get the bundles in a cluster group.
     *
     * @param group the cluster group name.
     * @return the list of bundles in the cluster group.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getBundles(String group) throws Exception;

}
