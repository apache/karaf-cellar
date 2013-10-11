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
package org.apache.karaf.cellar.config;

import org.apache.karaf.cellar.core.event.Event;

/**
 * Cluster configuration event.
 */
public class ClusterConfigurationEvent extends Event {

	private Integer type;

    public ClusterConfigurationEvent(String id) {
        super(id);
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

	@Override
	public String toString() {
		return "ClusterConfigurationEvent [type=" + type + ", id=" + id
				+ ", sourceNode=" + sourceNode + ", sourceGroup=" + sourceGroup
				+ ", destination=" + destination + ", force=" + force
				+ ", postPublish=" + postPublish + "]";
	}

}
