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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarGroupsTest extends CellarTestSupport {

    @Test
    @Ignore
    public void testGroupsWithChildNodes() throws InterruptedException {
        installCellar();
        createCellarChild("child1");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("cluster:node-list"));
        Node localNode = clusterManager.getNode();
        Set<Node> nodes = clusterManager.listNodes();
        assertTrue("There should be at least 2 cellar nodes running", 2 <= nodes.size());

        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("cluster:group-set testgroup " + localNode.getId()));
        System.err.println(executeCommand("cluster:group-list"));

        GroupManager groupManager = getOsgiService(GroupManager.class);
        assertNotNull(groupManager);

        Set<Group> groups = groupManager.listAllGroups();
        assertEquals("There should be 2 cellar groups", 2, groups.size());

        System.err.println(executeCommand("cluster:group-delete testgroup "));
        System.err.println(executeCommand("cluster:group-list"));
        groups = groupManager.listAllGroups();
        assertEquals("There should be a single cellar group", 1, groups.size());
    }

    @After
    public void tearDown() {
        try {
            destroyCellarChild("child1");
            unInstallCellar();
        } catch (Exception ex) {
            //Ignore
        }
    }

}
