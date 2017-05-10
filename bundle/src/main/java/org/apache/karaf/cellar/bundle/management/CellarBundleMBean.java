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
     * Install a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param location the bundle location.
     * @param start true to start the bundle, false else.
     * @throws Exception in case of install failure.
     */
    void install(String group, String location, boolean start) throws Exception;

    /**
     * Install and eventually start a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param location the bundle location.
     * @param level the bundle start level.
     * @param start true to start the bundle, false else.
     * @throws Exception
     */
    void install(String group, String location, Integer level, boolean start) throws Exception;

    /**
     * Uninstall a bundle from a cluster group.
     *
     * @param group the cluster group name.
     * @param id the bundle id.
     * @throws Exception in case of uninstall failure.
     */
    void uninstall(String group, String id) throws Exception;

    /**
     * Start a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param id the bundle id.
     * @throws Exception in case of start failure.
     */
    void start(String group, String id) throws Exception;

    /**
     * Stop a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param id the bundle id.
     * @throws Exception in case of stop failure.
     */
    void stop(String group, String id) throws Exception;

    /**
     * Update a bundle in a cluster group.
     *
     * @param group the cluster group name.
     * @param id the bundle id.
     * @throws Exception in case of update failure.
     */
    void update(String group, String id) throws Exception;

    /**
     * Update a bundle in a cluster group using a given location.
     *
     * @param group the cluster group name.
     * @param id the bundle id.
     * @param location the update bundle location.
     * @throws Exception in case of update failure.
     */
    void update(String group, String id, String location) throws Exception;

    /**
     * Updating blocking policy for a given bundle pattern.
     *
     * @param group the cluster group name where to apply the blocking policy.
     * @param pattern the bundle pattern.
     * @param whitelist true to allow bundle by updating the whitelist.
     * @param blacklist true to block bundle by updating the blacklist.
     * @param in true to update the inbound policy.
     * @param out true to update the outbound policy.
     * @throws Exception
     */
    void block(String group, String pattern, boolean whitelist, boolean blacklist, boolean in, boolean out) throws Exception;

    /**
     * Get the bundles in a cluster group.
     *
     * @param group the cluster group name.
     * @return the list of bundles in the cluster group.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getBundles(String group) throws Exception;

}
