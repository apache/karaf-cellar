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
package org.apache.karaf.cellar.features.management;

import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * Describe the operations and attributes on the Cellar Features MBean.
 */
public interface CellarFeaturesMBean {

    /**
     * Add a features repository in a cluster group.
     *
     * @param group the cluster group name.
     * @param nameOrUrl the features repository name or URL.
     * @throws Exception in case of add failure.
     */
    void addRepository(String group, String nameOrUrl) throws Exception;

    /**
     * Add a features repository in a cluster group.
     *
     * @param group the cluster group name.
     * @param nameOrUrl the features repository name or URL.
     * @param version the features repository version (when name is provided).
     * @throws Exception in case of add failure.
     */
    void addRepository(String group, String nameOrUrl, String version) throws Exception;

    /**
     * Add a features repository in a cluster group.
     *
     * @param group the cluster group name.
     * @param nameOrUrl the features repository URL.
     * @param version the features repository version (when name is provided).
     * @param install true to install all features contained in the repository, false else.
     * @throws Exception in case of add failure.
     */
    void addRepository(String group, String nameOrUrl, String version, boolean install) throws Exception;

    /**
     * Remove a features repository from a cluster group.
     *
     * @param group the cluster group name.
     * @param repository the features repository name or URL.
     * @throws Exception in case of remove failure.
     */
    void removeRepository(String group, String repository) throws Exception;

    /**
     * Remove a features repository from a cluster group, eventually uninstalling all features described in the repository.
     *
     * @param group the cluster group name.
     * @param repository the features repository name or URL.
     * @param uninstall true to uninstall all features described in the repository URL.
     * @throws Exception
     */
    void removeRepository(String group, String repository, boolean uninstall) throws Exception;

    /**
     * Refresh a features repository in a cluster group.
     *
     * @param group the cluster group name.
     * @param repository the features repository name or URL.
     * @throws Exception
     */
    void refreshRepository(String group, String repository) throws Exception;

    /**
     * Install a feature in a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @throws Exception in case of install failure.
     */
    void installFeature(String group, String name) throws Exception;

    /**
     * Install a feature in a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @param noRefresh true to not automatically refresh the bundles, false else.
     * @param noStart true to not automatically start the bundles, false else.
     * @param noManage true to not automatically manage the bundles, false else.
     * @param upgrade true to upgrade an existing feature or install it, false else.
     * @throws Exception in case of install failure.
     */
    void installFeature(String group, String name, boolean noRefresh, boolean noStart, boolean noManage, boolean upgrade) throws Exception;

    /**
     * Install a feature in a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @param version the feature version.
     * @throws Exception in case of install failure.
     */
    void installFeature(String group, String name, String version) throws Exception;

    /**
     * Install a feature in a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @param version the feature version.
     * @param noRefresh true to not automatically refresh the bundles, false else.
     * @param noStart true to not automatically start the bundles, false else.
     * @param noManage true to not automatically manage the bundles, false else.
     * @param upgrade true to upgrade an existing feature or install it, false else.
     * @throws Exception in case of install failure.
     */
    void installFeature(String group, String name, String version, boolean noRefresh, boolean noStart, boolean noManage, boolean upgrade) throws Exception;

    /**
     * Uninstall a feature from a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @throws Exception in case of uninstall failure.
     */
    void uninstallFeature(String group, String name) throws Exception;

    /**
     * Uninstall a feature from a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @param noRefresh true to not automatically refresh the bundles, false else.
     * @throws Exception in case of uninstall failure.
     */
    void uninstallFeature(String group, String name, boolean noRefresh) throws Exception;

    /**
     * Uninstall a feature from a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @param version the feature version.
     * @throws Exception in case of uninstall failure.
     */
    void uninstallFeature(String group, String name, String version) throws Exception;

    /**
     * Uninstall a feature from a cluster group.
     *
     * @param group the cluster group name.
     * @param name the feature name.
     * @param version the feature version.
     * @param noRefresh true to not automatically refresh the bundles, false else.
     * @throws Exception in case of uninstall failure.
     */
    void uninstallFeature(String group, String name, String version, boolean noRefresh) throws Exception;

    /**
     * Change the blocking policy for a feature pattern.
     *
     * @param group the cluster group name.
     * @param pattern the feature pattern.
     * @param whitelist true to allow the feature by updating the whitelist.
     * @param blacklist true to block the feature by updating the blacklist
     * @param in true to change the inbound blocking policy.
     * @param out true to change the outbound blocking policy.
     * @throws Exception
     */
    void block(String group, String pattern, boolean whitelist, boolean blacklist, boolean in, boolean out) throws Exception;

    /**
     * Get the list of features repository URLs in a cluster group.
     *
     * @param group the cluster group name.
     * @return the list of features repository URLs.
     * @throws Exception in case of retrieval failure.
     */
    List<String> getRepositories(String group) throws Exception;

    /**
     * Get the list of features in a cluster group.
     *
     * @param group the cluster group name.
     * @return the list of features.
     * @throws Exception in case of retrieval failure.
     */
    TabularData getFeatures(String group) throws Exception;

}
