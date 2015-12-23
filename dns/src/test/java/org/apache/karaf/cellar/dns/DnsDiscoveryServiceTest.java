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
package org.apache.karaf.cellar.dns;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;

public class DnsDiscoveryServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsDiscoveryServiceTest.class);

    @Ignore
    @Test
    public void testDiscovery() {
        DnsDiscoveryService discovery = new DnsDiscoveryService();
        discovery.setDnsService("_xmpp-server._tcp.gmail.com");

        assertFalse(discovery.discoverMembers().isEmpty());
    }
}
