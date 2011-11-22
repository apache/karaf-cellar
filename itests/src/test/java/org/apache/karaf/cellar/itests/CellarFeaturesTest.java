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


import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CellarFeaturesTest extends CellarTestSupport {

    private static final String UNINSTALLED="[uninstalled]";
    private static final String INSTALLED="[installed  ]";

    @Test
    public void testCellarFeaturesModule() throws InterruptedException {
        installCellar();
        configureLocalDiscovery(2);
        createCellarChild("child1");
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);

        System.err.println(executeCommand("admin:list"));

        String httpFeatureStatus = executeCommand("admin:connect child1 features:list | grep eventadmin");
        System.err.println(httpFeatureStatus);
        assertTrue(httpFeatureStatus.startsWith(UNINSTALLED));

        //Test feature sync - install
        System.err.println(executeCommand("features:install eventadmin"));
        Thread.sleep(5000);
        httpFeatureStatus = executeCommand("admin:connect child1 features:list | grep eventadmin");
        System.err.println(httpFeatureStatus);
        assertTrue(httpFeatureStatus.startsWith(INSTALLED));

        //Test feature sync - uninstall
        System.err.println(executeCommand("features:uninstall eventadmin"));
        Thread.sleep(5000);
        httpFeatureStatus = executeCommand("admin:connect child1 features:list | grep eventadmin");
        System.err.println(httpFeatureStatus);
        assertTrue(httpFeatureStatus.startsWith(UNINSTALLED));

        //Test feature command - install
        System.err.println(executeCommand("cluster:features-install default eventadmin"));
        Thread.sleep(5000);
        httpFeatureStatus = executeCommand("admin:connect child1 features:list | grep eventadmin");
        System.err.println(httpFeatureStatus);
        assertTrue(httpFeatureStatus.startsWith(INSTALLED));

        //Test feature command - uninstall
        System.err.println(executeCommand("cluster:features-uninstall default eventadmin"));
        Thread.sleep(5000);
        httpFeatureStatus = executeCommand("admin:connect child1 features:list | grep eventadmin");
        System.err.println(httpFeatureStatus);
        assertTrue(httpFeatureStatus.startsWith(UNINSTALLED));


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
