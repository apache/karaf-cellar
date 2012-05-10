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
 * MBean interface describing the operations and attributes on a Cellar feature.
 */
public interface CellarFeaturesMBean {

    void addUrl(String group, String url) throws Exception;
    void removeUrl(String group, String url) throws Exception;
    void install(String group, String name) throws Exception;
    void install(String group, String name, String version) throws Exception;
    void uninstall(String group, String name) throws Exception;
    void uninstall(String group, String name, String version) throws Exception;

    List<String> getUrls(String group) throws Exception;
    TabularData getFeatures(String group) throws Exception;

}
