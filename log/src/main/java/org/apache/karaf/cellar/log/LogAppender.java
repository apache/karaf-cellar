/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.log;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import java.util.Map;

public class LogAppender implements PaxAppender {

    public static final String LOG_MAP = "org.apache.karaf.cellar.log";

    private ClusterManager clusterManager;

    @Override
    public void doAppend(PaxLoggingEvent event) {

        Map<ClusterLogKey, ClusterLogRecord> clusterLog = clusterManager.getMap(LOG_MAP);

        ClusterLogRecord record = new ClusterLogRecord();
        ClusterLogKey key = new ClusterLogKey();

        Node node = clusterManager.getNode();
        key.setNodeId(node.getId());
        key.setNodeAlias(node.getAlias());
        key.setTimeStamp(event.getTimeStamp());
        key.setId(clusterManager.generateId());

        record.setFQNOfLoggerClass(event.getFQNOfLoggerClass());
        record.setLevel(event.getLevel().toString());
        record.setLoggerName(event.getLoggerName());
        record.setMessage(event.getMessage());
        record.setRenderedMessage(event.getRenderedMessage());
        record.setThreadName(event.getThreadName());
        record.setThrowableStringRep(event.getThrowableStrRep());

        clusterLog.put(key, record);
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

}
