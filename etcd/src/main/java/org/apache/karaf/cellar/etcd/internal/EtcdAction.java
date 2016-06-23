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


public enum EtcdAction {
    set,
    get,
    create,
    update,
    delete,
    expire,
    compareAndSwap,
    compareAndDelete;

    private final static EtcdAction[] VALUES = values();

    public EtcdAction fromActionName(String action) {
        for(int i = 0; i < VALUES.length; i++) {
            if(VALUES[i].name().equalsIgnoreCase(action)) {
                return VALUES[i];
            }
        }

        throw new IllegalArgumentException(
            "Unable to convert " + action + " to EtcdAction");
    }
}
