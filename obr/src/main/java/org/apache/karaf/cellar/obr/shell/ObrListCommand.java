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
package org.apache.karaf.cellar.obr.shell;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.obr.Constants;
import org.apache.karaf.cellar.obr.ObrBundleInfo;

import java.util.Set;

/**
 * cluster:obr-list command.
 */
@Command(scope = "cluster", name = "obr-list", description = "List available bundles in the OBR of all nodes assigned to a cluster group")
public class ObrListCommand extends CellarCommandSupport {

    @Argument(index = 0, name = "group", description = "The cluster group name", required = true, multiValued = false)
    String groupName;

    public Object doExecute() {
        // check if the group exists
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            Set<ObrBundleInfo> bundles = clusterManager.getSet(Constants.BUNDLES_DISTRIBUTED_SET_NAME + Configurations.SEPARATOR + groupName);
            int maxPName = 4;
            int maxSName = 13;
            int maxVersion = 7;
            for (ObrBundleInfo bundle : bundles) {
                maxPName = Math.max(maxPName, emptyIfNull(bundle.getPresentationName()).length());
                maxSName = Math.max(maxSName, emptyIfNull(bundle.getSymbolicName()).length());
                maxVersion = Math.max(maxVersion, emptyIfNull(bundle.getVersion()).length());
            }
            String formatHeader = "  %-" + maxPName + "s  %-" + maxSName + "s   %-" + maxVersion + "s";
            String formatLine = "[%-" + maxPName + "s] [%-" + maxSName + "s] [%-" + maxVersion + "s]";
            System.out.println(String.format(formatHeader, "NAME", "SYMBOLIC NAME", "VERSION"));
            for (ObrBundleInfo bundle : bundles) {
                System.out.println(String.format(formatLine, emptyIfNull(bundle.getPresentationName()), emptyIfNull(bundle.getSymbolicName()), emptyIfNull(bundle.getVersion())));
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

    private String emptyIfNull(Object st) {
        return st == null ? "" : st.toString();
    }

}
