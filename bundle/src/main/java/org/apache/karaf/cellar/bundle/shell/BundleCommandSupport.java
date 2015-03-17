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
package org.apache.karaf.cellar.bundle.shell;

import org.apache.karaf.cellar.bundle.BundleState;
import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.shell.commands.Argument;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BundleCommandSupport extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
    List<String> ids;

    protected abstract Object doExecute() throws Exception;

    /**
     * Bundle selector on the cluster.
     *
     * @return the bundle key is the distributed bundle map.
     */
    protected List<String> selector(Map<String, ExtendedBundleState> clusterBundles) {
        List<String> bundles = new ArrayList<String>();

        if (ids != null && !ids.isEmpty()) {
            for (String id : ids) {
                if (id == null) {
                    continue;
                }

                addMatchingBundles(id, bundles, clusterBundles);

            }
        }
        return bundles;
    }

    protected void addMatchingBundles(String nameId, List<String> bundles, Map<String, ExtendedBundleState> clusterBundles) {

        // id is a number
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(nameId);
        if (matcher.find()) {
            int id = Integer.parseInt(nameId);
            for (String bundle : clusterBundles.keySet()) {
                if (clusterBundles.get(bundle).getId() == id) {
                    bundles.add(bundle);
                    break;
                }
            }
            return;
        }

        // id as a number range
        pattern = Pattern.compile("^(\\d+)-(\\d+)$");
        matcher = pattern.matcher(nameId);
        if (matcher.find()) {
            int index = nameId.indexOf('-');
            long startId = Long.parseLong(nameId.substring(0, index));
            long endId = Long.parseLong(nameId.substring(index + 1));
            if (startId < endId) {
                int bundleIndex = 0;
                for (String bundle : clusterBundles.keySet()) {
                    if (bundleIndex >= startId && bundleIndex <= endId) {
                        bundles.add(bundle);
                    }
                    bundleIndex++;
                }
            }
            return;
        }

        int index = nameId.indexOf('/');
        if (index != -1) {
            // id is name/version
            String[] idSplit = nameId.split("/");
            String name = idSplit[0];
            String version = idSplit[1];
            for (String bundle : clusterBundles.keySet()) {
                String[] bundleSplit = bundle.split("/");
                if (bundleSplit[1].equals(version)) {
                    // regex on the name
                    Pattern namePattern = Pattern.compile(name);
                    BundleState state = clusterBundles.get(bundle);
                    if (state.getName() != null) {
                        // bundle name is populated, check if it matches the regex
                        matcher = namePattern.matcher(state.getName());
                        if (matcher.find()) {
                            bundles.add(bundle);
                        } else {
                            // no match on bundle name, fall back to symbolic name and check if it matches the regex
                            matcher = namePattern.matcher(name);
                            if (matcher.find()) {
                                bundles.add(bundle);
                            }
                        }
                    } else {
                        // no bundle name, fall back to symbolic name and check if it matches the regex
                        matcher = namePattern.matcher(name);
                        if (matcher.find()) {
                            bundles.add(bundle);
                        }
                    }
                }
            }
            return;
        }

        // id is just name
        // regex support on the name
        Pattern namePattern = Pattern.compile(nameId);
        // looking for bundle using only the name
        for (String bundle : clusterBundles.keySet()) {
            BundleState state = clusterBundles.get(bundle);
            if (state.getName() != null) {
                // bundle name is populated, check if it matches the regex
                matcher = namePattern.matcher(state.getName());
                if (matcher.find()) {
                    bundles.add(bundle);
                } else {
                    // no match on bundle name, fall back to symbolic name and check if it matches the regex
                    matcher = namePattern.matcher(bundle);
                    if (matcher.find()) {
                        bundles.add(bundle);
                    }
                }
            } else {
                // no bundle name, fall back to symbolic name and check if it matches the regex
                matcher = namePattern.matcher(bundle);
                if (matcher.find()) {
                    bundles.add(bundle);
                }
            }
        }
    }

    protected Map<String, ExtendedBundleState> gatherBundles() {
        Map<String, ExtendedBundleState> bundles = new HashMap<String, ExtendedBundleState>();

        // retrieve bundles from the cluster
        Map<String, BundleState> clusterBundles = clusterManager.getMap(Constants.BUNDLE_MAP + Configurations.SEPARATOR + groupName);
        for (String key : clusterBundles.keySet()) {
            BundleState state = clusterBundles.get(key);
            ExtendedBundleState extendedState = new ExtendedBundleState();
            extendedState.setId(state.getId());
            extendedState.setName(state.getName());
            extendedState.setStatus(state.getStatus());
            extendedState.setLocation(state.getLocation());
            extendedState.setVersion(state.getVersion());
            // extendedState.setData(state.getData());
            extendedState.setCluster(true);
            extendedState.setLocal(false);
            bundles.put(key, extendedState);
        }

        // retrieve local bundles
        for (Bundle bundle : bundleContext.getBundles()) {
            String version = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_VERSION);
            String key = bundle.getSymbolicName() + "/" + version;
            if (bundles.containsKey(key)) {
                ExtendedBundleState extendedState = bundles.get(key);
                extendedState.setLocal(true);
            } else {
                ExtendedBundleState extendedState = new ExtendedBundleState();

                // get the bundle name or location.
                String name = (String) bundle.getHeaders().get(org.osgi.framework.Constants.BUNDLE_NAME);
                // if there is no name, then default to symbolic name.
                name = (name == null) ? bundle.getSymbolicName() : name;
                // if there is no symbolic name, resort to location.
                name = (name == null) ? bundle.getLocation() : name;
                extendedState.setId(bundle.getBundleId());
                extendedState.setName(name);
                extendedState.setVersion(bundle.getVersion().toString());
                extendedState.setLocation(bundle.getLocation());
                int status = bundle.getState();
                if (status == Bundle.ACTIVE)
                    status = BundleEvent.STARTED;
                if (status == Bundle.INSTALLED)
                    status = BundleEvent.INSTALLED;
                if (status == Bundle.RESOLVED)
                    status = BundleEvent.RESOLVED;
                if (status == Bundle.STARTING)
                    status = BundleEvent.STARTING;
                if (status == Bundle.UNINSTALLED)
                    status = BundleEvent.UNINSTALLED;
                if (status == Bundle.STOPPING)
                    status = BundleEvent.STARTED;
                extendedState.setStatus(status);
                extendedState.setCluster(false);
                extendedState.setLocal(true);
                bundles.put(key, extendedState);
            }
        }

        return bundles;
    }

    class ExtendedBundleState extends BundleState {

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
