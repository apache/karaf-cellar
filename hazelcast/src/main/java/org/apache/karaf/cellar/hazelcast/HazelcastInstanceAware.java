package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastInstanceAware {

    protected HazelcastInstance instance;

    public void bind(HazelcastInstance instance) {
        this.instance = instance;
    }

    public void unbind(HazelcastInstance instance) {
        this.instance = null;
    }

    public HazelcastInstance getInstance() {
        return instance;
    }

    public void setInstance(HazelcastInstance instance) {
        this.instance = instance;
    }

}
