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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Configuration support.
 */
public class ConfigurationSupport extends CellarSupport {

    private static String[] EXCLUDED_PROPERTIES = { "felix.fileinstall.filename", "felix.fileinstall.dir", "felix.fileinstall.tmpdir", "org.ops4j.pax.url.mvn.defaultRepositories" };

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
                    String value = String.valueOf(dictionary.get(key));
                    properties.put(key, value);
                }
            }
        }
        return properties;
    }

    /**
     * Filter a dictionary, specially replace the karaf.home property with a relative value.
     * @param dictionary
     * @return
     */
    public Dictionary filter(Dictionary dictionary) {
        Dictionary result = new Properties();
        if (dictionary != null) {
            Enumeration enumaration = dictionary.keys();
            while (enumaration.hasMoreElements()) {
                String key = (String) enumaration.nextElement();
                if (!isExcludedProperty(key)) {
                    String value = String.valueOf(dictionary.get(key));
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Returns true if property is Filtered.
     *
     * @param propertyName
     * @return
     */
    public boolean isExcludedProperty(String propertyName) {
        for (int i = 0; i < EXCLUDED_PROPERTIES.length; i++) {
            if (EXCLUDED_PROPERTIES[i].equals(propertyName))
                return true;
        }
        return false;
    }

}
