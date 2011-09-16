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
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Dispatcher;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hazelcast cluster manager.
 */
public class HazelcastClusterManager extends HazelcastInstanceAware implements ClusterManager {

    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";

    private IdGenerator idgenerator;

    private ConfigurationAdmin configurationAdmin;
    private CombinedClassLoader combinedClassLoader;

    /**
     * Returns a named distributed map.
     *
     * @param mapName
     * @return
     */
    public Map getMap(String mapName) {
        return instance.getMap(mapName);
    }

    /**
     * Returns a named distributed list.
     *
     * @param listName
     * @return
     */
    public List getList(String listName) {
        return instance.getList(listName);
    }

    /**
     * Returns a named distributed set.
     * @param setName
     * @return
     */
    public Set getSet(String setName) {
        return instance.getSet(setName);
    }    

    /**
     * Returns the list of Hazelcast Nodes.
     *
     * @return
     */
    public Set<Node> listNodes() {
        Set<Node> nodes = new HashSet<Node>();

        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Set<Member> members = cluster.getMembers();
            if (members != null && !members.isEmpty()) {
                for (Member member : members) {
                    HazelcastNode node = new HazelcastNode(member.getInetSocketAddress().getHostName(), member.getInetSocketAddress().getPort());
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }



    /**
     * Returns the {@code Node}s with the corresponding ids.
     *
     * @param ids
     * @return
     */
    public Set<Node> listNodes(Collection<String> ids) {
        Set<Node> nodes = new HashSet<Node>();
        if (ids != null && !ids.isEmpty()) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        HazelcastNode node = new HazelcastNode(member.getInetSocketAddress().getHostName(), member.getInetSocketAddress().getPort());
                        if (ids.contains(node.getId())) {
                            nodes.add(node);
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * Returns the {@code Node} with the corresponding id.
     *
     * @param id
     * @return
     */
    public Node findNodeById(String id) {
        if (id != null) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        HazelcastNode node = new HazelcastNode(member.getInetSocketAddress().getHostName(), member.getInetSocketAddress().getPort());
                        if (id.equals(node.getId())) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    public Set<Node> listNodesByGroup(Group group) {
        return group.getMembers();
    }

    /**
     * Generate an id.
     *
     * @return
     */
    public synchronized String generateId() {
        if (idgenerator == null) {
            idgenerator = instance.getIdGenerator(GENERATOR_ID);
        }
        return String.valueOf(idgenerator.newId());
    }

    public void start() {

    }

    public void stop() {
        instance.shutdown();
    }

    public void restart() {
        instance.restart();
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public CombinedClassLoader getCombinedClassLoader() {
        return this.combinedClassLoader;
    }

    public void setCombinedClassLoader(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }

}
