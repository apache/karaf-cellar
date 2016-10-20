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
package org.apache.karaf.cellar.kar;

import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import java.net.URI;
import java.util.Map;

/**
 * Handler for cluster KAR event.
 */
public class KarEventHandler extends CellarSupport implements EventHandler<ClusterKarEvent> {

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.kar.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Reference
    private KarService karService;

    @Override
    public void handle(ClusterKarEvent event) {
        if (event.isInstall()) {
            try {
                String karUrl = event.getId();
                karService.install(new URI(karUrl));
            } catch (Exception e) {
                LOGGER.error("CELLAR KAR: can't install {}", event.getId(), e);
                return;
            }
        } else {
            try {
                karService.uninstall(event.getId());
            } catch (Exception e) {
                LOGGER.error("CELLAR KAR: can't uninstall {}", event.getId(), e);
                return;
            }
        }
    }

    @Override
    public Class<ClusterKarEvent> getType() {
        return ClusterKarEvent.class;
    }

    @Override
    public Switch getSwitch() {
        return eventSwitch;
    }

    public KarService getKarService() {
        return karService;
    }

    public void setKarService(KarService karService) {
        this.karService = karService;
    }

}
