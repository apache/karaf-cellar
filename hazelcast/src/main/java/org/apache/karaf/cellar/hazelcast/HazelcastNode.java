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
package org.apache.karaf.cellar.hazelcast;

import com.hazelcast.core.Member;
import org.apache.karaf.cellar.core.Node;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Cluster node powered by Hazelcast.
 */
public class HazelcastNode implements Node {

    private String id;
    private String host;
    private int port;
    private transient String alias;

    public HazelcastNode(Member member) {
        InetSocketAddress address = member.getSocketAddress();
        this.host = getHostString(address);
        this.port = address.getPort();
        StringBuilder builder = new StringBuilder();
        this.id = builder.append(host).append(":").append(port).toString();
        this.alias = member.getStringAttribute("alias");
    }

    static String getHostString(InetSocketAddress socketAddress) {
        InetAddress address = socketAddress.getAddress();
        return (address != null && address.toString().startsWith("/")) ? address.getHostAddress() : socketAddress.getHostName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HazelcastNode that = (HazelcastNode) o;

        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

	@Override
	public String toString() {
		return "HazelcastNode [id=" + id + ", host=" + host + ", port=" + port
				+ "]";
	}
    
}
