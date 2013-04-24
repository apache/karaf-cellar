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
import org.osgi.framework.*;

import java.util.Dictionary;

/**
 * Hazelcast bundle listener.
 */
public class HazelcastBundleListener implements SynchronousBundleListener {

    private BundleContext bundleContext;
    private CombinedClassLoader combinedClassLoader;

    public HazelcastBundleListener(BundleContext bundleContext, CombinedClassLoader combinedClassLoader) {
        this.bundleContext = bundleContext;
        this.combinedClassLoader = combinedClassLoader;
    }

    public void scanExistingBundles() {
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (isBundleEligible(bundle)) {
                combinedClassLoader.addBundle(bundle);
            }
        }
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTING:
            case BundleEvent.STARTED:
                if (isBundleEligible(event.getBundle())) {
                    combinedClassLoader.addBundle(event.getBundle());
                }
                break;
            case BundleEvent.STOPPING:
            case BundleEvent.STOPPED:
            case BundleEvent.RESOLVED:
            case BundleEvent.UNINSTALLED:
                if (isBundleEligible(event.getBundle())) {
                    combinedClassLoader.removeBundle(event.getBundle());
                }
                break;
        }
    }

    public boolean isBundleEligible(Bundle bundle) {
        if (bundle != null) {
            Dictionary dictionary = bundle.getHeaders();
            if (dictionary != null) {
                String importPackage = (String) dictionary.get(org.osgi.framework.Constants.IMPORT_PACKAGE);
                if (importPackage != null && (importPackage.contains("com.hazelcast") || importPackage.contains("org.apache.karaf.cellar"))) {
                    return true;
                }
            }
        }
        return false;
    }

}
