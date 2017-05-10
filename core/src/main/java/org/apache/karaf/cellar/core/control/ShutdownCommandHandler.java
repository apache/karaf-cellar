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
package org.apache.karaf.cellar.core.control;

import org.apache.karaf.cellar.core.command.CommandHandler;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Cluster shutdown command handler.
 */
public class ShutdownCommandHandler extends CommandHandler<ShutdownCommand, ShutdownResult> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.command.shutdown.switch";
    private final Switch commandSwitch = new BasicSwitch(SWITCH_ID);

    private BundleContext bundleContext;

    @Override
    public ShutdownResult execute(ShutdownCommand command) {
        try {
            if (command.isHalt()) {
                bundleContext.getBundle(0).stop();
            } else {
                ServiceReference<FeaturesService> ref = bundleContext.getServiceReference(FeaturesService.class);
                FeaturesService featuresService = bundleContext.getService(ref);
                featuresService.uninstallFeature("cellar");
                bundleContext.ungetService(ref);
            }
        } catch (Exception e) {
            // nothing to do
        }
        return new ShutdownResult(command.getId());
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Class<ShutdownCommand> getType() {
        return ShutdownCommand.class;
    }

    @Override
    public Switch getSwitch() {
        return commandSwitch;
    }

}
