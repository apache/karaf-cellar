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
 * Config interface describing the operations and attributes available on a Cellar configuration.
 */
public interface CellarConfigMBean {

    List<String> listConfig(String group) throws Exception;
    void deleteConfig(String group, String pid) throws Exception;
    TabularData listProperties(String group, String pid) throws Exception;
    void setProperty(String group, String pid, String key, String value) throws Exception;
    void appendProperty(String group, String pid, String key, String value) throws Exception;
    void deleteProperty(String group, String pid, String key) throws Exception;

}
