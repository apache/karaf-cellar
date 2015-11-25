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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Hazelcast service factory test.
 */
@RunWith(JUnit4.class)
public class HazelcastServiceFactoryTest {

    @Test
    public void testDefaultInstance() throws InterruptedException {
        // Joining a cluster may not work with the default configuration on
        // networks where multicast is disabled. Use a custom hazelcast.xml
        // configuration that disables multicast and enables tcp on 127.0.0.1
        System.setProperty("karaf.etc", "src/test/resources/etc");
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        HazelcastServiceFactory factory = new HazelcastServiceFactory();
        factory.setConfigurationManager(new HazelcastConfigurationManager());
        factory.init();
        factory.getInstance();
        HazelcastInstance factoryInstance = factory.getInstance();

        HazelcastInstance defaultInstance = Hazelcast.newHazelcastInstance(null);

        Assert.assertEquals(true, factoryInstance.getCluster().getMembers().size() >= 2);

        factoryInstance.shutdown();
        defaultInstance.shutdown();
    }

    @Test
    public void testDefaultInstanceWithSaxon() throws InterruptedException {
        System.setProperty("karaf.etc", "src/test/resources/etc");
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        HazelcastServiceFactory factory = new HazelcastServiceFactory();
        factory.setConfigurationManager(new HazelcastConfigurationManager());
        factory.init();
        factory.getInstance();
        HazelcastInstance factoryInstance = factory.getInstance();

        HazelcastInstance defaultInstance = Hazelcast.newHazelcastInstance(null);

        Assert.assertEquals(true, factoryInstance.getCluster().getMembers().size() >= 2);

        factoryInstance.shutdown();
        defaultInstance.shutdown();
    }

}
