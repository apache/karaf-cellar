/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.itests;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.itests.KarafTestSupport;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.stream.Stream;

import static org.junit.Assert.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarInstallationTest extends CellarTestSupport {

    @Test
    public void testInstallation() throws InterruptedException {
        installCellar();

        String bundles = executeCommand("bundle:list");
        System.out.println("bundles");
        assertContains("Hazelcast", bundles);
        String nodes = executeCommand("cluster:node-list");
        System.out.println(nodes);
        assertNotNull(nodes);

        System.out.println("Testing cluster shell commands ...");

        String output = executeCommand("cluster:consumer-status");
        System.out.println(output);
        String[] lines = output.split("\n");
        assertEquals(3, lines.length);
        assertContains("ON", lines[2]);

        output = executeCommand("cluster:producer-status");
        System.out.println(output);
        lines = output.split("\n");
        assertEquals(3, lines.length);
        assertContains("ON", lines[2]);

        output = executeCommand("cluster:handler-status");
        System.out.println(output);
        lines = output.split("\n");
        assertEquals(7, lines.length);

        output = executeCommand("cluster:group-list");
        System.out.println(output);
        lines = output.split("\n");
        assertEquals(3, lines.length);
        assertContains("default", lines[2]);

        output = executeCommand("cluster:bundle-list default");
        System.out.println(output);
        lines = output.split("\n");
        assertTrue(lines.length > 3);

        output = executeCommand("cluster:config-list default");
        System.out.println(output);
        lines = output.split("\n");
        assertTrue(lines.length > 3);

        output = executeCommand("cluster:feature-list default");
        System.out.println(output);
        lines = output.split("\n");
        assertTrue(lines.length > 3);
    }

}
