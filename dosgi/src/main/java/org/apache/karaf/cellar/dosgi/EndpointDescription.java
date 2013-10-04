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
    private final Set<Node> nodes = new LinkedHashSet<Node>();
    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Constructor
     *
     * @param id
     * @param node
     */
    public EndpointDescription(String id, Node node) {
        this.id = id;
        this.nodes.add(node);
        properties.put(org.osgi.framework.Constants.OBJECTCLASS,getServiceClass());
    }


    /**
     * Tests the properties of this <code>EndpointDescription</code> against
     * the given filter using a case insensitive match.
     *
     * @param filter The filter to test.
     * @return <code>true</code> If the properties of this
     *         <code>EndpointDescription</code> match the filter,
     *         <code>false</code> otherwise.
     * @throws IllegalArgumentException If <code>filter</code> contains an
     *                                  invalid filter string that cannot be parsed.
     */
    public boolean matches(String filter) {
        Filter f;
        try {
            f = FrameworkUtil.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            IllegalArgumentException iae = new IllegalArgumentException(e.getMessage());
            iae.initCause(e);
            throw iae;
        }

        Dictionary dictionary = new Properties();
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            dictionary.put(key, value);
        }
        /*
           * we can use matchCase here since properties already supports case
           * insensitive key lookup.
           */
        return f.matchCase(dictionary);
    }

    public String getId() {
        return id;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

     public void setNodes(Set<Node> nodes) {
         if(nodes != null) {
             for(Node node:nodes) {
                 this.nodes.add(node);
             }
         }
     }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public final String getServiceClass() {
        String result = null;

        if(id != null) {
            String[] parts = id.split(Constants.SEPARATOR);
            if(parts != null && parts.length > 0) {
                result = parts[0];
            }
        }
        return result;
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

        if (id != null ? !id.equals(endpointDescription.id) : endpointDescription.id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
