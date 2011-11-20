package org.apache.karaf.cellar.itests;

import java.util.Set;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


import static org.junit.Assert.*;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarChildNodesTest extends CellarTestSupport {

    @Test
    public void testClusterWithChildNodes() throws InterruptedException {
        installCellar();
        configureLocalDiscovery(2);
        createCellarChild("child1");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        Node localNode = clusterManager.getNode();
        Set<Node> nodes =clusterManager.listNodes();
        System.err.println(executeCommand("cluster:nodes-list"));
        assertTrue("There should be at least 2 cellar nodes running", 2 <= nodes.size());


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
