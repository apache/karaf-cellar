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

/**
 * Cellar Group MBean to manipule Cellar cluster group.
 */
public interface CellarGroupMBean {

    // Operations
    void create(String name) throws Exception;
    void delete(String name) throws Exception;
    void join(String name, String nodeId) throws Exception;
    void quit(String name, String nodeId) throws Exception;

    // Attributes
    TabularData getGroups() throws Exception;

}
