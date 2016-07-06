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
package org.apache.karaf.cellar.log.management;

import javax.management.openmbean.TabularData;
import java.util.List;

/**
 * Describe the operations and attributes on the CellarLogMBean.
 */
public interface CellarLogMBean {

    /**
     * Display the cluster log messages.
     *
     * @param logger The logger name to filter or null.
     * @param nodeId The node ID or alias to filter or null for all nodes.
     * @param entries The number of entries to display limit (0 means no limit).
     * @return The list of log messages.
     */
    List<String> displayLog(String logger, String nodeId, int entries);

    /**
     * Add a log message on the cluster.
     *
     * @param message The message to log.
     * @param level The log message level (default is INFO when null).
     * @param nodeId The node ID or alias or null for all nodes.
     */
    void logMessage(String message, String level, String nodeId);

    /**
     * Set the log level on the cluster.
     *
     * @param logger The logger to set the level or null for the root logger.
     * @param level The level to set.
     * @param nodeId The node ID or alias or null for all nodes.
     */
    void setLevel(String level, String logger, String nodeId) throws Exception;

    /**
     * Get the log level on the cluster.
     *
     * @param logger The logger to filter or null for all loggers.
     * @param nodeId The node ID or alias or null for all nodes.
     * @return A tabular data reprepsenting the levels.
     */
    TabularData getLevel(String logger, String nodeId) throws Exception;

}
