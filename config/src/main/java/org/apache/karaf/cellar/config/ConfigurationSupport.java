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
                String value = String.valueOf(dictionary.get(key));
                value = convertStrings(value, HOME, RELATIVE_HOME);
                properties.put(key, value);
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
                String value = String.valueOf(dictionary.get(key));
                value = convertStrings(value, RELATIVE_HOME, HOME);
                properties.put(key, value);
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
        if (absolute != null && (absolute.trim().length() > 0) && value.contains(absolute)) {
            result = value.replace(absolute, relative);
        }
        return result;
    }

}
