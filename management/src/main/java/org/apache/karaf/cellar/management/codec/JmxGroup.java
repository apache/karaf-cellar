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

import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.management.CellarGroupMBean;

import javax.management.openmbean.*;
import java.util.List;

/**
 * JMX representation of a Cellar cluster group.
 */
public class JmxGroup {

    static final CompositeType GROUP;
    static final TabularType GROUP_TABLE;

    static {
        GROUP = createGroupType();
        GROUP_TABLE = createGroupTableType();
    }

    private final CompositeDataSupport data;

    private CompositeData asCompositeData() {
        return data;
    }

    public JmxGroup(Group group) {
        try {
            String[] itemNames = CellarGroupMBean.GROUP;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = group.getName();
            StringBuffer buffer = new StringBuffer();
            for (Node node : group.getMembers()) {
                buffer.append(node.getId());
                buffer.append(" ");
            }
            itemValues[1] = buffer.toString();
            data = new CompositeDataSupport(GROUP, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot create group open data", e);
        }
    }

    private static CompositeType createGroupType() {
        try {
            String desc = "This type describes Karaf Cellar groups";
            String[] itemNames = CellarGroupMBean.GROUP;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] descriptions = new String[itemNames.length];

            itemTypes[0] = SimpleType.STRING;
            descriptions[0] = "The name of the Cellar group";

            itemTypes[1] = SimpleType.STRING;
            descriptions[1] = "The members of the Cellar group";

            return new CompositeType("Group", desc, itemNames, descriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build group", e);
        }
    }

    private static TabularType createGroupTableType() {
        try {
            return new TabularType("Groups", "Table of all Karaf Cellar groups", GROUP, new String[] { CellarGroupMBean.GROUP_NAME });
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build group table type", e);
        }
    }

    public static TabularData tableFrom(List<JmxGroup> groups) {
        TabularDataSupport table = new TabularDataSupport(GROUP_TABLE);
        for (JmxGroup group : groups) {
            table.put(group.asCompositeData());
        }
        return table;
    }

}
