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
package org.apache.karaf.cellar.config;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Generic configuration support.
 */
public class ConfigurationSupport extends CellarSupport {

    private static final String FELIX_FILEINSTALL_FILENAME = "felix.fileinstall.filename";

    protected File storage;

    /**
     * Read a {@code Dictionary} and create a corresponding {@code Properties}.
     *
     * @param dictionary the source dictionary.
     * @return the corresponding properties.
     */
    public Properties dictionaryToProperties(Dictionary dictionary) {
        Properties properties = new Properties();
        if (dictionary != null) {
            Enumeration keys = dictionary.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key != null && dictionary.get(key) != null) {
                    properties.put(key, dictionary.get(key));
                }
            }
        }
        return properties;
    }

    /**
     * Returns true if dictionaries are equal.
     *
     * @param source the source dictionary.
     * @param target the target dictionary.
     * @return true if the two dictionaries are equal, false else.
     */
    protected boolean equals(Dictionary source, Dictionary target) {
        if (source == null && target == null)
            return true;

        if (source == null || target == null)
            return false;

        if (source.isEmpty() && target.isEmpty())
            return true;

        if (source.size() != target.size())
            return false;

        Enumeration sourceKeys = source.keys();
        while (sourceKeys.hasMoreElements()) {
            Object key = sourceKeys.nextElement();
            Object sourceValue = source.get(key);
            Object targetValue = target.get(key);
            if (sourceValue != null && targetValue == null)
                return false;
            if (sourceValue == null && targetValue != null)
                return false;
            if (!sourceValue.equals(targetValue))
                return false;
        }

        return true;
    }

    /**
     * Filter a dictionary, and populate a target dictionary.
     *
     * @param dictionary the source dictionary.
     * @return the filtered dictionary
     */
    public Dictionary filter(Dictionary dictionary) {
        Dictionary result = new Properties();
        if (dictionary != null) {
            Enumeration sourceKeys = dictionary.keys();
            while (sourceKeys.hasMoreElements()) {
                String key = (String) sourceKeys.nextElement();
                if (!isExcludedProperty(key)) {
                    Object value = dictionary.get(key);
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Check if a property is in the default excluded list.
     *
     * @param propertyName the property name to check.
     * @return true if the property is excluded, false else.
     */
    public boolean isExcludedProperty(String propertyName) {
        try {
            Configuration nodeConfiguration = configurationAdmin.getConfiguration(Configurations.NODE, null);
            if (nodeConfiguration != null) {
                Dictionary properties = nodeConfiguration.getProperties();
                if (properties != null) {
                    String property = properties.get("config.excluded.properties").toString();
                    String[] excludedProperties = property.split(",");
                    for (int i = 0; i < excludedProperties.length; i++) {
                        if (excludedProperties[i].trim().equals(propertyName))
                            return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("CELLAR CONFIG: can't check excluded properties", e);
        }
        return false;
    }

    /**
     * Persist a configuration to a storage.
     *
     * @param admin the configuration admin service.
     * @param pid the configuration PID.
     * @param props the properties to store, associated with the configuration PID.
     */
    protected void persistConfiguration(ConfigurationAdmin admin, String pid, Dictionary props) {
        try {
            if (pid.matches(".*-.*-.*-.*-.*")) {
                // it's UUID
                return;
            }
            File storageFile = new File(storage, pid + ".cfg");
            Configuration cfg = admin.getConfiguration(pid, null);
            if (cfg != null && cfg.getProperties() != null) {
                Object val = cfg.getProperties().get(FELIX_FILEINSTALL_FILENAME);
                try {
                    if (val instanceof URL) {
                        storageFile = new File(((URL) val).toURI());
                    }
                    if (val instanceof URI) {
                        storageFile = new File((URI) val);
                    }
                    if (val instanceof String) {
                        storageFile = new File(new URL((String) val).toURI());
                    }
                } catch (Exception e) {
                    throw new IOException(e.getMessage(), e);
                }
            }

            org.apache.felix.utils.properties.Properties p = new org.apache.felix.utils.properties.Properties(storageFile);
            List<String> propertiesToRemove = new ArrayList<String>();
            Set<String> set = p.keySet();

            for (String key : set) {
                if (!org.osgi.framework.Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FELIX_FILEINSTALL_FILENAME.equals(key)) {
                    propertiesToRemove.add(key);
                }
            }

            for (String key : propertiesToRemove) {
                p.remove(key);
            }

            for (Enumeration<String> keys = props.keys(); keys.hasMoreElements(); ) {
                String key = keys.nextElement();
                if (!org.osgi.framework.Constants.SERVICE_PID.equals(key)
                        && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                        && !FELIX_FILEINSTALL_FILENAME.equals(key)) {
                    p.put(key, (String) props.get(key));
                }
            }

            // save the cfg file
            storage.mkdirs();
            p.save();
        } catch (Exception e) {
            // nothing to do
        }
    }

    /**
     * Delete the storage file of a configuration.
     *
     * @param pid the configuration PID.
     */
    protected void deleteStorage(String pid) {
        File cfgFile = new File(storage, pid + ".cfg");
        cfgFile.delete();
    }

    public File getStorage() {
        return storage;
    }

    public void setStorage(File storage) {
        this.storage = storage;
    }

}
