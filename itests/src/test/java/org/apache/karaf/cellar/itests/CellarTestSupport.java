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
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;

import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;

public class CellarTestSupport extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        Option[] options = new Option[]{
                KarafDistributionOption.replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", getConfigFile("/etc/org.ops4j.pax.logging.cfg")),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "cellar.version", System.getProperty("cellar.version"))
        };
        return Stream.of(super.config(), options).flatMap(Stream::of).toArray(Option[]::new);
    }

    public void installCellar() {
        System.out.println("Installing Cellar ...");
        System.out.println(executeCommand("feature:repo-add cellar " + System.getProperty("cellar.version")));
        System.out.println(executeCommand("feature:install cellar", new RolePrincipal("admin")));
        ClusterManager clusterManager = getOsgiService(ClusterManager.class);
        assertNotNull(clusterManager);
    }

    public void createCellarInstance(String name) throws Exception {
        System.out.println("Creating " + name + " Cellar instance ...");
        System.out.println(executeCommand("instance:create --featureURL mvn:org.apache.karaf.cellar/apache-karaf-cellar/" + System.getProperty("cellar.version") + "/xml/features --feature cellar " + name));
        System.out.println(executeCommand("instance:start " + name));

        System.out.println("Waiting " + name + " instance to be fully started ...");
        boolean started = false;
        while (!started) {
            Thread.sleep(2000);
            String status = executeCommand("instance:status " + name);
            System.out.println(status);
            started = status.contains("Started");
        }
    }

    public void stopAndDestroyCellarInstance(String name) {
        System.out.println("Stopping " + name + " Cellar instance ...");
        System.out.println(executeCommand("instance:stop " + name));
        System.out.println("Destroying " + name + " Cellar instance ...");
        System.out.println(executeCommand("instance:destroy " + name));
    }

}
