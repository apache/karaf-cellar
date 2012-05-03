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
 * Configuration support test.
 */
public class ConfigurationSupportTest {

    ConfigurationSupport support = new ConfigurationSupport();

    @Test
    public void testEquals() throws Exception {
        Dictionary source = new Properties();
        Dictionary target = new Properties();

        source.put("key1", "value1");
        source.put("key2", 1);
        source.put("key3", Boolean.FALSE);
        source.put("key4", 12L);

        target.put("key1", "value1");
        target.put("key2", 1);
        target.put("key3", Boolean.FALSE);
        target.put("key4", 12L);

        Assert.assertEquals(true, support.equals(source, target));
    }

    @Test
    public void testNotEquals() throws Exception {
        Dictionary source = new Properties();
        Dictionary target = new Properties();

        source.put("key1", "value1");
        source.put("key2", 2);
        source.put("key3", Boolean.FALSE);
        source.put("key4", 12L);

        target.put("key1", "value1");
        target.put("key2", 1);
        target.put("key3", Boolean.FALSE);
        target.put("key4", 12L);

        Assert.assertEquals(false, support.equals(source, target));
    }

    @Test
    public void testNotEqualsDueToSize() throws Exception {
        Dictionary source = new Properties();
        Dictionary target = new Properties();

        source.put("key1", "value1");
        source.put("key2", 2);
        source.put("key3", Boolean.FALSE);

        target.put("key1", "value1");
        target.put("key2", 1);
        target.put("key3", Boolean.FALSE);
        target.put("key4", 12L);

        Assert.assertEquals(false, support.equals(source, target));
    }

}
