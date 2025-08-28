/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.itests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class CellarGroupsTest extends CellarTestSupport {

    @Test
    public void testGroups() throws InterruptedException {
        installCellar();

        String groupList = executeCommand("cluster:group-list");
        System.out.println(groupList);
        String[] groups = groupList.split("\n");
        Assert.assertEquals(3, groups.length);
        assertContains("default", groups[2]);

        System.out.println("Creating test cluster group ...");
        System.out.println(executeCommand("cluster:group-create test"));

        groupList = executeCommand("cluster:group-list");
        System.out.println(groupList);
        groups = groupList.split("\n");
        Assert.assertEquals(4, groups.length);
        assertContains("test", groups[2]);
        assertContains("default", groups[3]);

        System.out.println("Local node join the test cluster group ...");
        groupList = executeCommand("cluster:group-join test");
        System.out.println(groupList);
        groups = groupList.split("\n");
        assertContains("x", groups[2]);

        System.out.println("Local node quit the test cluster group ...");
        groupList = executeCommand("cluster:group-quit test");
        System.out.println(groupList);
        groups = groupList.split("\n");
        assertContainsNot("x", groups[2]);

        System.out.println("Delete test cluster group ...");
        executeCommand("cluster:group-delete test");
        groupList = executeCommand("cluster:group-list");
        System.out.println(groupList);
        groups = groupList.split("\n");
        Assert.assertEquals(3, groups.length);
        assertContains("default", groups[2]);
    }

}
