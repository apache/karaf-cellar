/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.dosgi;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class EndpointDescriptionTest {

    String objectClass = "org.apache.karaf.cellar.dosgi.Test";
    String filterPattern = "(&(objectClass=%s))";

    @Test
    public void testMatches() throws Exception {
        // this is a dummy test for testing the behaviour of matches method
        String version = "1.0.0";
        String testEndpointFilter = String.format(filterPattern, objectClass);
        String endpointId = objectClass + Constants.SEPARATOR + version;
        HashMap<String, Object> exportedProperties = new HashMap<String, Object>();
        exportedProperties.put(org.osgi.framework.Constants.OBJECTCLASS, objectClass);

        EndpointDescription endpointDescription1 = new EndpointDescription(endpointId, null, exportedProperties);
        EndpointDescription endpointDescription2 = new EndpointDescription(endpointId, null, exportedProperties);
        Assert.assertTrue(endpointDescription1.matches(testEndpointFilter));
        Assert.assertTrue(endpointDescription2.matches(testEndpointFilter));
    }

    @Test
    public void testFilterStringCreation() {
        Map<String, Object> properties = new HashMap();
        properties.put(org.osgi.framework.Constants.OBJECTCLASS, "SomeServiceClass");
        String result = EndpointDescription.createFilterString(properties, false);
        Assert.assertNull(result);
        EndpointDescription endpointDescription = new EndpointDescription("id", null, properties);
        Assert.assertNull(endpointDescription.getFilter());
        result = EndpointDescription.createFilterString(properties, true);
        Assert.assertTrue(result.equals("(objectClass=SomeServiceClass)"));
        properties.put("someServiceKey", "SomeServiceValue");
        result = EndpointDescription.createFilterString(properties, false);
        Assert.assertTrue(result.equals("(someServiceKey=SomeServiceValue)"));
        result = EndpointDescription.createFilterString(properties, true);
        Assert.assertTrue(result.equals("(&(objectClass=SomeServiceClass)(someServiceKey=SomeServiceValue))"));
        properties.put("someServiceObject", new Object());
        result = EndpointDescription.createFilterString(properties, false);
        Assert.assertTrue(result.equals("(someServiceKey=SomeServiceValue)"));
        result = EndpointDescription.createFilterString(properties, true);
        Assert.assertTrue(result.equals("(&(objectClass=SomeServiceClass)(someServiceKey=SomeServiceValue))"));
        endpointDescription = new EndpointDescription("id", null, properties);
        Assert.assertTrue(endpointDescription.getFilter().equals("(someServiceKey=SomeServiceValue)"));
    }

}