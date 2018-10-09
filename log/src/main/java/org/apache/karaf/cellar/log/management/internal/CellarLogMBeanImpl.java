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
package org.apache.karaf.cellar.log.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.log.*;
import org.apache.karaf.cellar.log.management.CellarLogMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CellarLogMBeanImpl extends StandardMBean implements CellarLogMBean {

    private ClusterManager clusterManager;
    private ExecutionContext executionContext;

    public CellarLogMBeanImpl() throws NotCompliantMBeanException {
        super(CellarLogMBean.class);
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public List<String> displayLog(String logger, String nodeId, int entries) {
        List<String> result = new ArrayList<String>();

        String node = null;
        if (nodeId != null && clusterManager.findNodeByIdOrAlias(nodeId) == null) {
            throw new IllegalArgumentException("Node " + nodeId + " doesn't exist");
        }
        if (nodeId != null && clusterManager.findNodeByIdOrAlias(nodeId) != null) {
            node = clusterManager.findNodeByIdOrAlias(nodeId).getId();
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            Map<ClusterLogKey, ClusterLogRecord> clusterLog = clusterManager.getMap(LogAppender.LOG_MAP);
            int index = 0;
            for (ClusterLogKey key : clusterLog.keySet()) {
                if (entries == 0 || (entries != 0 && index < entries)) {
                    ClusterLogRecord record = clusterLog.get(key);
                    if (node == null || (node != null && key.getNodeId().equals(node))) {
                        if (logger == null || (logger != null && logger.equals("ALL")) || (logger != null && record.getLoggerName() != null && record.getLoggerName().contains(logger))) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                            String message = key.getNodeId() + " | "
                                    + dateFormat.format(new Date(key.getTimeStamp())) + " | "
                                    + record.getLevel() + " | "
                                    + record.getThreadName() + " | "
                                    + record.getLoggerName() + " | "
                                    + record.getMessage();
                            result.add(message);
                            index++;
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return result;
    }

    @Override
    public void logMessage(String message, String nodeId, String level) {
        if (message == null) {
            throw new IllegalArgumentException("Message is required");
        }
        if (level != null && !level.equalsIgnoreCase("INFO") && !level.equalsIgnoreCase("DEBUG")
                && !level.equalsIgnoreCase("WARN") && !level.equalsIgnoreCase("ERROR")) {
            throw new IllegalArgumentException("Incorrect level value");
        }
        if (level == null) {
            level = "INFO";
        }
        if (nodeId != null && clusterManager.findNodeByIdOrAlias(nodeId) == null) {
            throw new IllegalArgumentException("Node " + nodeId + " doesn't exist");
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
    }

    @Override
    public void setLevel(String level, String logger, String nodeId) throws Exception {
        SetLogCommand command = new SetLogCommand(clusterManager.generateId());
        command.setTimeout(30 * 1000);

        Set<Node> recipientList = new HashSet<Node>();
        if (nodeId != null && clusterManager.findNodeByIdOrAlias(nodeId) == null) {
            throw new IllegalArgumentException("Node " + nodeId + " doesn't exist");
        }
        if (nodeId == null) {
            recipientList = clusterManager.listNodes();
        } else {
            recipientList.add(clusterManager.findNodeByIdOrAlias(nodeId));
        }

        if (recipientList.size() < 1)
            throw new IllegalArgumentException("No recipient list");

        command.setDestination(recipientList);
        command.setLogger(logger);
        command.setLevel(level);

        Map<Node, SetLogResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("No result received within given timeout");
        }
    }

    @Override
    public TabularData getLevel(String logger, String nodeId) throws Exception {
        if (nodeId != null && clusterManager.findNodeByIdOrAlias(nodeId) == null) {
            throw new IllegalArgumentException("Node " + nodeId + " doesn't exist");
        }

        Set<Node> recipientList = new HashSet<Node>();
        if (nodeId == null) {
            recipientList = clusterManager.listNodes();
        } else {
            recipientList.add(clusterManager.findNodeByIdOrAlias(nodeId));
        }

        CompositeType levelType = new CompositeType("Level", "Log Levels",
                new String[]{ "node", "logger", "level" },
                new String[]{ "Node ID", "Logger name", "Log level"},
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });

        TabularType tabularType = new TabularType("Levels", "Table of all log levels",
                levelType, new String[]{ "node", "logger" });
        TabularData tabularData = new TabularDataSupport(tabularType);

        if (recipientList.size() < 1)
            return null;

        GetLogCommand command = new GetLogCommand(clusterManager.generateId());
        command.setTimeout(30 * 1000);
        command.setDestination(recipientList);
        command.setLogger(logger);

        Map<Node, GetLogResult> results = executionContext.execute(command);
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("No result received within given timeout");
        } else {
            for (Node node : results.keySet()) {
                GetLogResult result = results.get(node);
                Map<String, String> loggers = result.getLoggers();
                String nodeName = node.getAlias();
                if (nodeName == null) {
                    nodeName = node.getId();
                }
                for (String l : loggers.keySet()) {
                    CompositeData data = new CompositeDataSupport(levelType,
                            new String[]{ "node", "logger", "level" },
                            new Object[]{ nodeName, l, loggers.get(l) });
                    tabularData.put(data);
                }
            }
        }

        return tabularData;
    }

}
