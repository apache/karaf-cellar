package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.Node;

public class HazelcastInstanceAware {

    protected HazelcastInstance instance;

    public void bind(HazelcastInstance instance) {
        this.instance = instance;
    }

    public void unbind(HazelcastInstance instance) {
        this.instance = null;
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
