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
package org.apache.karaf.cellar.shell;

import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.Synchronizer;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Command(scope = "cluster", name = "sync", description = "Manipulate the synchronizers")
@Service
public class SyncCommand extends ClusterCommandSupport {

    @Option(name = "-g", aliases = { "--group" }, description = "The cluster group name", required = false, multiValued = false)
    private String groupName;

    @Option(name = "-b", aliases = { "--bundle" }, description = "Cluster bundle support", required = false, multiValued = false)
    private boolean bundleSync = false;

    @Option(name = "-c", aliases = { "--config" }, description = "Cluster config support", required = false, multiValued = false)
    private boolean configSync = false;

    @Option(name = "-f", aliases = { "--feature" }, description = "Cluster feature support", required = false, multiValued = false)
    private boolean featuresSync = false;

    @Option(name = "-o", aliases = { "--obr" }, description = "Cluster OBR support", required = false, multiValued = false)
    private boolean obrSync = false;

    @Argument(name = "policy", description = "The definition of the sync policy for the given cluster resource", required = false, multiValued = false)
    private String policy;

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Reference
    private BundleContext bundleContext;

    @Override
    protected Object doExecute() throws Exception {
        boolean allResources = false;
        // if the user didn't provide any resource, we consider all
        if (!bundleSync && !configSync && !featuresSync && !obrSync) {
            allResources = true;
        }
        Set<Group> groups;
        if (groupName == null || groupName.isEmpty()) {
            groups = groupManager.listLocalGroups();
        } else {
            groups = new HashSet<Group>();
            if (groupManager.findGroupByName(groupName) == null) {
                System.err.println("Cluster group " + groupName + " doesn't exist");
                return null;
            }
            groups.add(groupManager.findGroupByName(groupName));
        }
        if (policy == null || policy.isEmpty()) {
            // we are in sync mode
            // constructing the target cluster groups
            for (Group group : groups) {
                System.out.println("Synchronizing cluster group " + group.getName());
                if (bundleSync || allResources) {
                    doSync("bundle", group);
                }
                if (configSync || allResources) {
                    doSync("config", group);
                }
                if (featuresSync || allResources) {
                    doSync("feature", group);
                }
                if (obrSync || allResources) {
                    doSync("obr.urls", group);
                }
            }
        } else {
            // we are in set mode
            if (!policy.equalsIgnoreCase("cluster") && !policy.equalsIgnoreCase("node") && !policy.equalsIgnoreCase("clusterOnly") && !policy.equalsIgnoreCase("nodeOnly") && !policy.equalsIgnoreCase("disabled")) {
                System.err.println("The sync policy " + policy + " is not valid. Valid sync policies are: cluster, node, clusterOnly, nodeOnly, disabled");
                return null;
            }
            for (Group group : groups) {
                System.out.println("Updating sync policy for cluster group " + group.getName());
                if (bundleSync || allResources) {
                    updateSync("bundle", group, policy);
                }
                if (configSync || allResources) {
                    updateSync("config", group, policy);
                }
                if (featuresSync || allResources) {
                    updateSync("feature", group, policy);
                }
                if (obrSync || allResources) {
                    updateSync("obr.urls", group, policy);
                }
            }
        }
        return null;
    }

    private void doSync(String resource, Group group) throws Exception {
        // looking for the resource synchronizer
        System.out.print("\t" + resource + ": ");
        ServiceReference[] references = bundleContext.getAllServiceReferences(Synchronizer.class.getName(), "(resource=" + resource + ")");
        if (references != null && references.length > 0) {
            for (ServiceReference reference : references) {
                Synchronizer synchronizer = (Synchronizer) bundleContext.getService(reference);
                synchronizer.sync(group);
                bundleContext.ungetService(reference);
            }
            System.out.println("done");
        } else {
            System.out.println("No synchronizer found for " + resource);
        }
    }

    private void updateSync(String resource, Group group, String policy) throws Exception {
        System.out.print("\t" + resource + ": ");
        Configuration configuration = configurationAdmin.getConfiguration(Configurations.GROUP, null);
        if (configuration != null) {
            Dictionary properties = configuration.getProperties();
            if (properties == null)
                properties = new Properties();
            properties.put(group.getName() + "." + resource + ".sync", policy);
            configuration.update(properties);
            System.out.println("done");
        }
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }
}
