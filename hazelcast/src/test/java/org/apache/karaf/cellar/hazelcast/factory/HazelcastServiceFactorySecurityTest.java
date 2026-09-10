/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.hazelcast.factory;

import com.hazelcast.config.Config;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.config.SocketInterceptorConfig;
import com.hazelcast.config.SymmetricEncryptionConfig;
import com.hazelcast.config.XmlConfigBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HazelcastServiceFactorySecurityTest {

    @Test
    public void shippedDefaultGroupIsInsecure() {
        Config config = new Config();
        config.getGroupConfig().setName("cellar").setPassword("pass");
        Assert.assertTrue(HazelcastServiceFactory.hasInsecureDefaultGroup(config));
    }

    @Test
    public void customNameWithDefaultPasswordIsNotFlagged() {
        Config config = new Config();
        config.getGroupConfig().setName("my-cluster").setPassword("pass");
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureDefaultGroup(config));
    }

    @Test
    public void defaultNameWithCustomPasswordIsNotFlagged() {
        Config config = new Config();
        config.getGroupConfig().setName("cellar").setPassword("s3cr3t");
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureDefaultGroup(config));
    }

    @Test
    public void multicastEnabledWithNoProtectionIsInsecure() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        Assert.assertTrue(HazelcastServiceFactory.hasInsecureMulticastJoin(config));
    }

    @Test
    public void multicastDisabledIsNotFlaggedEvenWithoutProtection() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureMulticastJoin(config));
    }

    @Test
    public void multicastEnabledWithSslIsNotFlagged() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        config.getNetworkConfig().setSSLConfig(new SSLConfig().setEnabled(true));
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureMulticastJoin(config));
    }

    @Test
    public void multicastEnabledWithSymmetricEncryptionIsNotFlagged() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        config.getNetworkConfig().setSymmetricEncryptionConfig(new SymmetricEncryptionConfig().setEnabled(true));
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureMulticastJoin(config));
    }

    @Test
    public void multicastEnabledWithSocketInterceptorIsNotFlagged() {
        Config config = new Config();
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        config.getNetworkConfig().setSocketInterceptorConfig(new SocketInterceptorConfig().setEnabled(true));
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureMulticastJoin(config));
    }

    @Test
    public void nullConfigIsNeverFlagged() {
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureDefaultGroup(null));
        Assert.assertFalse(HazelcastServiceFactory.hasInsecureMulticastJoin(null));
    }

    @Test
    public void warnIfInsecureDefaultsNeverThrowsOnNullConfig() {
        HazelcastServiceFactory.warnIfInsecureDefaults(null);
    }

    @Test
    public void minimalXmlWithNoSslElementsDoesNotNpe() {
        // regression guard: src/test/resources/etc/hazelcast.xml has no <group>, <ssl>,
        // <symmetric-encryption> or <socket-interceptor> elements, so getSSLConfig() etc. return null
        allowXxeProtectionFailures();
        Config config = new XmlConfigBuilder(getClass().getResourceAsStream("/etc/hazelcast.xml")).build();
        HazelcastServiceFactory.hasInsecureMulticastJoin(config);
        HazelcastServiceFactory.warnIfInsecureDefaults(config);
    }

    @Test
    public void shippedAssemblyDefaultsAreDetectedAsInsecure() throws Exception {
        // regression guard so this detector doesn't silently drift out of sync if the shipped
        // assembly/src/main/resources/hazelcast.xml defaults ever change
        allowXxeProtectionFailures();
        Config config = new XmlConfigBuilder("../assembly/src/main/resources/hazelcast.xml").build();
        Assert.assertTrue("update this test if assembly/hazelcast.xml defaults intentionally change",
                HazelcastServiceFactory.hasInsecureDefaultGroup(config));
        Assert.assertTrue("update this test if assembly/hazelcast.xml defaults intentionally change",
                HazelcastServiceFactory.hasInsecureMulticastJoin(config));
    }

    // some JDKs ship a default TransformerFactory that doesn't support the XXE-protection
    // attributes Hazelcast's XmlConfigBuilder tries to set; same workaround as HazelcastServiceFactoryTest
    private static void allowXxeProtectionFailures() {
        System.setProperty("hazelcast.ignoreXxeProtectionFailures", "true");
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
    }

}
