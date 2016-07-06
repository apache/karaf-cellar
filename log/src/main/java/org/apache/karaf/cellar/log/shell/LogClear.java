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

@Command(scope = "cluster", name = "log-clear", description = "Clean the cluster log messages")
@Service
public class LogClear extends CellarCommandSupport {

    @Argument(index = 0, name = "nodeIdOrAlias", description = "The node ID or alias", required = false, multiValued = false)
    @Completion(AllNodeCompleter.class)
    String nodeIdOrAlias;

    @Override
    public Object doExecute() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<ClusterLogKey, ClusterLogRecord> clusterLog = clusterManager.getMap(LogAppender.LOG_MAP);
            if (nodeIdOrAlias == null) {
                clusterLog.clear();
            } else {
                Node node = clusterManager.findNodeByIdOrAlias(nodeIdOrAlias);
                if (node == null) {
                    System.err.println("Node " + nodeIdOrAlias + " doesn't exist");
                    return null;
                }
                for (ClusterLogKey key : clusterLog.keySet()) {
                    if (key.getNodeId().equals(node.getId())) {
                        clusterLog.remove(key);
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

}
