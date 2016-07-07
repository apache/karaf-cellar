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
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.utils.CombinedClassLoader;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cluster manager implementation powered by Hazelcast.
 */
public class HazelcastClusterManager extends HazelcastInstanceAware implements ClusterManager {

    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";

    private IdGenerator idgenerator;

    private ConfigurationAdmin configurationAdmin;
    private CombinedClassLoader combinedClassLoader;

    /**
     * Get a Map in Hazelcast.
     *
     * @param mapName the Map name.
     * @return the Map in Hazelcast.
     */
    @Override
    public Map getMap(String mapName) {
        return instance.getMap(mapName);
    }

    /**
     * Get a List in Hazelcast.
     *
     * @param listName the List name.
     * @return the List in Hazelcast.
     */
    @Override
    public List getList(String listName) {
        return instance.getList(listName);
    }

    /**
     * Get a Set in Hazelcast.
     *
     * @param setName the Set name.
     * @return the Set in Hazelcast.
     */
    @Override
    public Set getSet(String setName) {
        return instance.getSet(setName);
    }

    /**
     * Get the list of Hazelcast nodes.
     *
     * @return a Set containing the Hazelcast nodes.
     */
    @Override
    public Set<Node> listNodes() {
        Set<Node> nodes = new HashSet<Node>();

        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Set<Member> members = cluster.getMembers();
            if (members != null && !members.isEmpty()) {
                for (Member member : members) {
                    HazelcastNode node = new HazelcastNode(member);
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    /**
     * Get the nodes with given IDs.
     *
     * @param ids a collection of IDs to look for.
     * @return a Set containing the nodes.
     */
    @Override
    public Set<Node> listNodes(Collection<String> ids) {
        Set<Node> nodes = new HashSet<Node>();
        if (ids != null && !ids.isEmpty()) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        HazelcastNode node = new HazelcastNode(member);
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
     * Get a node with a given ID.
     *
     * @param id the node ID.
     * @return the node.
     */
    @Override
    public Node findNodeById(String id) {
        if (id != null) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        HazelcastNode node = new HazelcastNode(member);
                        if (id.equals(node.getId())) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Node findNodeByAlias(String alias) {
        if (alias != null) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        HazelcastNode node = new HazelcastNode(member);
                        if (alias.equals(node.getAlias())) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Node findNodeByIdOrAlias(String idOrAlias) {
        if (idOrAlias != null) {
            Cluster cluster = instance.getCluster();
            if (cluster != null) {
                Set<Member> members = cluster.getMembers();
                if (members != null && !members.isEmpty()) {
                    for (Member member : members) {
                        HazelcastNode node = new HazelcastNode(member);
                        if (idOrAlias.equals(node.getId())) {
                            return node;
                        }
                        if (idOrAlias.equals(node.getAlias())) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * List the nodes in a given cluster group.
     *
     * @param group the cluster group.
     * @return a Set with the nodes.
     */
    @Override
    public Set<Node> listNodesByGroup(Group group) {
        return group.getNodes();
    }

    /**
     * Generate an unique ID.
     *
     * @return the generated unique ID.
     */
    @Override
    public synchronized String generateId() {
        if (idgenerator == null) {
            idgenerator = instance.getIdGenerator(GENERATOR_ID);
        }
        return String.valueOf(idgenerator.newId());
    }

    @Override
    public void start() {
        // nothing to do
    }

    @Override
    public void stop() {
        if (instance != null && instance.getLifecycleService().isRunning()) {
            instance.getLifecycleService().shutdown();
        }
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public CombinedClassLoader getCombinedClassLoader() {
        return combinedClassLoader;
    }

    public void setCombinedClassLoader(CombinedClassLoader combinedClassLoader) {
        this.combinedClassLoader = combinedClassLoader;
    }

}
