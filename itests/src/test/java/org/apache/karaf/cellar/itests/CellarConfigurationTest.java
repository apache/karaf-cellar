/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.karaf.cellar.core.ClusterManager;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarConfigurationTest extends CellarTestSupport {

    private static final String TESTPID = "org.apache.karaf.cellar.tst";

    @Test
    @Ignore
    public void testCellarFeaturesModule() throws InterruptedException {
        installCellar();
        createCellarChild("child1");
        createCellarChild("child2");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        String node1 = getNodeIdOfChild("child1");
        String node2 = getNodeIdOfChild("child2");
        System.err.println(executeCommand("instance:list"));

        String properties = executeCommand("instance:connect child1 config:proplist --pid " + TESTPID);
        System.err.println(properties);
        assertFalse((properties.contains("myKey")));

        //Test configuration sync - add property
        System.err.println(executeCommand("config:propset --pid " + TESTPID + " myKey myValue"));
        Thread.sleep(5000);
        properties = executeCommand("instance:connect child1 config:proplist --pid " + TESTPID);
        System.err.println(properties);
        assertTrue(properties.contains("myKey = myValue"));

        //Test configuration sync - remove property
        System.err.println(executeCommand("config:propdel --pid " + TESTPID + " myKey"));
        Thread.sleep(5000);
        properties = executeCommand("instance:connect child1 config:proplist --pid " + TESTPID);
        System.err.println(properties);
        assertFalse(properties.contains("myKey"));


        //Test configuration sync - add property - join later
        System.err.println(executeCommand("cluster:group-set new-grp " + node1));
        Thread.sleep(5000);
        System.err.println(executeCommand("instance:connect child1 config:propset --pid " + TESTPID + " myKey2 myValue2"));
        properties = executeCommand("instance:connect child1 config:proplist --pid " + TESTPID);
        Thread.sleep(5000);
        System.err.println(executeCommand("cluster:group-set new-grp " + node2));
        properties = executeCommand("instance:connect child2 config:proplist --pid " + TESTPID);
        System.err.println(properties);
        assertTrue(properties.contains("myKey2 = myValue2"));
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("child1");
            destroyCellarChild("child2");
            unInstallCellar();
        } catch (Exception ex) {
            //Ignore
        }
    }

}
