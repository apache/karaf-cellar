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
package org.apache.karaf.cellar.hazelcast.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import org.apache.karaf.cellar.core.discovery.Discovery;
import org.apache.karaf.cellar.core.utils.CellarUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastConfigurationManager {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastServiceFactory.class);

    private String xmlConfigLocation = System.getProperty("karaf.home") + "/etc/hazelcast.xml";

    private Set<String> discoveredMemberSet = new LinkedHashSet<String>();

     /**
     * Builds a Hazelcast {@link com.hazelcast.config.Config}
     *
     * @return
     */
    public Config getHazelcastConfig() {
        System.setProperty("hazelcast.config", xmlConfigLocation);
        Config config = new XmlConfigBuilder().build();
        if (discoveredMemberSet != null) {
            TcpIpConfig tcpIpConfig = config.getNetworkConfig().getJoin().getTcpIpConfig();
            tcpIpConfig.getMembers().addAll(discoveredMemberSet);
        }
        return config;
    }

    /**
     * Update Hazelcast config with discovered members.
     *
     * @param properties the map containing the discovered members.
     * @return true if the config has been updated, false else.
     */
    public boolean isUpdated(Map properties) {
        Boolean updated = Boolean.FALSE;
        if (properties != null) {
            if (properties.containsKey(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME)) {
                Set<String> newDiscoveredMemberSet = CellarUtils.createSetFromString((String) properties.get(Discovery.DISCOVERED_MEMBERS_PROPERTY_NAME));
                if (!CellarUtils.collectionEquals(discoveredMemberSet, newDiscoveredMemberSet)) {
                    LOGGER.info("Hazelcast discoveredMemberSet has been changed from {} to {}", discoveredMemberSet, newDiscoveredMemberSet);
                    discoveredMemberSet = newDiscoveredMemberSet;
                    updated = Boolean.TRUE;
                }
            }
        }
        return updated;
    }

	/**
	 * @return the discoveredMemberSet
	 */
	public Set<String> getDiscoveredMemberSet() {
		return discoveredMemberSet;
	}

}
