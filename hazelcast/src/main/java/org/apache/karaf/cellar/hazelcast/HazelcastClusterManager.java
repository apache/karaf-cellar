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
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.EventConsumer;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hazelcast cluster manager.
 */
public class HazelcastClusterManager implements ClusterManager {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HazelcastClusterManager.class);

    private static final String GENERATOR_ID = "org.apache.karaf.cellar.idgen";

    private HazelcastInstance instance;
    private IdGenerator idgenerator;

    private List<EventProducer> producerList;
    private List<EventConsumer> consumerList;

    private ConfigurationAdmin configurationAdmin;

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

    @Override
    public EventProducer getEventProducer(String groupName) {
        ITopic topic = instance.getTopic(Constants.TOPIC + "." + groupName);
        TopicProducer producer = new TopicProducer();
        producer.setTopic(topic);
        producer.setNode(getNode());
        return producer;
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
     * Returns the node on which the command was run.
     *
     * @return
     */
    public Node getNode() {
        Cluster cluster = instance.getCluster();
        if (cluster != null) {
            Member member = cluster.getLocalMember();
            return new HazelcastNode(member.getInetSocketAddress().getHostName(), member.getInetSocketAddress().getPort());
        } else return null;
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

    @Override
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

    /**
     * Returns the Hazelcast instance.
     *
     * @return
     */
    public HazelcastInstance getInstance() {
        return instance;
    }

    /**
     * Sets the Hazelcast instance.
     *
     * @param instance
     */
    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public List<EventProducer> getProducerList() {
        return producerList;
    }

    public void setProducerList(List<EventProducer> producerList) {
        this.producerList = producerList;
    }

    public List<EventConsumer> getConsumerList() {
        return consumerList;
    }

    public void setConsumerList(List<EventConsumer> consumerList) {
        this.consumerList = consumerList;
    }

}
