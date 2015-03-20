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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * A factory for Hazelcast instance, integrating with OSGi ServiceRegistry and ConfigAdmin.
 */
public class HazelcastServiceFactory  {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastServiceFactory.class);

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

    public void update(Map properties) {
    	if (configurationManager.isUpdated(properties)) {
    		LOGGER.debug("CELLAR HAZELCAST: configuration update is true");
    		Config config = instance.getConfig();
    		TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
    		List<String> members = tcpIpConfig.getMembers();
    		
    		Set<String> discoveredMemberSet = configurationManager.getDiscoveredMemberSet();
    		discoveredMemberSet.removeAll(members);
    		
    		if (!discoveredMemberSet.isEmpty()) {
    			LOGGER.debug("CELLAR HAZELCAST: will add following members {}", discoveredMemberSet);
    			instance.getLifecycleService().pause();
    			for (String discoveredMember : discoveredMemberSet) {
    				tcpIpConfig.addMember(discoveredMember);
				}
    			if (!tcpIpConfig.isEnabled()) {
    				LOGGER.debug("CELLAR HAZELCAST: tcpip mode needs to be enabled, will do now!");
    				tcpIpConfig.setEnabled(true);
    			}
    			instance.getLifecycleService().restart();
    		}
    	}
    }

    /**
     * Get the Hazelcast instance.
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
        if(combinedClassLoader != null) {
            Thread.currentThread().setContextClassLoader(combinedClassLoader);
        }
        return Hazelcast.newHazelcastInstance(configurationManager.getHazelcastConfig());
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
