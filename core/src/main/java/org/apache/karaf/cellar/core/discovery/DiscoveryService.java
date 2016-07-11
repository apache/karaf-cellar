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
package org.apache.karaf.cellar.core.discovery;

import java.util.Set;

public interface DiscoveryService {

    /**
     * Sign In to the {@link DiscoveryService}.
     */
    void signIn();

    /**
     * Refresh to the {@link DiscoveryService}.
     */
    void refresh();

    /**
     * Sign Out of the {@link DiscoveryService}.
     */
    void signOut();

    /**
     * Returns a {@link java.util.Set} of peers that where discovered.
     *
     * @return
     */
    Set<String> discoverMembers();

}
