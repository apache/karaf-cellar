package org.apache.karaf.cellar.itests;

import java.util.Set;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarGroupsTest extends CellarTestSupport {

    @Test
    public void testGroupsWithChildNodes() throws InterruptedException {
        installCellar();
        configureLocalDiscovery(2);
        createCellarChild("child1");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("cluster:nodes-list"));
        Node localNode = clusterManager.getNode();
        Set<Node> nodes =clusterManager.listNodes();
        assertTrue("There should be at least 2 cellar nodes running", 2 <= nodes.size());

        System.err.println(executeCommand("cluster:group-list"));
        System.err.println(executeCommand("cluster:group-set testgroup "+localNode.getId()));
        System.err.println(executeCommand("cluster:group-list"));

        GroupManager groupManager = getOsgiService(GroupManager.class);
        assertNotNull(groupManager);

        Set<Group> groups = groupManager.listAllGroups();
        assertEquals("There should be 2 cellar groups", 2 , groups.size());

        System.err.println(executeCommand("cluster:group-delete testgroup "));
        System.err.println(executeCommand("cluster:group-list"));
        groups = groupManager.listAllGroups();
        assertEquals("There should be a single cellar group", 1 , groups.size());

    }

    @After
    public void tearDown() {
        try {
            unInstallCellar();
            destroyCellarChild("child1");
        } catch (Exception ex) {
            //Ignore
        }
    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                cellarDistributionConfiguration(), keepRuntimeFolder()};
    }
}
