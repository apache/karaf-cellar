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
package org.apache.karaf.cellar.etcd;

import com.google.gson.Gson;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.gson.GsonDecoder;
import feign.slf4j.Slf4jLogger;
import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.apache.karaf.cellar.etcd.internal.CellarNode;
import org.apache.karaf.cellar.etcd.internal.EtcdKeyNode;
import org.apache.karaf.cellar.etcd.internal.EtcdKeyResponse;
import org.apache.karaf.cellar.etcd.internal.EtcdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Discovery service that uses the CoreOS's Etcd to discover Cellar nodes.
 */
public class EtcdDiscoveryService implements DiscoveryService  {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdDiscoveryService.class);
    private static final Gson GSON = new Gson();

    private String etcdServiceName;
    private String etcdEnpoint;
    private String etcdUsername;
    private String etcdPassword;

    private EtcdService etcdService;

    public EtcdDiscoveryService() {
        this.etcdServiceName = null;
        this.etcdEnpoint = null;
        this.etcdUsername = null;
        this.etcdPassword = null;

        this.etcdService = null;
    }

    @Override
    public Set<String> discoverMembers() {
        LOGGER.debug("CELLAR Etcd: query services at [{}] with name [{}]", etcdEnpoint, etcdServiceName);

        Set<String> members = new HashSet<String>();
        StringBuilder builder = new StringBuilder();
        try {
            if(this.etcdService != null) {
                EtcdKeyResponse response = this.etcdService.get(this.etcdServiceName);
                EtcdKeyNode rootNode = response.node();

                if(rootNode != null && rootNode.hasNodes()) {
                    EtcdKeyNode[] nodes = rootNode.nodes();
                    for(int i = 0; i < nodes.length; i++) {
                        if(nodes[i].hasValue()) {
                            CellarNode cellarNode = GSON.fromJson(nodes[i].value(), CellarNode.class);

                            builder.setLength(0);
                            builder.append(cellarNode.getHost());
                            builder.append(":");
                            builder.append(cellarNode.getPort());

                            members.add(builder.toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("CELLAR Etcd: can't get service", e);
        }

        return members;
    }

    @Override
    public void signIn() {
        final Feign.Builder builder = Feign.builder()
            .logger(new Slf4jLogger())
            .decoder(new GsonDecoder());

        if(this.etcdUsername != null && this.etcdPassword != null) {
            builder.requestInterceptor(
                new BasicAuthRequestInterceptor(this.etcdUsername, this.etcdPassword));
        }

        this.etcdService = builder.target(EtcdService.class, this.etcdEnpoint);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void signOut() {
    }

    EtcdDiscoveryService setEtcdServiceName(String etcdServiceName) {
        this.etcdServiceName = etcdServiceName;
        return this;
    }

    EtcdDiscoveryService setEtcdEndpoint(String etcdEnpoint) {
        this.etcdEnpoint = etcdEnpoint;
        return this;
    }

    EtcdDiscoveryService setEtcdUsername(String etcdUsername) {
        this.etcdUsername = etcdUsername;
        return this;
    }

    EtcdDiscoveryService setEtcdPassword(String etcdPassword) {
        this.etcdPassword = etcdPassword;
        return this;
    }
}
