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
package org.apache.karaf.cellar.hazelcast.factory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Properties;

/**
 * Hazelcast service factory test.
 */
@RunWith(JUnit4.class)
public class HazelcastServiceFactoryTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDefaultInstance() throws InterruptedException {
        HazelcastServiceFactory factory = new HazelcastServiceFactory();
        factory.init();
        factory.getInstance();
        factory.update(null);
        HazelcastInstance defaultInstance = Hazelcast.newHazelcastInstance(null);

        // define the username and password as in the hazelcast-default.xml provided by Hazelcast
        // without this, "cellar" instance is not in the same cluster as the Hazelcast default one
        Properties properties = new Properties();
        properties.put(HazelcastConfigurationManager.USERNAME, "dev");
        properties.put(HazelcastConfigurationManager.PASSWORD, "dev-pass");
        factory.update(properties);

        HazelcastInstance factoryInstance = factory.getInstance();
        Assert.assertEquals(true, factoryInstance.getCluster().getMembers().size() >= 2);
    }

}
