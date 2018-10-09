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
package org.apache.karaf.cellar.core;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Properties;
import org.apache.karaf.cellar.core.event.EventType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


import static junit.framework.TestCase.assertTrue;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class CellarSupportTest {

    ConfigurationAdmin configurationAdmin = createMock(ConfigurationAdmin.class);
    Configuration configuration = createMock(Configuration.class);
    Properties props = new Properties();

    Group defaultGroup = new Group("default");

    @Before
    public void setUp() throws Exception {
        InputStream is = getClass().getResourceAsStream("groups.properties");
        props.load(is);
        is.close();
        expect(configurationAdmin.getConfiguration(EasyMock.<String>anyObject(), EasyMock.anyString())).andReturn(configuration).anyTimes();
        Dictionary propsDictionary = (Dictionary) props;
        expect(configuration.getProperties()).andReturn(propsDictionary).anyTimes();
        replay(configuration);
        replay(configurationAdmin);
    }

    @After
    public void tearDown() throws Exception {
        verify(configuration);
        verify(configurationAdmin);
    }

    @Test
    public void testIsAllowed() {
        CellarSupport support = new CellarSupport();
        support.setConfigurationAdmin(configurationAdmin);

        Boolean expectedResult = false;
        Boolean result = support.isAllowed(defaultGroup,"config","org.apache.karaf.shell", EventType.INBOUND);
        assertEquals("Shell should not be allowed",expectedResult,result);

        expectedResult = true;
        result = support.isAllowed(defaultGroup,"config","org.apache.karaf.cellar.group", EventType.INBOUND);
        assertEquals("Group config should be allowed",expectedResult,result);

        expectedResult = false;
        result = support.isAllowed(defaultGroup,"config","org.apache.karaf.cellar.node", EventType.INBOUND);
        assertEquals("Node config should be allowed",expectedResult,result);

        expectedResult = false;
        result = support.isAllowed(defaultGroup,"config","org.apache.karaf.cellar.instance", EventType.INBOUND);
        assertEquals("Instance config should be allowed",expectedResult,result);
    }

    @Test
    public void testBundleWildcard() {
        CellarSupport support = new CellarSupport();

        boolean test = support.wildCardMatch("this_is_a_test/1.0", "*");
        assertTrue(test);

        test = support.wildCardMatch("foo_bar", "foo*");
        assertTrue(test);

        test = support.wildCardMatch("foo_bar", "*bar");
        assertTrue(test);

        test = support.wildCardMatch("foo_bar", "hell*");
        assertFalse(test);
    }

}
