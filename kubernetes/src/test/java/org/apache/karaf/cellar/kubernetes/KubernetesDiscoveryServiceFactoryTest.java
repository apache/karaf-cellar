package org.apache.karaf.cellar.kubernetes;

import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_MASTER;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class KubernetesDiscoveryServiceFactoryTest {
    private static final String ANY_PID = "anyPid";
    private final ServiceRegistration registration = EasyMock.mock(ServiceRegistration.class);
    private final BundleContext bundleContext = EasyMock.mock(BundleContext.class);
    private final KubernetesDiscoveryServiceFactory serviceFactory = new KubernetesDiscoveryServiceFactory(bundleContext);
    private final Dictionary<String, String> properties = new Hashtable<>();

    @Test
    public void verifyUpdatedOldConfigScheme() throws Exception {
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_HOST, "foo");
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_PORT, "55555");

        Capture<Object> service = newCapture();
        Capture<Dictionary<String, ?>> serviceProperties = newCapture();

        expect(bundleContext.registerService(
                eq(DiscoveryService.class.getName()),
                capture(service),
                capture(serviceProperties))).andReturn(registration);
        replay(bundleContext);
        serviceFactory.updated(ANY_PID, properties);

        KubernetesDiscoveryService registeredService = (KubernetesDiscoveryService)service.getValue();
        assertEquals("http://foo:55555", registeredService.getKubernetesMaster());
    }

    @Test
    public void verifyUpdatedNewMasterHasPrecedence() throws Exception {
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_HOST, "foo");
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_PORT, "55555");
        properties.put(ConfigKey.KUBERNETES_MASTER.propertyName, EXPECTED_KUBERNETES_MASTER);

        Capture<Object> service = newCapture();
        Capture<Dictionary<String, ?>> serviceProperties = newCapture();

        expect(bundleContext.registerService(
                eq(DiscoveryService.class.getName()),
                capture(service),
                capture(serviceProperties))).andReturn(registration);
        replay(bundleContext);
        serviceFactory.updated(ANY_PID, properties);

        KubernetesDiscoveryService registeredService = (KubernetesDiscoveryService)service.getValue();
        assertEquals(EXPECTED_KUBERNETES_MASTER, registeredService.getKubernetesMaster());
    }
}
