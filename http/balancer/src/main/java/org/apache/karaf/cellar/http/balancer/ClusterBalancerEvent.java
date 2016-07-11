/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.http.balancer;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.Event;

import java.util.List;

public class ClusterBalancerEvent extends Event {

    private String alias;
    private int type;
    private List<String> locations;
    private Node local;

    public static int ADDING = 0;
    public static int REMOVING = 1;

    public ClusterBalancerEvent(String alias, int type, List<String> locations) {
        super(alias);
        this.alias = alias;
        this.type = type;
        this.locations = locations;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }

    public Node getLocal() {
        return local;
    }

    public void setLocal(Node local) {
        this.local = local;
    }
}
