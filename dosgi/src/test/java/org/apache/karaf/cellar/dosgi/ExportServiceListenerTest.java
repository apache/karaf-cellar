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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class ExportServiceListenerTest {

    private ExportServiceListener listener = new ExportServiceListener();

    @Test
    public void testGetServiceInterfaces() throws Exception {
        System.out.println("Test Service interfaces with null service");
        Set<String> expectedResult = new LinkedHashSet<String>();
        Set<String> result = listener.getServiceInterfaces(null,null);
        Assert.assertEquals(expectedResult,result);

        result = listener.getServiceInterfaces(null,new String[] {"*"});
        Assert.assertEquals(expectedResult,result);

        System.out.println("Test Service interfaces with ArrayList and wildcard services");
        result = listener.getServiceInterfaces(new ArrayList(),new String[] {"*"});
        Assert.assertTrue(result.contains("java.util.List"));

        System.out.println("Test Service interfaces with ArrayList and List/Serializable services");
        result = listener.getServiceInterfaces(new ArrayList(),new String[] {"java.util.List","java.io.Serializable"});
        Assert.assertTrue(result.contains("java.util.List"));
        Assert.assertTrue(result.contains("java.io.Serializable"));
    }

}
