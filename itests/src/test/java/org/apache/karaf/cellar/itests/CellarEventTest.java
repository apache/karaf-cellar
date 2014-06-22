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
public class CellarEventTest extends CellarTestSupport {

    @Test
    @Ignore
    public void testCellarEventFeatureInstall() throws Exception {
        installCellar();
        Thread.sleep(DEFAULT_TIMEOUT);
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
        
        System.err.println(executeCommand("features:install cellar-event"));
    }

    @After
    public void tearDown() {
        try {
            unInstallCellar();
        } catch (Exception ex) {
            //Ignore
        }
    }

}
