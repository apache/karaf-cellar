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
package org.apache.karaf.cellar.management.codec;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.management.CellarNodeMBean;

import javax.management.openmbean.*;
import java.util.List;

/**
 * JMX representation of a Cellar cluster Node.
 */
public class JmxNode {

    static final CompositeType NODE;
    static final TabularType NODE_TABLE;

    static {
        NODE = createNodeType();
        NODE_TABLE = createNodeTableType();
    }

    private final CompositeDataSupport data;

    private CompositeData asCompositeData() {
        return data;
    }

    public JmxNode(Node node, ClusterManager clusterManager) {
        try {
            String[] itemNames = CellarNodeMBean.NODE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = node.getId();
            if (node.getHost().equals(clusterManager.getNode())) {
                itemValues[1] = true;
            } else {
                itemValues[1] = false;
            }
            data = new CompositeDataSupport(NODE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot create instance open data", e);
        }
    }

    private static CompositeType createNodeType() {
        try {
            String desc = "This type describes Karaf Cellar nodes";
            String[] itemNames = CellarNodeMBean.NODE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] descriptions = new String[itemNames.length];

            itemTypes[0] = SimpleType.STRING;
            descriptions[0] = "The name of the Cellar node";

            itemTypes[1] = SimpleType.BOOLEAN;
            descriptions[1] = "Whether the Cellar node is the local one or not";

            return new CompositeType("Node", desc, itemNames, descriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build instance type", e);
        }
    }

    private static TabularType createNodeTableType() {
        try {
            return new TabularType("Nodes", "Table of all Karaf Cellar nodes", NODE, new String[] {CellarNodeMBean.NODE_NAME});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build node table tyep", e);
        }
    }

    public static TabularData tableFrom(List<JmxNode> nodes) {
        TabularDataSupport table = new TabularDataSupport(NODE_TABLE);
        for (JmxNode node : nodes) {
            table.put(node.asCompositeData());
        }
        return table;
    }

}
