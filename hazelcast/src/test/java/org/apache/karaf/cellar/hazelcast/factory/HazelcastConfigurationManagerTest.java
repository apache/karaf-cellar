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
