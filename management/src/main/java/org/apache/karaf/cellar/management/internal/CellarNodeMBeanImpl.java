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
package org.apache.karaf.cellar.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.management.CellarNodeMBean;
import org.apache.karaf.cellar.management.codec.JmxNode;
import org.apache.karaf.cellar.utils.ping.Ping;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.TabularData;
import java.util.*;

/**
 * Implementation of the Cellar Node MBean to manipulate Cellar cluster nodes.
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

    public long pingNode(String nodeId) throws Exception {
        Node node = clusterManager.findNodeById(nodeId);
        Long start = System.currentTimeMillis();
        Ping ping = new Ping(clusterManager.generateId());
        ping.setDestination(new HashSet(Arrays.asList(node)));
        executionContext.execute(ping);
        Long stop = System.currentTimeMillis();
        return (stop - start);
    }

    public TabularData getNodes() throws Exception {
        Set<Node> allNodes = clusterManager.listNodes();
        List<JmxNode> nodes = new ArrayList<JmxNode>();
        for (Node node : allNodes) {
            nodes.add(new JmxNode(node, clusterManager));
        }
        TabularData table = JmxNode.tableFrom(nodes);
        return table;
    }

}
