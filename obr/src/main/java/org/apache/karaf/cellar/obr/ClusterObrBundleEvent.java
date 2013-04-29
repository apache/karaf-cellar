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
package org.apache.karaf.cellar.obr;

import org.apache.karaf.cellar.core.event.Event;

/**
 * Cluster OBR bundle event.
 */
public class ClusterObrBundleEvent extends Event {

    private String bundleId;
    private int type;

    public ClusterObrBundleEvent(String bundleId, int type) {
        super(bundleId);
        this.bundleId = bundleId;
        this.type = type;
    }

    public String getBundleId() {
        return this.bundleId;
    }

    public int getType() {
        return this.type;
    }

}
