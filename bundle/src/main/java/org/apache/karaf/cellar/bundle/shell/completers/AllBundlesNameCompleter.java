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
package org.apache.karaf.cellar.bundle.shell.completers;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.List;

/**
 * Completer on all bundle symbolic name.
 */
public class AllBundlesNameCompleter implements Completer {
    
    private BundleContext bundleContext;
    
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (Bundle bundle : bundleContext.getBundles()) {
            delegate.getStrings().add(bundle.getSymbolicName());
        }
        return delegate.complete(buffer, cursor, candidates);
    }
    
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
}
