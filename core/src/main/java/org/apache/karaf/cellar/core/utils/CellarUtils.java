package org.apache.karaf.cellar.core.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CellarUtils {

    public static enum MergeType {
        MERGA
    }

    public static final String MERGABLE = "MERGABLE[%s]";
    public static final String MERGABLE_REGEX = "MERGABLE\\[([^\\s]+[\\,]*[\\s]*)*\\]";

    private static final Pattern mergablePattern = Pattern.compile(MERGABLE_REGEX);


    /**
     * Returns true if String is mergable.
     * @param s
     * @return
     */
    public static boolean isMergable(String s) {
       Matcher matcher = mergablePattern.matcher(s);
       return matcher.matches();
    }

    /**
     * Converts a comma delimited String to a Set of Strings.
     *
     * @param text
     * @return
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
     * Creates a comma delimited list of items.
     *
     * @param items
     * @return
     */
    public static String createStringFromSet(Set<String> items, boolean mergable) {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iterator = items.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next());
            if (iterator.hasNext()) {
                builder.append(",");
            }
        }
        if (mergable) {
            return String.format(MERGABLE, builder.toString());
        } else {
            return builder.toString();
        }
    }

    /**
     * Returns true if both {@link java.util.Collection}s contain exactly the same items (order doesn't matter).
     *
     * @param col1
     * @param col2
     * @return
     */
    public static boolean collectionEquals(Collection col1, Collection col2) {
        return collectionSubset(col1, col2) && collectionSubset(col2, col1);
    }

    /**
     * Returns true if one {@link Collection} contains all items of the others
     *
     * @param source
     * @param target
     * @return
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
