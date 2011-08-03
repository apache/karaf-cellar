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
import org.apache.karaf.cellar.core.control.*;
import org.apache.karaf.cellar.management.CellarMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the Cellar core MBean.
 */
public class CellarMBeanImpl extends StandardMBean implements CellarMBean {

    private ClusterManager clusterManager;
    private ExecutionContext executionContext;

    public CellarMBeanImpl() throws NotCompliantMBeanException {
        super(CellarMBean.class);
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

    public TabularData handlerStatus() throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());
        command.setHandlesName(null);
        command.setStatus(null);

        Map<Node, ManageHandlersResult> results = executionContext.execute(command);

        CompositeType compositeType = new CompositeType("Event Handler", "Karaf Cellar cluster event handler",
                new String[]{ "node", "handler", "status" },
                new String[]{ "Node hosting event handler", "Name of the event handler", "Current status of the event handler" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });
        TabularType tableType = new TabularType("Event Handlers", "Table of Karaf Cellar cluster event handlers",
                compositeType, new String[]{ "node", "handler" });
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Map.Entry<Node, ManageHandlersResult> handlersResultEntry : results.entrySet()) {
            Node node = handlersResultEntry.getKey();
            ManageHandlersResult result = handlersResultEntry.getValue();
            if (result != null && result.getHandlers() != null) {
                for (Map.Entry<String, String> handlerEntry : result.getHandlers().entrySet()) {
                    String handler = handlerEntry.getKey();
                    String status = handlerEntry.getValue();
                    CompositeDataSupport data = new CompositeDataSupport(compositeType,
                            new String[]{ "node", "handler", "status" },
                            new Object[]{ node.getId(), handler, status });
                    table.put(data);
                }
            }
        }

        return table;
    }

    public void handlerStart(String handlerId, String nodeId) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());
        List<String> nodeIds = new ArrayList<String>();
        nodeIds.add(nodeId);
        Set<Node> nodes = clusterManager.listNodes(nodeIds);
        command.setHandlesName(handlerId);
        command.setDestination(nodes);
        command.setStatus(Boolean.TRUE);
    }

    public void handlerStop(String handlerId, String nodeId) throws Exception {
        ManageHandlersCommand command = new ManageHandlersCommand(clusterManager.generateId());
        List<String> nodeIds = new ArrayList<String>();
        nodeIds.add(nodeId);
        Set<Node> nodes = clusterManager.listNodes(nodeIds);
        command.setHandlesName(handlerId);
        command.setDestination(nodes);
        command.setStatus(Boolean.FALSE);
    }

    public TabularData consumerStatus() throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());
        command.setStatus(null);

        Map<Node, ConsumerSwitchResult> results = executionContext.execute(command);

        CompositeType compositeType = new CompositeType("Event Consumer", "Karaf Cellar cluster event consumer",
                new String[]{ "node", "status" },
                new String[]{ "Node hosting event consumer", "Current status of the event consumer" },
                new OpenType[]{ SimpleType.STRING, SimpleType.BOOLEAN });
        TabularType tableType = new TabularType("Event Consumers", "Table of Karaf Cellar cluster event consumers",
                compositeType, new String[]{ "node" });
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Node node : results.keySet()) {
            ConsumerSwitchResult consumerSwitchResult = results.get(node);
            CompositeDataSupport data = new CompositeDataSupport(compositeType,
                    new String[]{ "node", "status" },
                    new Object[]{ node.getId(), consumerSwitchResult.getStatus() });
            table.put(data);
        }

        return table;
    }

    public void consumerStart(String node) throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());
        List<String> ids = new ArrayList<String>();
        ids.add(node);
        Set<Node> nodes = clusterManager.listNodes(ids);
        command.setDestination(nodes);
        command.setStatus(SwitchStatus.ON);
        executionContext.execute(command);
    }

    public void consumerStop(String node) throws Exception {
        ConsumerSwitchCommand command = new ConsumerSwitchCommand(clusterManager.generateId());
        List<String> ids = new ArrayList<String>();
        ids.add(node);
        Set<Node> nodes = clusterManager.listNodes(ids);
        command.setDestination(nodes);
        command.setStatus(SwitchStatus.OFF);
        executionContext.execute(command);
    }

    public TabularData producerStatus() throws Exception {
        ProducerSwitchCommand command = new ProducerSwitchCommand(clusterManager.generateId());
        command.setStatus(null);

        Map<Node, ProducerSwitchResult> results = executionContext.execute(command);

        CompositeType compositeType = new CompositeType("Event Producer", "Karaf Cellar cluster event producer",
                new String[]{ "node", "status" },
                new String[]{ "Node hosting event producer", "Current status of the event producer" },
                new OpenType[]{ SimpleType.STRING, SimpleType.BOOLEAN });
        TabularType tableType = new TabularType("Event Producers", "Table of Karaf Cellar cluster event producers",
                compositeType, new String[]{ "node" });
        TabularDataSupport table = new TabularDataSupport(tableType);

        for (Node node : results.keySet()) {
            ProducerSwitchResult producerSwitchResult = results.get(node);
            CompositeDataSupport data = new CompositeDataSupport(compositeType,
                    new String[]{ "node", "status" },
                    new Object[]{ node.getId(), producerSwitchResult.getStatus() });
            table.put(data);
        }

        return table;
    }

    public void producerStop(String node) throws Exception {
        ProducerSwitchCommand command = new ProducerSwitchCommand(clusterManager.generateId());
        List<String> ids = new ArrayList<String>();
        ids.add(node);
        Set<Node> nodes = clusterManager.listNodes(ids);
        command.setDestination(nodes);
        command.setStatus(SwitchStatus.OFF);
        executionContext.execute(command);
    }

    public void producerStart(String node) throws Exception {
        ProducerSwitchCommand command = new ProducerSwitchCommand(clusterManager.generateId());
        List<String> ids = new ArrayList<String>();
        ids.add(node);
        Set<Node> nodes = clusterManager.listNodes(ids);
        command.setDestination(nodes);
        command.setStatus(SwitchStatus.ON);
        executionContext.execute(command);
    }

}
