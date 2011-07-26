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

    private static String HOME_PLACEHOLDER = "karaf.home";
    private static String RELATIVE_HOME = "${" + HOME_PLACEHOLDER + "}";
    private static String HOME = System.getProperty("karaf.home");

    private static String[] FILTERED_PROPERTIES = {"service.pid", "service.factoryPid", "felix.fileinstall.filename"};

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
                    String value = (String) dictionary.get(key);
                    properties.put(key, dictionary.get(key));
                }
            }
        }
        return properties;
    }

    /**
     * Prepares a dictionary for push
     *
     * @param dictionary
     * @return
     */
    public Dictionary preparePush(Dictionary dictionary) {
        Dictionary properties = new Properties();
        Enumeration keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key != null && dictionary.get(key) != null) {
                String value = (String) dictionary.get(key);
                value = convertStrings(value, HOME, RELATIVE_HOME);
                properties.put(key, dictionary.get(key));
            }
        }
        return properties;
    }

    /**
     * Prepares a dictionary for Pull
     *
     * @param dictionary
     * @return
     */
    public Dictionary preparePull(Dictionary dictionary) {
        Dictionary properties = new Properties();
        Enumeration keys = dictionary.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            if (key != null && dictionary.get(key) != null) {
                String value = (String) dictionary.get(key);
                value = convertStrings(value, RELATIVE_HOME, HOME);
                properties.put(key, dictionary.get(key));
            }
        }
        return properties;
    }

    /**
     * Performs a string replacement on value.
     *
     * @param value
     * @return
     */
    public String convertStrings(String value, String absolute, String relative) {
        String result = value;
        if (absolute != null && !absolute.isEmpty() && value.contains(absolute)) {
            result = value.replace(absolute, relative);
        }
        return result;
    }

    public Dictionary filterDictionary(Dictionary dictionary) {
        Dictionary result = new Properties();
        if (dictionary != null) {
            Enumeration enumaration = dictionary.keys();
            while (enumaration.hasMoreElements()) {
                String key = (String) enumaration.nextElement();
                if (!isPropertyFiltered(key)) {
                    String value = (String) dictionary.get(key);
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * Returns true if dictionaries are equal.
     *
     * @param dict1
     * @param dict2
     * @return
     */
    protected boolean dictionariesEqual(Dictionary dict1, Dictionary dict2) {
        return subDictionary(dict1, dict2) && subDictionary(dict2, dict1);
    }

    /**
     * Returns true if target contains all source key/value pairs.
     *
     * @param source
     * @param target
     * @return
     */
    public boolean subDictionary(Dictionary source, Dictionary target) {
        if (source == null && target == null) {
            return true;
        } else if (source == null || target == null) {
            return false;
        } else if (source.isEmpty() && target.isEmpty()) {
            return true;
        } else {
            Enumeration keys = source.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value1 = (String) source.get(key);
                String value2 = (String) target.get(key);

                if (value1 == null && value2 == null)
                    continue;
                else if (value1 == null)
                    return false;
                else if (value2 == null)
                    return false;
                else if (value1.equals(value2))
                    continue;
            }
            return true;
        }
    }

    /**
     * Returns true if property is Filtered.
     *
     * @param propertyName
     * @return
     */
    public boolean isPropertyFiltered(String propertyName) {
        for (int i = 0; i < FILTERED_PROPERTIES.length; i++) {
            if (FILTERED_PROPERTIES[i].equals(propertyName))
                return true;
        }
        return false;
    }

}
