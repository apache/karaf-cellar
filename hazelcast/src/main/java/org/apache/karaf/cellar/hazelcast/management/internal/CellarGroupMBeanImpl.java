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
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.control.ManageGroupAction;
import org.apache.karaf.cellar.core.control.ManageGroupCommand;
import org.apache.karaf.cellar.core.management.CellarGroupMBean;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the Cellar Group MBean;
 */
public class CellarGroupMBeanImpl extends StandardMBean implements CellarGroupMBean {

    private ClusterManager clusterManager;
    private ExecutionContext executionContext;
    private GroupManager groupManager;

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

    public GroupManager getGroupManager() {
        return this.groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public CellarGroupMBeanImpl() throws NotCompliantMBeanException {
        super(CellarGroupMBean.class);
    }

    @Override
    public void create(String name) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(name);
        if (group != null) {
            throw new IllegalArgumentException("Cluster group " + name + " already exists");
        }
        groupManager.createGroup(name);
    }

    @Override
    public void delete(String name) throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Group g = groupManager.findGroupByName(name);
            List<String> nodes = new LinkedList<String>();

            if (g.getNodes() != null && !g.getNodes().isEmpty()) {
                for (Node n : g.getNodes()) {
                    nodes.add(n.getId());
                }
                ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
                command.setAction(ManageGroupAction.QUIT);
                command.setGroupName(name);
                Set<Node> recipientList = clusterManager.listNodes(nodes);
                command.setDestination(recipientList);
                executionContext.execute(command);
            }

            groupManager.deleteGroup(name);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Override
    public void join(String groupName, String nodeIdOrAlias) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        Node node = clusterManager.findNodeByIdOrAlias(nodeIdOrAlias);
        if (node == null) {
            throw new IllegalArgumentException("Cluster node " + nodeIdOrAlias + " doesn't exist");
        }

        Set<Node> nodes = new HashSet<Node>();
        nodes.add(node);

        ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
        command.setAction(ManageGroupAction.JOIN);
        command.setGroupName(groupName);
        command.setDestination(nodes);

        executionContext.execute(command);
    }

    @Override
    public void quit(String groupName, String nodeIdOrAlias) throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        Node node = clusterManager.findNodeByIdOrAlias(nodeIdOrAlias);
        if (node == null) {
            throw new IllegalArgumentException("Cluster node " + nodeIdOrAlias + " doesn't exist");
        }

        Set<Node> nodes = new HashSet<Node>();
        nodes.add(node);

        ManageGroupCommand command = new ManageGroupCommand(clusterManager.generateId());
        command.setAction(ManageGroupAction.QUIT);
        command.setGroupName(groupName);
        command.setDestination(nodes);
        executionContext.execute(command);
    }

    @Override
    public TabularData getGroups() throws Exception {
        Set<Group> allGroups = groupManager.listAllGroups();

        CompositeType groupType = new CompositeType("Group", "Karaf Cellar cluster group",
                new String[]{ "name", "members"},
                new String[]{ "Name of the cluster group", "Members of the cluster group" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING });

        TabularType tableType = new TabularType("Groups", "Table of all Karaf Cellar groups", groupType,
                new String[]{ "name" });

        TabularData table = new TabularDataSupport(tableType);

        for (Group group : allGroups) {
            StringBuffer members = new StringBuffer();
            for (Node node : group.getNodes()) {
                // display only up and running nodes in the cluster
                if (clusterManager.findNodeById(node.getId()) != null) {
                    if (node.getAlias() != null) {
                        members.append(node.getAlias());
                    } else {
                        members.append(node.getId());
                    }
                    members.append(" ");
                }
            }
            CompositeData data = new CompositeDataSupport(groupType,
                    new String[]{ "name", "members" },
                    new Object[]{ group.getName(), members.toString() });
            table.put(data);
        }

        return table;
    }

}
