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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

public class CellarUtilsTest {

    @Test
    public void testCollectionEquals() {

        //Lists
        Assert.assertTrue(CellarUtils.collectionEquals(null, null));
        Assert.assertTrue(CellarUtils.collectionEquals(new LinkedList(),new LinkedList()));
        Assert.assertTrue(CellarUtils.collectionEquals(new LinkedList(),new ArrayList()));

        Assert.assertTrue(CellarUtils.collectionEquals(Arrays.asList(""),Arrays.asList("")));
        Assert.assertTrue(CellarUtils.collectionEquals(Arrays.asList("1"),Arrays.asList("1")));
        Assert.assertFalse(CellarUtils.collectionEquals(Arrays.asList("1"), Arrays.asList("2")));
        Assert.assertTrue(CellarUtils.collectionEquals(Arrays.asList("1","2"),Arrays.asList("2","1")));
        Assert.assertFalse(CellarUtils.collectionEquals(Arrays.asList("1", "2"), Arrays.asList("2", "1", "3")));

        //Sets
        Assert.assertTrue(CellarUtils.collectionEquals(new LinkedHashSet(),new LinkedHashSet()));
        Set<String> s1 = new LinkedHashSet<String>();
        Set<String> s2 = new LinkedHashSet<String>();

        s1.add("");
        s2.add("");
        Assert.assertTrue(CellarUtils.collectionEquals(s1,s2));
        System.out.println(String.format("%s %s",s1,s2));
    }

    @Test
    public void testMergableRegex() {
        Pattern p = Pattern.compile(CellarUtils.MERGABLE_REGEX);
        Assert.assertEquals(true,p.matcher(String.format(CellarUtils.MERGABLE,"")).matches());
        Assert.assertEquals(true,p.matcher(String.format(CellarUtils.MERGABLE,"item1")).matches());
        Assert.assertEquals(true,p.matcher(String.format(CellarUtils.MERGABLE,"item1,item2")).matches());
        Assert.assertEquals(true,p.matcher(String.format(CellarUtils.MERGABLE,"item1, item2")).matches());
    }


    @Test
    public void testSetFromString() {
        Set<String> itemSet = new LinkedHashSet<String>();
        String items = "item1,item2";
        itemSet.add("item1");
        itemSet.add("item2");

        Set<String> resultSet = CellarUtils.createSetFromString(items);
        Assert.assertTrue(CellarUtils.collectionEquals(itemSet,resultSet));

        resultSet = CellarUtils.createSetFromString(String.format(CellarUtils.MERGABLE,items));
        Assert.assertTrue(CellarUtils.collectionEquals(itemSet,resultSet));
    }

    @Test
    public void testStringFromSet() {
        Set<String> itemSet = new LinkedHashSet<String>();
        String items = "item1,item2";
        itemSet.add("item1");
        itemSet.add("item2");

        String result = CellarUtils.createStringFromSet(itemSet,false);
        Assert.assertEquals(items,result);

        result = CellarUtils.createStringFromSet(itemSet,true);
        Assert.assertEquals(String.format(CellarUtils.MERGABLE,items),result);
    }

}
