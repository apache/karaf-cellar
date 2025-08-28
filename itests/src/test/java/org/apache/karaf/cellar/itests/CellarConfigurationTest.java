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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarConfigurationTest extends CellarTestSupport {

    @Test
    public void testCellarFeaturesModule() throws Exception {
        installCellar();

        System.out.println("Creating test cluster group ...");
        executeCommand("cluster:group-create test");

        System.out.println("Creating configuration in the test cluster group ...");
        executeCommand("cluster:config-property-set test org.apache.karaf.cellar.tst test test");
        String clusterConfig = executeCommand("cluster:config-property-list test org.apache.karaf.cellar.tst");
        System.out.println(clusterConfig);
        assertContains("test = test", clusterConfig);

        String localConfig = executeCommand("config:list \"(service.pid=org.apache.karaf.cellar.tst)\"");
        Assert.assertTrue(localConfig.isEmpty());

        System.out.println("Local node joins the test cluster group ...");
        System.out.println(executeCommand("cluster:group-join test"));

        localConfig = executeCommand("config:list \"(service.pid=org.apache.karaf.cellar.tst)\"");
        System.out.println(localConfig);
        assertContains("test = test", localConfig);
    }

}
