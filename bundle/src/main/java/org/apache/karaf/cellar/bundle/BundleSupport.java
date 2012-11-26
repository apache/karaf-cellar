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
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

public class BundleSupport extends CellarSupport {

    protected BundleContext bundleContext;

    /**
     * Reads a {@code Dictionary} object and creates a property object out of it.
     *
     * @param dictionary
     * @return
     */
    public Properties dictionaryToProperties(Dictionary dictionary) {
        Properties properties = new Properties();
        if (dictionary != null && dictionary.keys() != null) {

            Enumeration keys = dictionary.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (key != null && dictionary.get(key) != null) {
                    properties.put(key, dictionary.get(key));
                }
            }
        }
        return properties;
    }


    /**
     * Installs a bundle using its location.
     */
    public void installBundleFromLocation(String location) throws BundleException {
        getBundleContext().installBundle(location);
    }

    /**
     * Uninstalls a bundle using its Symbolic name and version.
     *
     * @param symbolicName
     * @param version
     * @throws BundleException
     */
    public void uninstallBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.uninstall();
                }
            }
        }
    }

    /**
     * Starts a bundle using its Symbolic name and version.
     *
     * @param symbolicName
     * @param version
     * @throws BundleException
     */
    public void startBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.start();
                }
            }
        }
    }

    /**
     * Stops a bundle using its Symbolic name and version.
     *
     * @param symbolicName
     * @param version
     * @throws BundleException
     */
    public void stopBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.stop();
                }
            }
        }
    }

    /**
     * Updates a bundle using its Symbolic name and version.
     *
     * @param symbolicName
     * @param version
     * @throws BundleException
     */
    public void updateBundle(String symbolicName, String version) throws BundleException {
        Bundle[] bundles = getBundleContext().getBundles();
        if (bundles != null) {
            for (Bundle bundle : bundles) {
                if (bundle.getSymbolicName().equals(symbolicName) && bundle.getVersion().toString().equals(version)) {
                    bundle.update();
                }
            }
        }
    }

    public Boolean isSyncEnabled(Group group) {
        Boolean result = Boolean.FALSE;
        String groupName = group.getName();

        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP);
            Dictionary<String, String> properties = configuration.getProperties();
            if (properties != null) {
                String propertyKey = groupName + Configurations.SEPARATOR + Constants.CATEGORY + Configurations.SEPARATOR + Configurations.SYNC;
                String propertyValue = properties.get(propertyKey);
                result = Boolean.parseBoolean(propertyValue);
            }
        } catch (IOException e) {
            LOGGER.error("CELLAR BUNDLE: failed to check if sync is enabled", e);
        }
        return result;
    }

    /**
     * Returns the {@link BundleContext}.
     *
     * @return
     */
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    /**
     * Sets the {@link BundleContext}.
     *
     * @param bundleContext
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
