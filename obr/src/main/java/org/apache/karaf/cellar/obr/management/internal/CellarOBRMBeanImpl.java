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
package org.apache.karaf.cellar.obr.management.internal;

import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.*;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventProducer;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.cellar.obr.ObrBundleEvent;
import org.apache.karaf.cellar.obr.ObrBundleInfo;
import org.apache.karaf.cellar.obr.ObrUrlEvent;
import org.apache.karaf.cellar.obr.management.CellarOBRMBean;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import javax.management.openmbean.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the Cellar OBR MBean.
 */
public class CellarOBRMBeanImpl extends StandardMBean implements CellarOBRMBean {

    private ClusterManager clusterManager;
    private GroupManager groupManager;
    private EventProducer eventProducer;
    private ConfigurationAdmin configurationAdmin;
    private RepositoryAdmin obrService;

    public CellarOBRMBeanImpl() throws NotCompliantMBeanException {
        super(CellarOBRMBean.class);
    }

    public List<String> listUrls(String groupName) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        List<String> result = new ArrayList<String>();
        Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        for (String url : urls) {
            result.add(url);
        }
        return result;
    }

    public TabularData listBundles(String groupName) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        CompositeType compositeType = new CompositeType("OBR Bundle", "Bundles available in the OBR service",
                new String[]{ "name", "symbolic", "version"},
                new String[]{ "Name of the bundle", "Symbolic name of the bundle", "Version of the bundle" },
                new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING });
        TabularType tableType = new TabularType("OBR Bundles", "Table of all bundles available in the OBR service", compositeType,
                new String[]{"name", "version"});
        TabularData table = new TabularDataSupport(tableType);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
            for (ObrBundleInfo info : bundles) {
                CompositeData data = new CompositeDataSupport(compositeType,
                        new String[]{ "name", "symbolic", "version" },
                        new Object[]{ info.getPresentationName(), info.getSymbolicName(), info.getVersion() });
                table.put(data);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return table;
    }

    public void addUrl(String groupName, String url) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the URL is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.URLS_CONFIG_CATEGORY, url, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("OBR URL " + url + " is blocked outbound");
        }

        // push the OBR URL in the distributed set
        Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        urls.add(url);
        // push the bundles in the OBR distributed set
        Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        synchronized (obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                bundles.add(info);
            }
            obrService.removeRepository(url);
        }

        // create an cluster event and produce it
        ObrUrlEvent event = new ObrUrlEvent(url, Constants.URL_ADD_EVENT_TYPE);
        event.setForce(true);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void removeUrl(String groupName, String url) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the URL is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.URLS_CONFIG_CATEGORY, url, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("OBR URL " + url + " is blocked outbound");
        }

        // remove URL from the distributed map
        Set<String> urls = clusterManager.getSet(Constants.URLS_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        urls.remove(url);
        // remove bundles from the distributed map
        Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
        synchronized (obrService) {
            Repository repository = obrService.addRepository(url);
            Resource[] resources = repository.getResources();
            for (Resource resource : resources) {
                ObrBundleInfo info = new ObrBundleInfo(resource.getPresentationName(), resource.getSymbolicName(), resource.getVersion().toString());
                bundles.remove(info);
            }
            obrService.removeRepository(url);
        }

        // create an event and produce it
        ObrUrlEvent event = new ObrUrlEvent(url, Constants.URL_REMOVE_EVENT_TYPE);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public void deploy(String groupName, String bundleId) throws Exception {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        // check if the producer is ON
        if (eventProducer.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            throw new IllegalStateException("Cluster event producer is OFF");
        }

        // check if the bundle ID is allowed outbound
        CellarSupport support = new CellarSupport();
        support.setClusterManager(this.clusterManager);
        support.setGroupManager(this.groupManager);
        support.setConfigurationAdmin(this.configurationAdmin);
        if (!support.isAllowed(group, Constants.BUNDLES_CONFIG_CATEGORY, bundleId, EventType.OUTBOUND)) {
            throw new IllegalArgumentException("OBR bundle " + bundleId + " is blocked outbound");
        }

        // create an event and produce it
        int type = 0;
        ObrBundleEvent event = new ObrBundleEvent(bundleId, type);
        event.setForce(true);
        event.setSourceGroup(group);
        eventProducer.produce(event);
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public EventProducer getEventProducer() {
        return eventProducer;
    }

    public void setEventProducer(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public RepositoryAdmin getObrService() {
        return obrService;
    }

    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

}
