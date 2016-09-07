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

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.control.BasicSwitch;
import org.apache.karaf.cellar.core.control.Switch;
import org.apache.karaf.cellar.core.control.SwitchStatus;
import org.apache.karaf.cellar.core.event.EventHandler;
import org.apache.karaf.cellar.core.event.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Handler for cluster OBR bundle event.
 */
public class ObrBundleEventHandler extends ObrSupport implements EventHandler<ClusterObrBundleEvent> {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(ObrBundleEventHandler.class);

    protected static final char VERSION_DELIM = ',';

    public static final String SWITCH_ID = "org.apache.karaf.cellar.event.obr.bundles.handler";

    private final Switch eventSwitch = new BasicSwitch(SWITCH_ID);

    @Override
    public void init(BundleContext bundleContext) {
        super.init(bundleContext);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    protected String[] getTarget(String bundle) {
        String[] target;
        int idx = bundle.indexOf(VERSION_DELIM);
        if (idx > 0) {
            target = new String[]{ bundle.substring(0, idx), bundle.substring(idx+1) };
        } else {
            target = new String[]{ bundle, null };
        }
        return target;
    }

    public Resource selectNewestVersion(Resource[] resources) {
        int idx = -1;
        Version v = null;
        for (int i = 0; (resources != null) && (i < resources.length); i++) {
            if (i == 0) {
                idx = 0;
                v = resources[i].getVersion();
            } else {
                Version vtmp = resources[i].getVersion();
                if (vtmp.compareTo(v) > 0) {
                    idx = i;
                    v = vtmp;
                }
            }
        }
        return (idx < 0) ? null : resources[idx];
    }

    protected Resource[] searchRepository(String targetId, String targetVersion) throws InvalidSyntaxException {
        try {
            Bundle bundle = getBundleContext().getBundle(Long.parseLong(targetId));
            targetId = bundle.getSymbolicName();
        } catch (NumberFormatException e) {
            // it was not a number, so ignore.
        }

        // the target ID may be a bundle name or a bundle symbolic name,
        // so create
        StringBuffer sb = new StringBuffer("(|(presentationname=");
        sb.append(targetId);
        sb.append(")(symbolicname=");
        sb.append(targetId);
        sb.append("))");
        if (targetVersion != null) {
            sb.insert(0, "&(");
            sb.append("(version=");
            sb.append(targetVersion);
            sb.append("))");
        }
        return obrService.discoverResources(sb.toString());
    }

    /**
     * Handle a received cluster OBR bundle event.
     *
     * @param event the received cluster OBR bundle event.
     */
    @Override
    public void handle(ClusterObrBundleEvent event) {

        // check if the handler is ON
        if (this.getSwitch().getStatus().equals(SwitchStatus.OFF)) {
            LOGGER.debug("CELLAR OBR: {} switch is OFF", SWITCH_ID);
            return;
        }

        if (groupManager == null) {
        	//in rare cases for example right after installation this happens!
        	LOGGER.error("CELLAR OBR: retrieved cluster event {} while groupManager is not available yet!", event);
        	return;
        }

        // check if the group is local
        if (!groupManager.isLocalGroup(event.getSourceGroup().getName())) {
            LOGGER.debug("CELLAR OBR: node is not part of the event cluster group {}", event.getSourceGroup().getName());
            return;
        }

        // check if it's not a "local" event
        if (event.getLocal() != null && event.getLocal().getId().equalsIgnoreCase(clusterManager.getNode().getId())) {
            LOGGER.trace("CELLAR OBR: cluster event is local (coming from local synchronizer or listener)");
            return;
        }

        String bundleId = event.getBundleId();
        boolean deployOptional = event.getDeployOptional();
        boolean start = event.getStart();
        try {
            if (isAllowed(event.getSourceGroup(), Constants.BUNDLES_CONFIG_CATEGORY, bundleId, EventType.INBOUND)) {
                Resolver resolver = obrService.resolver();
                String[] target = getTarget(bundleId);
                Resource resource = selectNewestVersion(searchRepository(target[0], target[1]));
                if (resource == null) {
                    LOGGER.warn("CELLAR OBR: bundle {} unknown", target[0]);
                    return;
                }
                resolver.add(resource);

                if ((resolver.getAddedResources() != null) &&
                        (resolver.getAddedResources().length > 0)) {
                    if (resolver.resolve(deployOptional ? 0 : Resolver.NO_OPTIONAL_RESOURCES)) {
                        resolver.deploy(start ? Resolver.START : 0);
                    }
                } else {
                    Reason[] reqs = resolver.getUnsatisfiedRequirements();
                    if (reqs != null && reqs.length > 0) {
                        LOGGER.warn("CELLAR OBR: unsatisfied requirement(s): ");
                        for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++) {
                            LOGGER.warn("  {}", reqs[reqIdx].getRequirement().getFilter());
                            LOGGER.warn("    {}", reqs[reqIdx].getResource().getPresentationName());
                        }
                    } else LOGGER.warn("CELLAR OBR: could not resolve targets");
                }
            } else LOGGER.trace("CELLAR OBR: bundle {} is marked as BLOCKED INBOUND for cluster group {}", bundleId, event.getSourceGroup().getName());
        } catch (Exception e) {
            LOGGER.error("CELLAR OBR: failed to handle bundle event {}", bundleId, e);
        }
    }

    @Override
    public Class<ClusterObrBundleEvent> getType() {
        return ClusterObrBundleEvent.class;
    }

    @Override
    public Switch getSwitch() {
        // load the switch status from the config
        try {
            Configuration configuration = configurationAdmin.getConfiguration(Configurations.NODE, null);
            if (configuration != null) {
                Boolean status = new Boolean((String) configuration.getProperties().get(Configurations.HANDLER + "." + this.getClass().getName()));
                if (status) {
                    eventSwitch.turnOn();
                } else {
                    eventSwitch.turnOff();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return this.eventSwitch;
    }

}
