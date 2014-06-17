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
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.shell.commands.Argument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BundleCommandSupport extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = true, multiValued = true)
    List<String> ids;

    protected abstract Object doExecute() throws Exception;

    /**
     * Bundle selector on the cluster.
     *
     * @return the bundle key is the distributed bundle map.
     */
    protected List<String> selector(Map<String, BundleState> clusterBundles) {
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

    protected void addMatchingBundles(String id, List<String> bundles, Map<String, BundleState> clusterBundles) {

        // id is a number
        Pattern pattern = Pattern.compile("^\\d+$");
        Matcher matcher = pattern.matcher(id);
        if (matcher.find()) {
            int idInt = Integer.parseInt(id);
            int index = 0;
            for (String bundle : clusterBundles.keySet()) {
                if (index == idInt) {
                    bundles.add(bundle);
                    break;
                }
                index++;
            }
            return;
        }

        // id as a number range
        pattern = Pattern.compile("^(\\d+)-(\\d+)$");
        matcher = pattern.matcher(id);
        if (matcher.find()) {
            int index = id.indexOf('-');
            long startId = Long.parseLong(id.substring(0, index));
            long endId = Long.parseLong(id.substring(index + 1));
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

        int index = id.indexOf('/');
        if (index != -1) {
            // id is name/version
            String[] idSplit = id.split("/");
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
        Pattern namePattern = Pattern.compile(id);
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

}
