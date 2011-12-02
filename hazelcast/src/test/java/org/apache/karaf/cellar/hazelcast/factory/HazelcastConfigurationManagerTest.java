package org.apache.karaf.cellar.hazelcast.factory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HazelcastConfigurationManagerTest {

    @Test
    public void testCollectionEquals() {
        HazelcastConfigurationManager cm = new HazelcastConfigurationManager();
        //Lists
        Assert.assertTrue(cm.collectionEquals(null,null));
        Assert.assertTrue(cm.collectionEquals(new LinkedList(),new LinkedList()));
        Assert.assertTrue(cm.collectionEquals(new LinkedList(),new ArrayList()));

        Assert.assertTrue(cm.collectionEquals(Arrays.asList(""),Arrays.asList("")));
        Assert.assertTrue(cm.collectionEquals(Arrays.asList("1"),Arrays.asList("1")));
        Assert.assertFalse(cm.collectionEquals(Arrays.asList("1"), Arrays.asList("2")));
        Assert.assertTrue(cm.collectionEquals(Arrays.asList("1","2"),Arrays.asList("2","1")));
        Assert.assertFalse(cm.collectionEquals(Arrays.asList("1", "2"), Arrays.asList("2", "1", "3")));

        //Sets
        Assert.assertTrue(cm.collectionEquals(new LinkedHashSet(),new LinkedHashSet()));
        Set<String> s1 = new LinkedHashSet<String>();
        Set<String> s2 = new LinkedHashSet<String>();

        s1.add("");
        s2.add("");
        Assert.assertTrue(cm.collectionEquals(s1,s2));
        System.out.println(String.format("%s %s",s1,s2));
    }

    @Test
    public void testIsUpdatedWithNoProperties()  {
        HazelcastConfigurationManager cm = new HazelcastConfigurationManager();
        Assert.assertFalse(cm.isUpdated(new Properties()));
    }

    @Test
    public void testIsUpdatedWithEmptyTcpMembers()  {
        HazelcastConfigurationManager cm = new HazelcastConfigurationManager();
        Properties p = new Properties();
        p.put(HazelcastConfigurationManager.TCPIP_MEMBERS,"");
        Assert.assertFalse(cm.isUpdated(p));
    }
}
