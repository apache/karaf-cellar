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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarFeaturesTest extends CellarTestSupport {

    @Test
    public void testCellarFeaturesModule() throws InterruptedException {
        installCellar();

        System.out.println("Checking if maven feature is installed locally ...");
        String features = executeCommand("feature:list -i");
        System.out.println(features);
        assertContainsNot("maven", features);

        System.out.println("Creating test cluster group ...");
        System.out.println(executeCommand("cluster:group-create test"));

        System.out.println("Adding Karaf standard features repository to the test cluster group ...");
        System.out.println(executeCommand("cluster:feature-repo-add test mvn:org.apache.karaf.features/standard/" + System.getProperty("karaf.version") + "/xml/features"));

        System.out.println("Installing maven feature on the test cluster group ...");
        System.out.println(executeCommand("cluster:feature-install test maven"));

        String clusterFeatures = executeCommand("cluster:feature-list -i test");
        System.out.println(clusterFeatures);
        assertContains("maven", clusterFeatures);

        System.out.println("Local node join test cluster group ...");
        System.out.println(executeCommand("cluster:group-join test"));

        System.out.println("Checking maven feature installed locally ...");
        features = executeCommand("feature:list -i");
        System.out.println(features);
        assertContains("maven", features);
    }

}
