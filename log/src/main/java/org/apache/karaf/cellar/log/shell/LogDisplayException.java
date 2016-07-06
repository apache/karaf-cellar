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

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllNodeCompleter;
import org.apache.karaf.cellar.log.ClusterLogKey;
import org.apache.karaf.cellar.log.ClusterLogRecord;
import org.apache.karaf.cellar.log.LogAppender;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.Map;

@Command(scope = "cluster", name = "log-exception-display", description = "Display the last occurred exception from the cluster log")
@Service
public class LogDisplayException extends CellarCommandSupport {

    @Argument(index = 0, name = "logger", description = "The name of the logger. This can be ROOT, ALL, or specific logger", required = false, multiValued = false)
    String logger;

    @Argument(index = 1, name = "node", description = "The node ID or alias", required = false, multiValued = false)
    @Completion(AllNodeCompleter.class)
    String nodeIdOrAlias;

    @Override
    public Object doExecute() throws Exception {
        String nodeId = null;
        if (nodeIdOrAlias != null) {
            Node node = clusterManager.findNodeByIdOrAlias(nodeIdOrAlias);
            if (node == null) {
                System.err.println("Node " + nodeIdOrAlias + " doesn't exist");
                return null;
            }
            nodeId = node.getId();
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<ClusterLogKey, ClusterLogRecord> clusterLog = clusterManager.getMap(LogAppender.LOG_MAP);
            for (ClusterLogKey key : clusterLog.keySet()) {
                ClusterLogRecord record = clusterLog.get(key);
                if (record.getThrowableStringRep() != null && record.getThrowableStringRep().length > 0) {
                    if (nodeId == null || (nodeId != null && key.getNodeId().equals(nodeId))) {
                        if (logger == null || (logger != null && record.getLoggerName().contains(logger))) {
                            for (String throwable : record.getThrowableStringRep()) {
                                System.out.println(throwable);
                            }
                            System.out.println();
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
