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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarNodesTest extends CellarTestSupport {

    @Test(timeout = 180000)
    public void testClusterWithChildNodes() throws Exception {
        installCellar();

        createCellarInstance("child1");

        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.out.println("Waiting the Cellar nodes ...");
        Set<Node> nodes = clusterManager.listNodes();
        while (nodes.size() < 2) {
            nodes = clusterManager.listNodes();
            Thread.sleep(2000);
            System.out.println(executeCommand("cluster:node-list"));
        }
    }

    @After
    public void tearDown() {
        try {
            stopAndDestroyCellarInstance("child1");
        } catch (Exception ex) {
            //Ignore
        }
    }

}
