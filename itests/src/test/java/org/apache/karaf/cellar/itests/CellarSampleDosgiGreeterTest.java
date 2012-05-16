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

import java.util.Set;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarSampleDosgiGreeterTest extends CellarTestSupport {

    private static final String UNINSTALLED = "[uninstalled]";
    private static final String INSTALLED = "[installed  ]";

    @Test
    public void testDosgiGreeter() throws InterruptedException {
        installCellar();
        createCellarChild("child1");
        createCellarChild("child2");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("features:addurl mvn:org.apache.karaf.cellar.samples/dosgi-greeter/2.2.4-SNAPSHOT/xml/features"));

        System.err.println(executeCommand("admin:list"));

        System.err.println(executeCommand("cluster:node-list"));
        Node localNode = clusterManager.getNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertTrue("There should be at least 3 cellar nodes running", 3 <= nodes.size());

        Thread.sleep(DEFAULT_TIMEOUT);

        String node1 = getNodeIdOfChild("child1");
        String node2 = getNodeIdOfChild("child2");

        System.err.println("Node 1: " + node1);
        System.err.println("Node 2: " + node2);

        executeCommand("cluster:group-create client-grp");
        executeCommand("cluster:group-create service-grp");
        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("cluster:group-set client-grp " + localNode.getId()));
        System.err.println(executeCommand("cluster:group-set service-grp " + node1));

        System.err.println(executeCommand("cluster:feature-install client-grp greeter-client"));
        Thread.sleep(10000);
        System.err.println(executeCommand("cluster:feature-install service-grp greeter-service"));
        Thread.sleep(10000);
        System.err.println(executeCommand("cluster:service-list"));
        String greetOutput = executeCommand("dosgi-greeter:greet Hi 10");
        System.err.println(greetOutput);
        assertEquals("Expected 10 greets", 10, countGreetsFromNode(greetOutput, node1));
        System.err.println(executeCommand("cluster:group-set service-grp " + node2));
        Thread.sleep(10000);
        Thread.sleep(10000);
        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("admin:connect child2  osgi:list -t 0"));
        System.err.println(executeCommand("cluster:service-list"));
        greetOutput = executeCommand("dosgi-greeter:greet Hi 10");
        System.err.println(greetOutput);
        assertEquals("Expected 5 greets", 5, countGreetsFromNode(greetOutput, node1));
        assertEquals("Expected 5 greets", 5, countGreetsFromNode(greetOutput, node2));
    }

    public int countGreetsFromNode(String output, String nodeId) {
        int count = 0;
        String[] greets = output.split("\n");
        for (String greet : greets) {
            if (greet.contains(nodeId)) {
                count++;
            }
        }
        return count;
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
