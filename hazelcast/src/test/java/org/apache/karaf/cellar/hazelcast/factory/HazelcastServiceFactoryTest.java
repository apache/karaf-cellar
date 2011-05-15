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

import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author: iocanel
 */
public class HazelcastServiceFactoryTest {

    HazelcastServiceFactory factory = new HazelcastServiceFactory();

    @Before
    public void setUp() throws Exception {
        factory.setUsername(GroupConfig.DEFAULT_GROUP_NAME);
        factory.setPassword(GroupConfig.DEFAULT_GROUP_PASSWORD);

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDefaultInstance() throws InterruptedException {
        HazelcastInstance defaultInstance = Hazelcast.newHazelcastInstance(null);
        HazelcastInstance factoryInstance = factory.buildInstance();
        Thread.sleep(5000);
        Assert.assertEquals(true, factoryInstance.getCluster().getMembers().size() >= 2);
    }
}
