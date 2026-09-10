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
package org.apache.karaf.cellar.hazelcast.factory;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.framework.BundleContext;

/**
 * Factory for Hazelcast instance, including integration with OSGi ServiceRegistry and ConfigAdmin.
 */
public class HazelcastServiceFactory {

    static final String INSECURE_DEFAULT_GROUP_NAME = "cellar";
    static final String INSECURE_DEFAULT_GROUP_PASSWORD = "pass";

    private BundleContext bundleContext;
    private CombinedClassLoader combinedClassLoader;
    private HazelcastConfigurationManager configurationManager;

    private CountDownLatch initializationLatch = new CountDownLatch(1);
    private CountDownLatch instanceLatch = new CountDownLatch(1);
    private HazelcastInstance instance;

    public void init() {
        if (combinedClassLoader != null) {
            combinedClassLoader.addBundle(bundleContext.getBundle());
        }
        initializationLatch.countDown();
    }

    public void destroy() {
        if (instance != null) {
            instance.getLifecycleService().shutdown();
        }
    }

    public void update(Map properties) throws InterruptedException {
        if (configurationManager.isUpdated(properties) && instance != null) {
            // updating the member list
            HazelcastConfigurationManager.LOGGER.info("Updating the member list to: {}", configurationManager.getDiscoveredMemberSet());
            instance.getConfig().getNetworkConfig().getJoin().getTcpIpConfig()
                    .setMembers(new LinkedList<String>(configurationManager.getDiscoveredMemberSet()));
        }
    }

    /**
     * Return the local Hazelcast instance.
     *
     * @return the Hazelcast instance.
     */
    public HazelcastInstance getInstance() throws InterruptedException {
        if (instance == null) {
            initializationLatch.await();
            this.instance = buildInstance();
            instanceLatch.countDown();
        }
        return instance;
    }

    /**
     * Build a {@link HazelcastInstance}.
     *
     * @return the Hazelcast instance.
     */
    private HazelcastInstance buildInstance() {
        if (combinedClassLoader != null) {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
        }
        Config config = configurationManager.getHazelcastConfig();
        warnIfInsecureDefaults(config);
        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Checks whether the given Hazelcast configuration still uses the group name/password shipped
     * as Cellar defaults ("cellar"/"pass"). Both must match (not either alone), so an operator who
     * deliberately reused just the name (or just the password) isn't flagged.
     *
     * @param config the Hazelcast configuration.
     * @return true if the group name and password are both still the shipped defaults.
     */
    static boolean hasInsecureDefaultGroup(Config config) {
        if (config == null || config.getGroupConfig() == null) {
            return false;
        }
        GroupConfig groupConfig = config.getGroupConfig();
        return INSECURE_DEFAULT_GROUP_NAME.equals(groupConfig.getName())
                && INSECURE_DEFAULT_GROUP_PASSWORD.equals(groupConfig.getPassword());
    }

    /**
     * Checks whether multicast auto-join is enabled while none of SSL, symmetric-encryption or a
     * socket-interceptor are enabled to protect the resulting cluster traffic/join.
     *
     * @param config the Hazelcast configuration.
     * @return true if multicast is enabled with no transport protection.
     */
    static boolean hasInsecureMulticastJoin(Config config) {
        if (config == null) {
            return false;
        }
        NetworkConfig networkConfig = config.getNetworkConfig();
        if (networkConfig == null || networkConfig.getJoin() == null
                || networkConfig.getJoin().getMulticastConfig() == null
                || !networkConfig.getJoin().getMulticastConfig().isEnabled()) {
            return false;
        }
        boolean sslEnabled = networkConfig.getSSLConfig() != null && networkConfig.getSSLConfig().isEnabled();
        boolean symmetricEncryptionEnabled = networkConfig.getSymmetricEncryptionConfig() != null
                && networkConfig.getSymmetricEncryptionConfig().isEnabled();
        boolean socketInterceptorEnabled = networkConfig.getSocketInterceptorConfig() != null
                && networkConfig.getSocketInterceptorConfig().isEnabled();
        return !sslEnabled && !symmetricEncryptionEnabled && !socketInterceptorEnabled;
    }

    /**
     * Logs a prominent, non-blocking warning if the given Hazelcast configuration matches known
     * insecure shipped defaults. Never throws: a failure here must never prevent Hazelcast/Cellar
     * from starting.
     *
     * @param config the Hazelcast configuration.
     */
    static void warnIfInsecureDefaults(Config config) {
        try {
            if (hasInsecureDefaultGroup(config)) {
                HazelcastConfigurationManager.LOGGER.warn(
                        "CELLAR HAZELCAST SECURITY WARNING: the Hazelcast group name and password are still set to " +
                        "the well-known Cellar defaults (\"cellar\"/\"pass\"). In Hazelcast OSS the group password is " +
                        "NOT an authentication mechanism: any host that can reach and join this cluster can do so " +
                        "regardless of this password, and once joined can push cluster events (including bundle " +
                        "installs) to every node. Change the group name/password, and see the 'Security " +
                        "considerations' section of the Cellar Hazelcast documentation for hardening recommendations.");
            }
            if (hasInsecureMulticastJoin(config)) {
                HazelcastConfigurationManager.LOGGER.warn(
                        "CELLAR HAZELCAST SECURITY WARNING: multicast auto-discovery is enabled and none of SSL, " +
                        "symmetric-encryption, or a socket-interceptor are enabled. Any host on the same network " +
                        "segment that can reach the multicast group can join this Hazelcast/Cellar cluster and push " +
                        "cluster events (including bundle installs) to every node. For anything other than a trusted, " +
                        "isolated network: disable multicast in favor of tcp-ip with an explicit member allow-list, " +
                        "and/or enable SSL or symmetric-encryption. See the 'Security considerations' section of the " +
                        "Cellar Hazelcast documentation for details.");
            }
        } catch (Exception e) {
            HazelcastConfigurationManager.LOGGER.debug("Error while checking Hazelcast configuration for insecure defaults", e);
        }
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public CombinedClassLoader getCombinedClassLoader() {
        return combinedClassLoader;
    }

    public void setCombinedClassLoader(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }
    
    public void setConfigurationManager(HazelcastConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

}
