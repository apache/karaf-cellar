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
     * Check if two dictionaries are egal.
     *
     * @param dict1 the source dictionary.
     * @param dict2 the target dictionary.
     * @return true if the two dictionaries are egal, false else
     */
    protected boolean equals(Dictionary dict1, Dictionary dict2) {
        if (dict1 == null && dict2 == null)
            return true;

        if (dict1 == null && dict2 != null)
            return false;

        if (dict1 != null && dict2 == null)
            return false;

        if (dict1.size() != dict2.size())
            return false;

        for (Enumeration e = dict1.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            if (!dict1.get(key).equals(dict2.get(key))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if target contains all source key/value pairs.
     *
     * @param source
     * @param target
     * @return
     */
    private boolean subDictionary(Dictionary source, Dictionary target) {
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

}
