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

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.Node;

/**
 * Hazelcast instance aware.
 */
public class HazelcastInstanceAware {

    protected HazelcastInstance instance;

    public void bind(HazelcastInstance instance) {
        this.instance = instance;
    }

    public void unbind(HazelcastInstance instance) {
        this.instance = null;
    }

    /**
     * Return the local node.
     *
     * @return the local node.
     */
    public Node getNode() {
        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Member member = cluster.getLocalMember();
            return new HazelcastNode(member);
        } else {
            return null;
        }
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

}
