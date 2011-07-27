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
 *  Cellar Node MBean to manipulate Cellar cluster nodes.
 */
public interface CellarNodeMBean {

    String NODE_NAME = "Name";
    String NODE_IS_LOCAL = "Is Local";

    String[] NODE = { NODE_NAME, NODE_IS_LOCAL };

    // Operations
    int pingNode(String name) throws Exception;

    // Attributes
    TabularData getNodes() throws Exception;

}
