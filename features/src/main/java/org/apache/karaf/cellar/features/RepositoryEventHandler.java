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

package org.apache.karaf.cellar.features;

import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.features.RepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * @author iocanel
 */
public class RepositoryEventHandler extends FeaturesSupport implements EventHandler<RemoteRepositoryEvent> {

    private static Logger logger = LoggerFactory.getLogger(RepositoryEventHandler.class);
    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.repository.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    public void handle(RemoteRepositoryEvent event) {
        String uri = event.getId();
        RepositoryEvent.EventType type = event.getType();
        try {
            if (RepositoryEvent.EventType.RepositoryAdded.equals(type)) {
                logger.debug("Adding repository url {}", uri);
                featuresService.addRepository(new URI(uri));
            } else {
                logger.debug("Removing repository url {}", uri);
                featuresService.removeRepository(new URI(uri));
            }
        } catch (Exception e) {
            logger.error("Failed to add/remove repository {}", uri);
        }
    }

    public Class<RemoteRepositoryEvent> getType() {
        return RemoteRepositoryEvent.class;
    }

    public Switch getSwitch() {
        return eventSwitch;
    }
}
