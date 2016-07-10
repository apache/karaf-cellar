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
package org.apache.karaf.cellar.http.balancer.management.internal;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.GroupManager;
import org.apache.karaf.cellar.http.balancer.Constants;
import org.apache.karaf.cellar.http.balancer.management.CellarHttpMBean;
import org.apache.karaf.shell.support.table.ShellTable;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CellarHttpMBeanImpl extends StandardMBean implements CellarHttpMBean {

    private GroupManager groupManager;
    private ClusterManager clusterManager;

    public CellarHttpMBeanImpl() throws NotCompliantMBeanException {
        super(CellarHttpMBean.class);
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    @Override
    public Map<String, String> listHttp(String groupName) throws Exception {
        Map<String, String> result = new HashMap<String, String>();
        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            throw new IllegalArgumentException("Cluster group " + groupName + " doesn't exist");
        }

        Map<String, List<String>> clusterServlets = clusterManager.getMap(Constants.BALANCER_MAP + Configurations.SEPARATOR + groupName);


        for (String alias : clusterServlets.keySet()) {
            List<String> locations = clusterServlets.get(alias);
            StringBuilder builder = new StringBuilder();
            for (String location : locations) {
                builder.append(location).append(" ");
            }
            result.put(alias, builder.toString());
        }

        return result;
    }
}
