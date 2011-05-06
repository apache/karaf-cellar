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

package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.core.Synchronizer;

import java.util.List;
import java.util.Set;

/**
 * @author: iocanel
 */
public class CellarMembershipListener implements MembershipListener {

    private GroupManager groupManager;
    private HazelcastInstance instance;
    private List<? extends Synchronizer> synchronizers;

    public CellarMembershipListener(HazelcastInstance instance) {
        this.instance = instance;
        instance.getCluster().addMembershipListener(this);
    }

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        Member local = instance.getCluster().getLocalMember();

        if (local.equals(member)) {
            if (synchronizers != null && !synchronizers.isEmpty()) {
                Set<Group> groups = groupManager.listLocalGroups();
                if (groups != null && !groups.isEmpty()) {
                    for (Group group : groups) {
                        for (Synchronizer synchronizer : synchronizers) {
                            if (synchronizer.isSyncEnabled(group)) {
                                synchronizer.pull(group);
                                synchronizer.push(group);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {

    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public List<? extends Synchronizer> getSynchronizers() {
        return synchronizers;
    }

    public void setSynchronizers(List<? extends Synchronizer> synchronizers) {
        this.synchronizers = synchronizers;
    }
}
