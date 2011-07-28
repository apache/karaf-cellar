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

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.management.CellarNodeMBean;

import javax.management.openmbean.*;
import javax.management.openmbean.SimpleType;
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

    public JmxNode(Node node) {
        try {
            String[] itemNames = CellarNodeMBean.NODE;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = node.getId();
            itemValues[1] = node.getHost();
            itemValues[2] = node.getPort();
            data = new CompositeDataSupport(NODE, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot create node open data", e);
        }
    }

    private static CompositeType createNodeType() {
        try {
            String desc = "This type describes Karaf Cellar nodes";
            String[] itemNames = CellarNodeMBean.NODE;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] descriptions = new String[itemNames.length];

            itemTypes[0] = SimpleType.STRING;
            descriptions[0] = "The ID of the Cellar node";

            itemTypes[1] = SimpleType.STRING;
            descriptions[1] = "The hostname of the Cellar node";

            itemTypes[2] = SimpleType.INTEGER;
            descriptions[2] = "The port number of the Cellar node";

            return new CompositeType("Node", desc, itemNames, descriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build node type", e);
        }
    }

    private static TabularType createNodeTableType() {
        try {
            return new TabularType("Nodes", "Table of all Karaf Cellar nodes", NODE, new String[] {CellarNodeMBean.NODE_ID});
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build node table type", e);
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
