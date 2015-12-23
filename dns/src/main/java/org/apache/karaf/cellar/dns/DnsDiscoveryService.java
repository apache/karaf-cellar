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

import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.*;

/**
 * Discovery service that uses the DNS SRV Record to discover Cellar nodes.
 */
public class DnsDiscoveryService implements DiscoveryService  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsDiscoveryService.class);

    private final Hashtable<String, String> dnsEnv;
    private String dnsService;

    public DnsDiscoveryService() {
        this.dnsEnv = new Hashtable<String, String>();
        this.dnsEnv.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        this.dnsEnv.put("java.naming.provider.url", "dns:");

        this.dnsService = null;
    }

    @Override
    public Set<String> discoverMembers() {
        LOGGER.debug("CELLAR DNS: query services with name [{}]", dnsService);
        Set<String> members = new HashSet<String>();
        try {
            DirContext ctx = new InitialDirContext(this.dnsEnv);
            Attributes attrs = ctx.getAttributes(this.dnsService, new String[]{"SRV"});
            NamingEnumeration<?> servers = attrs.get("srv").getAll();
            while (servers.hasMore()) {
                String dns = (String)servers.next();
                String[] split = dns.split(" ");

                members.add(split[3] + ":" + split[2]);
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR DNS: can't get service", e);
        }

        return members;
    }

    @Override
    public void signIn() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void signOut() {
    }

    public void setDnsService(String dnsService) {
        this.dnsService = dnsService;
    }
}
