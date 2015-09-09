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
package org.apache.karaf.cellar.features.shell;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.core.shell.completer.AllGroupsCompleter;
import org.apache.karaf.cellar.features.Constants;
import org.apache.karaf.cellar.features.FeatureState;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.*;

@Command(scope = "cluster", name = "feature-list", description = "List the features in a cluster group")
@Service
public class ListFeaturesCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    @Completion(AllGroupsCompleter.class)
    String groupName;

    @Option(name = "-i", aliases = { "--installed" }, description = "Display only installed features", required = false, multiValued = false)
    boolean installed;

    @Option(name = "-o", aliases = { "--ordered" }, description = "Display a list using alphabetical order", required = false, multiValued = false)
    boolean ordered;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Option(name = "--cluster", description = "Shows only features on the cluster", required = false, multiValued = false)
    boolean onlyCluster;

    @Option(name = "--local", description = "Shows only features on the local node", required = false, multiValued = false)
    boolean onlyLocal;

    @Option(name = "--blocked", description = "Shows only blocked features", required = false, multiValued = false)
    boolean onlyBlocked;

    @Reference
    private FeaturesService featuresService;

    @Override
    protected Object doExecute() throws Exception {
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        CellarSupport support = new CellarSupport();
        support.setClusterManager(clusterManager);
        support.setGroupManager(groupManager);
        support.setConfigurationAdmin(configurationAdmin);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Map<String, ExtendedFeatureState> features = gatherFeatures();

            if (features != null && !features.isEmpty()) {

                ShellTable table = new ShellTable();
                table.column("Name");
                table.column("Version");
                table.column("Installed");
                table.column("Located");
                table.column("Blocked");

                List<ExtendedFeatureState> featureStates = new ArrayList<ExtendedFeatureState>(features.values());
                if (ordered) {
                    Collections.sort(featureStates, new FeatureComparator());
                }
                for (ExtendedFeatureState info : featureStates) {

                    String name = info.getName();
                    String version = info.getVersion();
                    boolean isInstalled = info.getInstalled();

                    String located = "";
                    boolean cluster = info.isCluster();
                    boolean local = info.isLocal();
                    if (cluster && local)
                        located = "cluster/local";
                    if (local && !cluster) {
                        located = "local";
                        if (onlyCluster)
                            continue;
                    }
                    if (cluster && !local) {
                        located = "cluster";
                        if (onlyLocal)
                            continue;
                    }

                    String blocked = "";
                    boolean inbound = support.isAllowed(group, Constants.CATEGORY, name, EventType.INBOUND);
                    boolean outbound = support.isAllowed(group, Constants.CATEGORY, name, EventType.OUTBOUND);
                    if (inbound && outbound && onlyBlocked)
                        continue;
                    if (!inbound && !outbound)
                        blocked = "in/out";
                    if (!inbound && outbound)
                        blocked = "in";
                    if (!outbound && inbound)
                        blocked = "out";

                    if (version == null)
                        version = "";
                    if (!installed || (installed && isInstalled)) {
                        table.addRow().addContent(name, version,
                                isInstalled ? "x" : " ",
                                located, blocked);
                    }
                }

                table.print(System.out, !noFormat);
            } else System.err.println("No features in cluster group " + groupName);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }

    private Map<String, ExtendedFeatureState> gatherFeatures() throws Exception {
        Map<String, ExtendedFeatureState> features = new HashMap<String, ExtendedFeatureState>();

        // get cluster features
        Map<String, FeatureState> clusterFeatures = clusterManager.getMap(Constants.FEATURES_MAP + Configurations.SEPARATOR + groupName);
        for (String key : clusterFeatures.keySet()) {
            FeatureState state = clusterFeatures.get(key);
            ExtendedFeatureState extendedState = new ExtendedFeatureState();
            extendedState.setName(state.getName());
            extendedState.setInstalled(state.getInstalled());
            extendedState.setVersion(state.getVersion());
            extendedState.setCluster(true);
            extendedState.setLocal(true);
            features.put(key, extendedState);
        }

        // get local features
        for (Feature feature : featuresService.listFeatures()) {
            String key = feature.getName() + "/" + feature.getVersion();
            if (features.containsKey(key)) {
                ExtendedFeatureState extendedState = features.get(key);
                if (featuresService.isInstalled(feature))
                    extendedState.setInstalled(true);
                extendedState.setLocal(true);
            } else {
                ExtendedFeatureState extendedState = new ExtendedFeatureState();
                extendedState.setCluster(false);
                extendedState.setLocal(true);
                extendedState.setName(feature.getName());
                extendedState.setVersion(feature.getVersion());
                if (featuresService.isInstalled(feature))
                    extendedState.setInstalled(true);
                else extendedState.setInstalled(false);
                features.put(key, extendedState);
            }
        }

        return features;
    }

    public FeaturesService getFeaturesService() {
        return featuresService;
    }

    public void setFeaturesService(FeaturesService featuresService) {
        this.featuresService = featuresService;
    }

    class FeatureComparator implements Comparator<FeatureState> {
        public int compare(FeatureState f1, FeatureState f2) {
            return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
        }
    }

    class ExtendedFeatureState extends FeatureState {

        private boolean cluster;
        private boolean local;

        public boolean isCluster() {
            return cluster;
        }

        public void setCluster(boolean cluster) {
            this.cluster = cluster;
        }

        public boolean isLocal() {
            return local;
        }

        public void setLocal(boolean local) {
            this.local = local;
        }
    }

}
