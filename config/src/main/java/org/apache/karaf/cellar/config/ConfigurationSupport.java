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
     * Filter a dictionary, specially replace the karaf.home property with a relative value.
     *
     * @param source the source dictionary.
     * @param target the target dictionary
     */
    public void filter(Dictionary source, Dictionary target) {
        if (source != null) {
            Enumeration enumaration = source.keys();
            while (enumaration.hasMoreElements()) {
                String key = (String) enumaration.nextElement();
                if (!isExcludedProperty(key)) {
                    String value = String.valueOf(source.get(key));
                    target.put(key, value);
                }
            }
        }
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
