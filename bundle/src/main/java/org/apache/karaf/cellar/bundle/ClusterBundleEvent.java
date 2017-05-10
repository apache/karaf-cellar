/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.cellar.bundle;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.event.Event;

/**
 * Cluster bundle event.
 */
public class ClusterBundleEvent extends Event {

    private String symbolicName;
    private String version;
    private String location;
    private Integer startLevel;
    private int type;
    private Node local;

    public ClusterBundleEvent(String symbolicName, String version, String location, Integer startLevel, int type) {
        super(symbolicName + "/" + version);
        this.symbolicName = symbolicName;
        this.version = version;
        this.location = location;
        this.startLevel = startLevel;
        this.type = type;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(Integer startLevel) {
        this.startLevel = startLevel;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Node getLocal() {
        return local;
    }

    public void setLocal(Node local) {
        this.local = local;
    }
}
