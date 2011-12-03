package org.apache.karaf.cellar.hazelcast.merge;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.hazelcast.core.MapEntry;
import com.hazelcast.merge.MergePolicy;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.MultiNode;
import org.apache.karaf.cellar.core.utils.CellarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CellarMergePolicy  implements MergePolicy {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(CellarMergePolicy.class);
    /**
     * Returns the value of the entry after the merge
     * of entries with the same key. Returning value can be
     * You should consider the case where existingEntry is null.
     *
     * @param mapName       name of the map
     * @param mergingEntry  entry merging into the destination cluster
     * @param existingEntry existing entry in the destination cluster
     * @return final value of the entry. If returns null then no change on the entry.
     */
    public Object merge(String mapName, MapEntry mergingEntry, MapEntry existingEntry) {
        LOGGER.info("Cellar merge policy triggered merging entry {}, existing entry {}",mergingEntry,existingEntry);
        Object mergingDataValue = mergingEntry != null ? mergingEntry.getValue() : null;
        Object existingDataValue = existingEntry != null ? existingEntry.getValue() : null;

        if (existingDataValue == null && mergingDataValue != null) {
            return mergingDataValue;
        }

        else if (mergingDataValue == null && existingDataValue != null) {
            return existingDataValue;
        }

        //Merge MultiNodes by merging their members.
        else if(MultiNode.class.isAssignableFrom(mergingDataValue.getClass())
                && MultiNode.class.isAssignableFrom(existingDataValue.getClass())) {

            MultiNode existingMultiNode = (MultiNode) existingDataValue;
            MultiNode mergingMultiNode = (MultiNode) mergingDataValue;

            existingMultiNode.setNodes(merge(mergingMultiNode.getNodes(), existingMultiNode.getNodes()));
            return existingMultiNode;
        }

        else if(Set.class.isAssignableFrom(mergingDataValue.getClass())
                && Set.class.isAssignableFrom(existingDataValue.getClass())) {
             return merge((Set)mergingDataValue,(Set) existingDataValue);
        }

        else if(List.class.isAssignableFrom(mergingDataValue.getClass())
                && List.class.isAssignableFrom(existingDataValue.getClass())) {
             return merge((List)mergingDataValue,(List) existingDataValue);
        }

        else if (String.class.isAssignableFrom(mergingDataValue.getClass())
                && String.class.isAssignableFrom(existingDataValue.getClass())
                && (CellarUtils.isMergable((String) mergingDataValue) || CellarUtils.isMergable((String) existingDataValue))) {
            return merge((String) mergingDataValue, (String) existingDataValue);
        }

        return existingDataValue;
    }

    /**
     * Merges Sets.
     * @param mergingSet
     * @param existingSet
     * @param <T>
     * @return
     */
    public <T> Set<T> merge(Set<T> mergingSet, Set<T> existingSet) {
       Set<T> result = new LinkedHashSet<T>();

       //Copy new Set.
       if(mergingSet != null && !mergingSet.isEmpty()) {
           for(T obj:mergingSet) {
               result.add(obj);
           }
       }

       //Copy existing Set.
       if(existingSet != null && !existingSet.isEmpty()) {
           for(T obj:existingSet) {
               result.add(obj);
           }
       }

       return result;
    }


    /**
     * Merges Lists.
     * @param mergingList
     * @param existingList
     * @param <T>
     * @return
     */
    public <T> List<T> merge(List<T> mergingList, List<T> existingList) {
       List<T> result = new LinkedList<T>();

       //Copy existing List.
       if(existingList != null && !existingList.isEmpty()) {
           for(T obj:existingList) {
               result.add(obj);
           }
       }

       //Copy new List.
       if(mergingList != null && !mergingList.isEmpty()) {
           for(T obj:mergingList) {
               result.add(obj);
           }
       }

       return result;
    }

    /**
     * Merges Strings.
     * @param mergingString
     * @param existingString
     * @return
     */
    public String merge(String mergingString, String existingString) {
       String result = existingString;
       Set<String> items = new LinkedHashSet<String>();
       items.addAll(CellarUtils.createSetFromString(mergingString));
       items.addAll(CellarUtils.createSetFromString(existingString));
       if (!items.isEmpty()) {
           result = CellarUtils.createStringFromSet(items,true);
       }
       return result;
    }
}
