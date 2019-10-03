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
package org.apache.karaf.cellar.core.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.karaf.cellar.core.Configurations;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Cellar utils class.
 */
public class CellarUtils {

    public static enum MergeType {
        MERGA
    }

    private static final transient Logger LOGGER = LoggerFactory.getLogger(CellarUtils.class);

    public static final String MERGABLE = "MERGABLE[%s]";
    public static final String MERGABLE_REGEX = "MERGABLE\\[([^\\s]+[\\,]*[\\s]*)*\\]";

    private static final Pattern mergablePattern = Pattern.compile(MERGABLE_REGEX);

    /**
     * Check if a String is "merge-able".
     *
     * @param s the String to check.
     * @return true if the String is "merge-able", false else.
     */
    public static boolean isMergable(String s) {
       Matcher matcher = mergablePattern.matcher(s);
       return matcher.matches();
    }

    /**
     * Convert a comma delimited String to a Set of Strings.
     *
     * @param text the String to "split".
     * @return the set of Strings.
     */
    public static Set<String> createSetFromString(String text) {
        if(isMergable(text)) {
            text = text.substring(MERGABLE.length() - 3);
            text = text.substring(0,text.length() - 1);
        }
        Set<String> result = new LinkedHashSet<String>();
        if (text != null) {
            String[] items = text.split(",");
            if (items != null && items.length > 0) {

                for (String item : items) {
                    if (item != null && item.length() > 0) {
                        result.add(item.trim());
                    }
                }
            }
        }
        return result;
    }

    /**
     * Convert a set of Strings into a global String.
     *
     * @param items the set of String.
     * @param mergeable true if you want to use the MERRGEABLE string format, false else.
     * @return the global String resulting of the concatenation of the Strings in the Set.
     */
    public static String createStringFromSet(Set<String> items, boolean mergeable) {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iterator = items.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next());
            if (iterator.hasNext()) {
                builder.append(",");
            }
        }
        if (mergeable) {
            return String.format(MERGABLE, builder.toString());
        } else {
            return builder.toString();
        }
    }

    /**
     * Check if two collections contain the same elements.
     *
     * @param col1 the first collection.
     * @param col2 the second collection.
     * @return true if the two collections
     */
    public static boolean collectionEquals(Collection col1, Collection col2) {
        return collectionSubset(col1, col2) && collectionSubset(col2, col1);
    }

    /**
     * Check if the a collection if a subset of another one.
     *
     * @param source the source collection.
     * @param target the target collection.
     * @return true if source is a subset of the target, false else.
     */
    public static boolean collectionSubset(Collection source, Collection target) {
        if (source == null && target == null) {
            return true;
        } else if (source == null || target == null) {
            return false;
        } else if (source.isEmpty() && target.isEmpty()) {
            return true;
        } else {
            for (Object item : source) {
                if (!target.contains(item)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Retrieves the value of the configuration property from the specified configuration. If the property is not found or there is an error
     * retrieving it, return the provided default value.
     *
     * @param configurationAdmin
     *            the config admin service instance
     * @param configurationId
     *            the configuration PID to be retrieved
     * @param propertyKey
     *            the key of the property entry to look up
     * @param defaultValue
     *            a value to be returned, if the property is not present in the configuration or there is an error retrieving it
     * @return the value of the configuration property from the specified configuration. If the property is not found or there is an error
     *         retrieving it, return the provided default value
     */
    public static String getConfigurationProperty(ConfigurationAdmin configurationAdmin, String configurationId,
            String propertyKey, String defaultValue) {
        String propertyValue = null;
        try {
            Configuration configuration = configurationAdmin.getConfiguration(configurationId, null);
            Dictionary<String, Object> properties = configuration.getProperties();
            if (properties != null) {
                propertyValue = (String) properties.get(propertyKey);
            }
        } catch (IOException e) {
            LOGGER.warn("Error while retrieving the " + propertyKey + " entry from coonfiguration " + configurationId,
                    e);
        }

        return propertyValue != null ? propertyValue : defaultValue;
    }

    /**
     * Returns the flag value, indicating if the resources (bundles, configuration, features), not present on cluster, should be uninstalled
     * on cluster sync by corresponding synchronizers.
     * 
     * @param configurationAdmin
     *            the config admin service instance
     * @return the flag value, indicating if the resources (bundles, configuration, features), not present on cluster, should be uninstalled
     *         on cluster sync by corresponding synchronizers
     */
    public static boolean doCleanupResourcesNotPresentInCluster(ConfigurationAdmin configurationAdmin) {
        return Boolean.parseBoolean(getConfigurationProperty(configurationAdmin, Configurations.NODE,
                "org.apache.karaf.cellar.cleanupResourcesNotPresentInCluster", "true"));
    }

}
