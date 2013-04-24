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

import org.apache.karaf.cellar.core.event.Event;
import org.apache.karaf.features.FeatureEvent.EventType;

/**
 * Cluster features event.
 */
public class ClusterFeaturesEvent extends Event {

    private static final String separator = "/";

    private String name;
    private String version;
    private Boolean noClean;
    private Boolean noRefresh;
    private EventType type;

    public ClusterFeaturesEvent(String name, String version, EventType type) {
        super(name + separator + version);
        this.name = name;
        this.version = version;
        this.noClean = false;
        this.noRefresh = false;
        this.type = type;
    }

    public ClusterFeaturesEvent(String name, String version, Boolean noClean, Boolean noRefresh, EventType type) {
        super(name + separator + version);
        this.name = name;
        this.version = version;
        this.noClean = noClean;
        this.noRefresh = noRefresh;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Boolean getNoClean() {
        return noClean;
    }

    public Boolean getNoRefresh() {
        return noRefresh;
    }

    public EventType getType() {
        return type;
    }

}
