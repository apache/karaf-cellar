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
package org.apache.karaf.cellar.bundle;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.util.bundles.BundleUtils;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Generic Cellar bundle support.
 */
public class BundleSupport extends CellarSupport {

    protected BundleContext bundleContext;
	private FeaturesService featuresService;

    /**
     * Locally install a bundle.
     *
     * @param location the bundle location.
     * @param level optional bundle start level.
     * @throws BundleException in case of installation failure.
     */
    public void installBundleFromLocation(String location, Integer level) throws BundleException {
        Bundle bundle = getBundleContext().installBundle(location);
        if (level != null) {
            bundle.adapt(BundleStartLevel.class).setStartLevel(level);
        }
    }

    public boolean isInstalled(String location) {
        return findBundle(location) != null;
    }

    public boolean isStarted(String location) {
        Bundle[] bundles = getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getLocation().equals(location) && (bundle.getState() == Bundle.ACTIVE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Locally uninstall a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of un-installation failure.
     */
    public void uninstallBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getHeaders().get("Bundle-Version").toString().equals(version)) {
                    bundle.uninstall();
                }
            }
        }
    }

    /**
     * Locally start a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of start failure.
     */
    public void startBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getHeaders().get("Bundle-Version").toString().equals(version)) {
                    bundle.start();
                }
            }
        }
    }

    /**
     * Locally stop a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @throws BundleException in case of stop failure.
     */
    public void stopBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getHeaders().get("Bundle-Version").toString().equals(version)) {
                    bundle.stop();
                }
            }
        }
    }

    /**
     * Locally update a bundle.
     *
     * @param symbolicName the bundle symbolic name.
     * @param version the bundle version.
     * @param location the bundle update location.
     * @throws BundleException in case of update failure.
     */
    public void updateBundle(String symbolicName, String version, String location) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getHeaders().get("Bundle-Version").toString().equals(version)) {
                    if (location != null) {
                        try {
                            update(bundle, new URL(location));
                        } catch (Exception e) {
                            throw new BundleException("Can't update bundle", e);
                        }
                    } else {
                        String loc = bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_UPDATELOCATION);
                        if (loc != null && !loc.equals(bundle.getLocation())) {
                            try {
                                update(bundle, new URL(loc));
                            } catch (Exception e) {
                                throw new BundleException("Can't update bundle", e);
                            }
                        } else {
                            bundle.update();
                        }
                    }
                }
            }
        }
    }

    private void update(Bundle bundle, URL location) throws IOException, BundleException {
        try (InputStream is = location.openStream()) {
            File file = BundleUtils.fixBundleWithUpdateLocation(is, location.toString());
            try (FileInputStream fis = new FileInputStream(file)) {
                bundle.update(fis);
            }
            file.delete();
        }
    }

    /**
     * Get the list of features where the bundle is belonging.
     *
     * @param bundleLocation the bundle location.
     * @return the list of feature where the bundle is present.
     * @throws Exception in case of retrieval failure.
     */
	protected List<Feature> retrieveFeature(String bundleLocation) throws Exception {
		Feature[] features = featuresService.listFeatures();
		List<Feature> matchingFeatures = new ArrayList<Feature>();
		for (Feature feature : features) {
			List<BundleInfo> bundles = feature.getBundles();
			for (BundleInfo bundleInfo : bundles) {
				String location = bundleInfo.getLocation();
				if (location.equalsIgnoreCase(bundleLocation)) {
					matchingFeatures.add(feature);
					LOGGER.debug("CELLAR BUNDLE: found a feature {} containing bundle {}", feature.getName(), bundleLocation);
				}
			}
		}
		return matchingFeatures;
	}

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

	public FeaturesService getFeaturesService() {
		return featuresService;
	}

	public void setFeaturesService(FeaturesService featureService) {
		this.featuresService = featureService;
	}

    /**
     * Finds locally installed bundle by its location.
     * 
     * @param location
     *            the location of the bundle to be found
     * @return locally installed bundle for the specified location or <code>null</code> if there is no matching bundle installed
     */
    protected Bundle findBundle(String location) {
        Bundle[] bundles = getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getLocation().equals(location)) {
                return bundle;
            }
        }
        return null;
    }


    /**
     * Returns the system bundle of the OSGi framework.
     *
     * @return the system bundle of the OSGi framework
     */
    protected Bundle getSystemBundle() {
        return bundleContext.getBundle(0);
    }

    /**
     * Obtains the {@link FrameworkWiring}.
     *
     * @return the {@link FrameworkWiring}
     */
    protected FrameworkWiring getFrameworkWiring() {
        return getSystemBundle().adapt(FrameworkWiring.class);
    }
    
    /**
     * Refreshes the specified bundle.
     *
     * @param bundle the bundle to refresh
     */
    protected void refreshBundle(Bundle bundle) {
        refreshBundles(Collections.singleton(bundle));
    }
    
    /**
     * Refreshes the specified bundles.
     *
     * @param bundlesToRefresh the bundles to refresh
     */
    protected void refreshBundles(Collection<Bundle> bundlesToRefresh) {
        final CountDownLatch latch = new CountDownLatch(1);

        getFrameworkWiring().refreshBundles(bundlesToRefresh, new FrameworkListener() {
            @Override
            public void frameworkEvent(FrameworkEvent event) {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.warn("Waiting for refresh of bundles was interrupted", e);
        }
    }

    /**
     * Resolves the specified bundle.
     *
     * @param bundle the bundle to be resolved
     * @return {@code true} if the specified bundle is resolved; {@code false} otherwise.
     */
    public boolean resolveBundle(Bundle bundle) {
        return resolveBundles(Collections.singleton(bundle));
    }

    /**
     * Resolves the specified bundles.
     *
     * @param bundlesToResolve
     *            the bundles to resolve or {@code null} to resolve all unresolved bundles installed in the Framework
     * @return {@code true} if all specified bundles are resolved; {@code false} otherwise.
     */
    protected boolean resolveBundles(Collection<Bundle> bundlesToResolve) {
        return getFrameworkWiring().resolveBundles(bundlesToResolve);
    }

}
