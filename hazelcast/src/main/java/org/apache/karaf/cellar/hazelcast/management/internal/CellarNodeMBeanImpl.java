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
package org.apache.karaf.cellar.hazelcast.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.management.CellarNodeMBean;
import org.apache.karaf.cellar.utils.ping.Ping;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.*;

/**
 * Implementation of the Cellar Node MBean.
 */
public class CellarNodeMBeanImpl extends StandardMBean implements CellarNodeMBean {

    private ClusterManager clusterManager;
    private ExecutionContext executionContext;

    public CellarNodeMBeanImpl() throws NotCompliantMBeanException {
        super(CellarNodeMBean.class);
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    @Override
    public long pingNode(String nodeId) throws Exception {
        Node node = clusterManager.findNodeById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Cluster group " + nodeId + " doesn't exist");
        }
        Long start = System.currentTimeMillis();
        Ping ping = new Ping(clusterManager.generateId());
        ping.setDestination(new HashSet(Arrays.asList(node)));
        executionContext.execute(ping);
        Long stop = System.currentTimeMillis();
        return (stop - start);
    }

    @Override
    public TabularData getNodes() throws Exception {

        CompositeType nodeType = new CompositeType("Node", "Karaf Cellar cluster node",
                new String[]{ "id", "hostname", "port", "local" },
                new String[]{ "ID of the node", "Hostname of the node", "Port number of the node", "Flag defining if the node is local" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.BOOLEAN });

        TabularType tableType = new TabularType("Nodes", "Table of all Karaf Cellar nodes", nodeType, new String[]{ "id" });

        TabularData table = new TabularDataSupport(tableType);

        Set<Node> nodes = clusterManager.listNodes();

        for (Node node : nodes) {
            boolean local = (node.equals(clusterManager.getNode()));
            CompositeData data = new CompositeDataSupport(nodeType,
                    new String[]{ "id", "hostname", "port", "local" },
                    new Object[]{ node.getId(), node.getHost(), node.getPort(), local });
            table.put(data);
        }

        return table;
    }

}
