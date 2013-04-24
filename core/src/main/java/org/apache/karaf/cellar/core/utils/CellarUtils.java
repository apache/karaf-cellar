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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic Cellar utils class.
 */
public class CellarUtils {

    public static enum MergeType {
        MERGA
    }

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
}
