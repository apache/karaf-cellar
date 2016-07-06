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
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.Map;

@Command(scope = "cluster", name = "log-log", description = "Add a message in the cluster log")
@Service
public class LogMessage extends CellarCommandSupport {

    @Argument(index = 0, name = "message", description = "The message to log", required = true, multiValued = false)
    String message;

    @Argument(index = 1, name = "node", description = "The node ID or alias", required = false, multiValued = false)
    @Completion(AllNodeCompleter.class)
    String nodeId;

    @Option(name = "--level", aliases = { "-l" }, description = "The level the message will be logged at", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "DEBUG", "INFO", "WARN", "ERROR" })
    String level = "INFO";

    public Object doExecute() throws Exception {
        if (nodeId != null && clusterManager.findNodeByIdOrAlias(nodeId) == null) {
            System.err.println("Node " + nodeId + " doesn't exist");
            return null;
        }

        if (nodeId == null) {
            nodeId = clusterManager.getNode().getId();
        }
        long timestamp = System.currentTimeMillis();
        String id = clusterManager.generateId();

        ClusterLogKey key = new ClusterLogKey();
        key.setNodeId(nodeId);
        key.setTimeStamp(timestamp);
        key.setId(id);

        ClusterLogRecord record = new ClusterLogRecord();
        record.setMessage(message);
        record.setLevel(level);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<ClusterLogKey, ClusterLogRecord> clusterLog = clusterManager.getMap(LogAppender.LOG_MAP);
            clusterLog.put(key, record);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

}
