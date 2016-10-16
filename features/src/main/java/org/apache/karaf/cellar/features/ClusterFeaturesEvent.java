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
package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.features.FeatureEvent.EventType;

/**
 * Cluster features event.
 */
public class ClusterFeaturesEvent extends Event {

    private static final String separator = "/";

    private String name;
    private String version;
    private Boolean noRefresh;
    private Boolean noStart;
    private Boolean noManage;
    private Boolean upgrade;
    private EventType type;
    private Node local;

    public ClusterFeaturesEvent(String name, String version, EventType type) {
        super(name + separator + version);
        this.name = name;
        this.version = version;
        this.noRefresh = false;
        this.noStart = false;
        this.noManage = false;
        this.upgrade = false;
        this.type = type;
    }

    public ClusterFeaturesEvent(String name, String version, Boolean noRefresh, Boolean noStart, Boolean noManage, Boolean upgrade, EventType type) {
        super(name + separator + version);
        this.name = name;
        this.version = version;
        this.noRefresh = noRefresh;
        this.noStart = noStart;
        this.noManage = noManage;
        this.upgrade = upgrade;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Boolean getNoRefresh() {
        return noRefresh;
    }

    public Boolean getNoStart() {
        return noStart;
    }

    public Boolean getNoManage() {
        return noManage;
    }

    public Boolean getUpgrade() {
        return upgrade;
    }

    public EventType getType() {
        return type;
    }

    public Node getLocal() {
        return local;
    }

    public void setLocal(Node local) {
        this.local = local;
    }
}
