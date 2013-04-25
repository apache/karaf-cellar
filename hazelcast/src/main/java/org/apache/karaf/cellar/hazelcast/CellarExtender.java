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
package org.apache.karaf.cellar.hazelcast;

import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.framework.BundleContext;

/**
 * Cellar extender.
 */
public class CellarExtender  {

    private HazelcastBundleListener hazelcastBundleListener;
    private BundleContext bundleContext;
    private CombinedClassLoader combinedClassLoader;

    public void init() {
       hazelcastBundleListener = new HazelcastBundleListener(bundleContext,combinedClassLoader);
       bundleContext.addBundleListener(hazelcastBundleListener);
       combinedClassLoader.addBundle(bundleContext.getBundle());
       hazelcastBundleListener.scanExistingBundles();
    }

    public void destroy() {
      bundleContext.removeBundleListener(hazelcastBundleListener);
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
