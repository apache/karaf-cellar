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

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * A factory for a Hazelcast Instance, which integration with OSGi Service Registry and Config Admin.
 */
public class HazelcastServiceFactory  {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastServiceFactory.class);

    private BundleContext bundleContext;
    private CombinedClassLoader combinedClassLoader;
    private HazelcastConfigurationManager configurationManager = new HazelcastConfigurationManager();

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
        configurationManager.isUpdated(properties);
    }

    /**
     * Returs a Hazelcast instance from service registry.
     *
     * @return
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
     * Builds a {@link HazelcastInstance}
     *
     * @return
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

}
