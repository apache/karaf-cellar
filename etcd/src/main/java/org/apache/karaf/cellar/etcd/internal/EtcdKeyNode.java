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
package org.apache.karaf.cellar.etcd.internal;


public class EtcdKeyNode {
    private String key;
    private String value;
    private boolean dir;
    private long createdIndex;
    private long modifiedIndex;
    private long ttl;
    private String expiration;
    private EtcdKeyNode[] nodes;

    public EtcdKeyNode() {
        this.dir = false;
        this.key = null;
        this.value = null;
        this.createdIndex = -1;
        this.modifiedIndex = -1;
        this.ttl = -1;
        this.nodes = null;
        this.expiration = null;
    }

    public boolean dir() {
        return dir;
    }

    public String key() {
        return key;
    }

    public boolean hasKey() {
        return key != null && !key.isEmpty();
    }

    public long createdIndex() {
        return createdIndex;
    }

    public long modifiedIndex() {
        return modifiedIndex;
    }

    public long ttl() {
        return ttl;
    }

    public String expiration() {
        return expiration;
    }

    public boolean hasExpiration() {
        return expiration != null && !expiration.isEmpty();
    }


    public String value() {
        return value;
    }

    public boolean hasValue() {
        return value != null && !value.isEmpty();
    }


    public boolean hasNodes() {
        return nodes != null && nodes.length > 0;
    }

    public EtcdKeyNode[] nodes() {
        return nodes;
    }
}
