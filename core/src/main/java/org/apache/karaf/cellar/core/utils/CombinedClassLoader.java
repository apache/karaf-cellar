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
package org.apache.karaf.cellar.core.utils;

import org.osgi.framework.Bundle;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A class loader which combines multiple bundle class loaders.
 * A bundle can add itself for to this class loader, so that the class loader can load classes from the bundle.
 * It is meant to be used together with the extender pattern in order to extends Cellar class space.
 */
public class CombinedClassLoader extends ClassLoader {

    private final ConcurrentMap<Long, Bundle> bundles = new ConcurrentHashMap<Long, Bundle>();

    private final ConcurrentMap<String, Bundle> classBundleCache = new ConcurrentHashMap<String, Bundle>();

    public void init() {
        bundles.clear();
        classBundleCache.clear();
    }

    public void destroy() {
        bundles.clear();
        classBundleCache.clear();
    }

    public void addBundle(Bundle bundle) {
        bundles.put(bundle.getBundleId(), bundle);
    }

    public void removeBundle(Bundle bundle) {
        bundles.remove(bundle.getBundleId());
        // Remove all bundle classes from class name to bundle cache
        for (Map.Entry<String, Bundle> entry : classBundleCache.entrySet()) {
            if (entry.getValue().getBundleId() == bundle.getBundleId()) {
                classBundleCache.remove(entry.getKey());
            }
        }
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        // Trying to find class using class name to bundle cache first
        Bundle bundle = classBundleCache.get(name);
        if (bundle != null) {
            Class<?> clazz = loadClassFromBundle(name, bundle);
            if (clazz != null) {
                return clazz;
            }
            // Remove stale cache entry
            classBundleCache.remove(name);
        }
        // Class not found using class name to bundle cache, check all registered bundles
        for (Map.Entry<Long, Bundle> entry : bundles.entrySet()) {
            bundle = entry.getValue();
            Class<?> clazz = loadClassFromBundle(name, bundle);
            if (clazz != null) {
                classBundleCache.put(name, bundle);
                return clazz;
            }
        }
        throw new ClassNotFoundException(name);
    }

    private Class<?> loadClassFromBundle(String name, Bundle bundle) {
        try {
            if (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING) {
                return bundle.loadClass(name);
            }
        } catch (ClassNotFoundException cnfe) {
        }
        return null;
    }

    @Override
    public URL getResource(String name) {
        for (Map.Entry<Long, Bundle> entry : bundles.entrySet()) {
            Bundle bundle = entry.getValue();
            if (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING) {
                URL url = bundle.getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }
        return null;
    }

}