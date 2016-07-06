/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.log.shell;

import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllNodeCompleter;
import org.apache.karaf.cellar.log.ClusterLogKey;
import org.apache.karaf.cellar.log.ClusterLogRecord;
import org.apache.karaf.cellar.log.LogAppender;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Command(scope = "cluster", name = "log-display", description = "Display the cluster log messages")
@Service
public class LogDisplay extends CellarCommandSupport {

    @Argument(index = 0, name = "logger", description = "The logger name", required = false, multiValued = false)
    String logger;

    @Argument(index = 1, name = "node", description = "The node ID or alias", required = false, multiValued = false)
    @Completion(AllNodeCompleter.class)
    String nodeIdOrAlias;

    @Option(name = "-n", description = "Number of entries to display", required = false, multiValued = false)
    int entries;

    @Override
    public Object doExecute() throws Exception {
        String nodeId = null;
        if (nodeIdOrAlias != null && clusterManager.findNodeByIdOrAlias(nodeIdOrAlias) == null) {
            System.err.println("Node " + nodeIdOrAlias + " doesn't exist");
            return null;
        }
        if (nodeIdOrAlias != null && clusterManager.findNodeByIdOrAlias(nodeIdOrAlias) != null) {
            nodeId = clusterManager.findNodeByIdOrAlias(nodeIdOrAlias).getId();
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<ClusterLogKey, ClusterLogRecord> clusterLog = clusterManager.getMap(LogAppender.LOG_MAP);
            int index = 0;
            for (ClusterLogKey key : clusterLog.keySet()) {
                if (entries == 0 || (entries != 0 && index < entries)) {
                    ClusterLogRecord record = clusterLog.get(key);
                    if (nodeId == null || (nodeId != null && key.getNodeId().equals(nodeId))) {
                        if (logger == null || (logger != null && logger.equals("ALL")) || (logger != null && record.getLoggerName() != null && record.getLoggerName().contains(logger))) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                            System.out.println(key.getNodeId() + " | "
                                    + dateFormat.format(new Date(key.getTimeStamp())) + " | "
                                    + record.getLevel() + " | "
                                    + record.getThreadName() + " | "
                                    + record.getLoggerName() + " | "
                                    + record.getMessage());
                            index++;
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

}
