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
package org.apache.karaf.cellar.obr;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.karaf.cellar.core.CellarSupport;
import org.osgi.framework.BundleContext;

/**
 * Generic OBR support.
 */
public class ObrSupport extends CellarSupport {

    protected BundleContext bundleContext;
    protected RepositoryAdmin obrService;

    public void init(BundleContext bundleContext) {
        // nothing to do
    }

    public void destroy() {
        // nothing to do
    }

    public RepositoryAdmin getObrService() {
        return this.obrService;
    }

    public void setObrService(RepositoryAdmin obrService) {
        this.obrService = obrService;
    }

    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
