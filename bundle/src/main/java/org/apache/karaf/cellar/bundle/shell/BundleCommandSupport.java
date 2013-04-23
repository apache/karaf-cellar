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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BundleCommandSupport extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    @Argument(index = 1, name = "id", description = "The bundle ID or name", required = true, multiValued = false)
    String name;

    @Argument(index = 2, name = "version", description = "The bundle version", required = false, multiValued = false)
    String version;

    protected abstract Object doExecute() throws Exception;

    /**
     * Bundle selector on the cluster.
     *
     * @return the bundle key is the distributed bundle map.
     */
    protected String selector(Map<String, BundleState> clusterBundles) {
        String key = null;
        if (version == null) {
            // looking for bundle using ID
            int id = -1;
            try {
                id = Integer.parseInt(name);
                int index = 0;
                for (String bundle : clusterBundles.keySet()) {
                    if (index == id) {
                        key = bundle;
                        break;
                    }
                    index++;
                }
            } catch (NumberFormatException nfe) {
                // ignore
            }
            if (id == -1) {

                // add regex support
                Pattern namePattern = Pattern.compile(name);

                // looking for bundle using only the name
                for (String bundle : clusterBundles.keySet()) {
                    BundleState state = clusterBundles.get(bundle);
                    if (state.getName() != null) {
                        // bundle name is populated, check if it matches the regex
                        Matcher matcher = namePattern.matcher(state.getName());
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        } else {
                            // not matched on bundle name, fall back to symbolic name and check if it matches the regex
                            String split[] = bundle.split("/");
                            matcher = namePattern.matcher(split[0]);
                            if (matcher.find()) {
                                key = bundle;
                                break;
                            }
                        }
                    } else {
                        // no bundle name, fall back to symbolic name and check if it matches the regex
                        String split[] = bundle.split("/");
                        System.out.println("Bundle-SymbolicName: " + split[0]);
                        Matcher matcher = namePattern.matcher(split[0]);
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        }
                    }
                }
            }
        } else {
            // looking for the bundle using name and version

            // add regex support of the name
            Pattern namePattern = Pattern.compile(name);

            for (String bundle : clusterBundles.keySet()) {
                String[] split = bundle.split("/");
                BundleState state = clusterBundles.get(bundle);
                if (split[1].equals(version)) {
                    if (state.getName() != null) {
                        // bundle name is populated, check if it matches the regex
                        Matcher matcher = namePattern.matcher(state.getName());
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        } else {
                            // no match on bundle name, fall back to symbolic name and check if it matches the regex
                            matcher = namePattern.matcher(split[0]);
                            if (matcher.find()) {
                                key = bundle;
                                break;
                            }
                        }
                    } else {
                        // no bundle name, fall back to symbolic name and check if it matches the regex
                        Matcher matcher = namePattern.matcher(split[0]);
                        if (matcher.find()) {
                            key = bundle;
                            break;
                        }
                    }
                }
            }
        }
        return key;
    }

}
