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

package org.apache.karaf.cellar.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Dictionary;
import java.util.Properties;

/**
 * @author iocanel
 */
public class ConfigurationSupportTest {

    ConfigurationSupport support = new ConfigurationSupport();

    @Test
    public void testFilterDictionary() {
        Dictionary result = null;
        Dictionary source = new Properties();
        Dictionary expectedResult = new Properties();

        source.put("key1", "value1");
        source.put("key2", "value2");

        expectedResult.put("key1", "value1");
        expectedResult.put("key2", "value2");
        result = support.filterDictionary(source);

        source.put("service.pid", "value3");
        result = support.filterDictionary(source);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void testConvertStrings() throws Exception {
        String absolutePath = "/somewehre/karaf/etc";
        String home = "/somewehre/karaf";
        String var = "${karaf.home}";

        String expectedResult = "${karaf.home}/etc";

        String result = support.convertStrings(absolutePath, home, var);
        Assert.assertEquals(expectedResult, result);
    }
}
