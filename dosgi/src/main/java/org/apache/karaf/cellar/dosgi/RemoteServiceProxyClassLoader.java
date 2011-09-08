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
package org.apache.karaf.cellar.dosgi;

import org.osgi.framework.Bundle;

/**
 * Class loader for a remote service proxy.
 */
public class RemoteServiceProxyClassLoader extends ClassLoader {

    private Bundle bundle;

    public RemoteServiceProxyClassLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return bundle.loadClass(className);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class clazz = loadClass(className);
        if (resolve) {
            this.resolveClass(clazz);
        }
        return clazz;
    }

}
