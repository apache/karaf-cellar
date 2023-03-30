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
package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.MultiNode;
import org.apache.karaf.cellar.core.Node;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Description of an endpoint.
 */
public class EndpointDescription implements MultiNode {

    private final String id;
    private final String filter;
    private final Set<Node> nodes = new LinkedHashSet();
    private final Map<String, Object> properties = new HashMap();

    /**
     * Constructor with service properties
     *
     * @param id
     * @param node
     * @param properties
     */
    public EndpointDescription(String id, Node node, Map<String, Object> properties) {
        this.id = id;
        this.nodes.add(node);
        this.properties.putAll(properties);
        this.filter = createFilterString(properties, false);
    }

    /**
     * Constructor LDAP filter string from service properties
     * with or without object class
     *
     * @param properties
     * @param includeObjectClass
     */
    public static String createFilterString(Map<String, Object> properties, boolean includeObjectClass) {
        if (properties.size() == 0) {
            return null;
        }
        int filterCount = 0;
        StringBuilder filterStringBuilder = new StringBuilder();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getValue() instanceof String) {
                switch (entry.getKey()) {
                    case org.osgi.framework.Constants.OBJECTCLASS:
                        if (includeObjectClass) {
                            filterStringBuilder.append("(").append(entry.getKey()).append("=").append(entry.getValue()).append(")");
                            filterCount++;
                        }
                        break;
                    default:
                        filterStringBuilder.append("(").append(entry.getKey()).append("=").append(entry.getValue()).append(")");
                        filterCount++;
                        break;
                }
            }
        }
        if (filterCount == 1) {
            return filterStringBuilder.toString();
        } else if (filterCount > 1) {
            return filterStringBuilder.insert(0, "(&").append(')').toString();
        } else {
            return null;
        }
    }

    /**
     * Tests the properties of this <code>EndpointDescription</code> against
     * the given filter using a case insensitive match.
     *
     * @param filterString The filter to test.
     * @return <code>true</code> If the properties of this
     * <code>EndpointDescription</code> match the filter,
     * <code>false</code> otherwise.
     * @throws IllegalArgumentException If <code>filter</code> contains an
     *                                  invalid filter string that cannot be parsed.
     */
    public boolean matches(String filterString) {
        Filter filter;
        try {
            filter = FrameworkUtil.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException(e.getMessage(), e);
            throw illegalArgumentException;
        }

        Dictionary dictionary = new Properties();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            dictionary.put(key, value);
        }
        /*
         * we can use matchCase here since properties already supports case-insensitive key lookup.
         */
        return filter.matchCase(dictionary);
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        String result = null;
        String[] parts = id.split(Constants.SEPARATOR);
        if (parts != null && parts.length > 0) {
            result = parts[parts.length - 1];
        }
        return result;
    }

    public String getFilter() {
        return filter;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Set<Node> nodes) {
        if (nodes != null) {
            for (Node node : nodes) {
                this.nodes.add(node);
            }
        }
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public final String getServiceClass() {
        return (String) properties.get(org.osgi.framework.Constants.OBJECTCLASS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EndpointDescription endpointDescription = (EndpointDescription) o;

        return id != null ? id.equals(endpointDescription.id) : endpointDescription.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}