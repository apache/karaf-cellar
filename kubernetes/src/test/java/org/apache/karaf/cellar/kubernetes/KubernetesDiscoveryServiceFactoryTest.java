package org.apache.karaf.cellar.kubernetes;

import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_API_VERSION;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_AUTH_BASIC_PASSWORD;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_AUTH_BASIC_USERNAME;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_AUTH_TOKEN;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CA_DATA;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CA_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_DATA;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_ALGO;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_DATA;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_DISABLE_HOSTNAME_VERIFICATION;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_KEYSTORE_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_KEYSTORE_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TLS_VERSIONS;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TRUSTSTORE_FILE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TRUSTSTORE_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_TRUST_CERTIFICATES;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_USER_AGENT;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_WATCH_RECONNECTINTERVAL;
import static org.apache.karaf.cellar.kubernetes.ConfigKey.KUBERNETES_WATCH_RECONNECTLIMIT;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_API_VERSION;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CA_DATA;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CA_FILE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CLIENT_DATA;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CLIENT_FILE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_DISABLE_HOSTNAME_VERIFICATION;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_KEYSTORE_FILE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_MASTER;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_OAUTH_TOKEN;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_TLS_VERSION;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_TRUSTSTORE_FILE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_TRUST_CERTIFICATES;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_USER_AGENT;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL;
import static org.apache.karaf.cellar.kubernetes.KubernetesDiscoveryServiceTest.EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

public class KubernetesDiscoveryServiceFactoryTest {
    private static final String ANY_PID = "anyPid";
    private static final String EXPECTED_POD_LABEL_KEY = "expectedPodLabelKey";
    private static final String EXPECTED_POD_LABEL_VALUE = "expectedPodLabelValue";
    private final ServiceRegistration registration = EasyMock.mock(ServiceRegistration.class);
    private final BundleContext bundleContext = EasyMock.mock(BundleContext.class);
    private final KubernetesDiscoveryServiceFactory serviceFactory = new KubernetesDiscoveryServiceFactory(bundleContext);
    private final Dictionary<String, String> properties = new Hashtable<>();
    private final Capture<Object> serviceCapture = newCapture();
    private final Capture<Dictionary<String, ?>> servicePropertiesCapture = newCapture();
    private Dictionary<String, ?> serviceProperties;
    private KubernetesDiscoveryService registeredService;

    @Before
    public void setup() throws Exception {
        expect(bundleContext.registerService(
                eq(DiscoveryService.class.getName()),
                capture(serviceCapture),
                capture(servicePropertiesCapture))).andReturn(registration);
        replay(bundleContext);
    }

    private void update() throws Exception {
        serviceFactory.updated(ANY_PID, properties);
        this.registeredService = (KubernetesDiscoveryService) serviceCapture.getValue();
        this.serviceProperties = servicePropertiesCapture.getValue();
    }

    @Test
    public void verifyUpdatedOldConfigScheme() throws Exception {
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_HOST, "foo");
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_PORT, "55555");
        update();
        assertEquals("http://foo:55555", registeredService.getKubernetesMaster());
    }

    @Test
    public void verifyUpdatedNewMasterHasPrecedence() throws Exception {
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_HOST, "foo");
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_PORT, "55555");
        properties.put(ConfigKey.KUBERNETES_MASTER.propertyName, EXPECTED_KUBERNETES_MASTER);
        update();
        assertEquals(EXPECTED_KUBERNETES_MASTER, registeredService.getKubernetesMaster());
    }

    @Test
    public void verifyUpdatedDefaultPodKeyAndLabel() throws Exception {
        update();
        assertEquals(KubernetesDiscoveryServiceFactory.DEFAULT_POD_LABEL_KEY, registeredService.getKubernetesPodLabelKey());
        assertEquals(KubernetesDiscoveryServiceFactory.DEFAULT_POD_LABEL_VALUE, registeredService.getKubernetesPodLabelValue());
    }

    @Test
    public void verifyUpdatedPodKeyAndLabel() throws Exception {
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_POD_LABEL_KEY, EXPECTED_POD_LABEL_KEY);
        properties.put(KubernetesDiscoveryServiceFactory.KUBERNETES_POD_LABEL_VALUE, EXPECTED_POD_LABEL_VALUE);
        update();
        assertEquals(EXPECTED_POD_LABEL_KEY, registeredService.getKubernetesPodLabelKey());
        assertEquals(EXPECTED_POD_LABEL_VALUE, registeredService.getKubernetesPodLabelValue());
    }

    @Test
    public void verifyUpdateKubernetesConfig() throws Exception {
        properties.put(KUBERNETES_API_VERSION.propertyName, EXPECTED_KUBERNETES_API_VERSION);
        properties.put(KUBERNETES_TRUST_CERTIFICATES.propertyName, EXPECTED_KUBERNETES_TRUST_CERTIFICATES);
        properties.put(KUBERNETES_DISABLE_HOSTNAME_VERIFICATION.propertyName, EXPECTED_KUBERNETES_DISABLE_HOSTNAME_VERIFICATION);
        properties.put(KUBERNETES_CERTS_CA_FILE.propertyName, EXPECTED_KUBERNETES_CERTS_CA_FILE);
        properties.put(KUBERNETES_CERTS_CA_DATA.propertyName, EXPECTED_KUBERNETES_CERTS_CA_DATA);
        properties.put(KUBERNETES_CERTS_CLIENT_FILE.propertyName, EXPECTED_KUBERNETES_CERTS_CLIENT_FILE);
        properties.put(KUBERNETES_CERTS_CLIENT_DATA.propertyName, EXPECTED_KUBERNETES_CERTS_CLIENT_DATA);
        properties.put(KUBERNETES_CERTS_CLIENT_KEY_FILE.propertyName, EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE);
        properties.put(KUBERNETES_CERTS_CLIENT_KEY_DATA.propertyName, EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA);
        properties.put(KUBERNETES_CERTS_CLIENT_KEY_ALGO.propertyName, EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO);
        properties.put(KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE.propertyName, EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE);
        properties.put(KUBERNETES_AUTH_BASIC_USERNAME.propertyName, EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME);
        properties.put(KUBERNETES_AUTH_BASIC_PASSWORD.propertyName, EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD);
        properties.put(KUBERNETES_AUTH_TOKEN.propertyName, EXPECTED_KUBERNETES_OAUTH_TOKEN);
        properties.put(KUBERNETES_WATCH_RECONNECTINTERVAL.propertyName, EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL);
        properties.put(KUBERNETES_WATCH_RECONNECTLIMIT.propertyName, EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT);
        properties.put(KUBERNETES_USER_AGENT.propertyName, EXPECTED_KUBERNETES_USER_AGENT);
        properties.put(KUBERNETES_TLS_VERSIONS.propertyName, EXPECTED_KUBERNETES_TLS_VERSION);
        properties.put(KUBERNETES_TRUSTSTORE_FILE.propertyName, EXPECTED_KUBERNETES_TRUSTSTORE_FILE);
        properties.put(KUBERNETES_TRUSTSTORE_PASSPHRASE.propertyName, EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE);
        properties.put(KUBERNETES_KEYSTORE_FILE.propertyName, EXPECTED_KUBERNETES_KEYSTORE_FILE);
        properties.put(KUBERNETES_KEYSTORE_PASSPHRASE.propertyName, EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE);
        update();
        assertEquals(EXPECTED_KUBERNETES_API_VERSION, registeredService.getKubernetesApiVersion());
        assertEquals(Boolean.parseBoolean(EXPECTED_KUBERNETES_TRUST_CERTIFICATES), registeredService.isKubernetesTrustCertificates());
        assertEquals(Boolean.parseBoolean(EXPECTED_KUBERNETES_DISABLE_HOSTNAME_VERIFICATION), registeredService.isKubernetesDisableHostnameVerification());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CA_FILE, registeredService.getKubernetesCertsCaFile());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CA_DATA, registeredService.getKubernetesCertsCaData());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_FILE, registeredService.getKubernetesCertsClientFile());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_DATA, registeredService.getKubernetesCertsClientData());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE, registeredService.getKubernetesCertsClientKeyFile());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA, registeredService.getKubernetesCertsClientKeyData());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO, registeredService.getKubernetesCertsClientKeyAlgo());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE, registeredService.getKubernetesCertsClientKeyPassphrase());
        assertEquals(EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME, registeredService.getKubernetesAuthBasicUsername());
        assertEquals(EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD, registeredService.getKubernetesAuthBasicPassword());
        assertEquals(EXPECTED_KUBERNETES_OAUTH_TOKEN, registeredService.getKubernetesOauthToken());
        assertEquals(Integer.parseInt(EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL), registeredService.getKubernetesWatchReconnectInterval());
        assertEquals(Integer.parseInt(EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT), registeredService.getKubernetesWatchReconnectLimit());
        assertEquals(EXPECTED_KUBERNETES_USER_AGENT, registeredService.getKubernetesUserAgent());
        assertEquals(EXPECTED_KUBERNETES_TLS_VERSION, registeredService.getKubernetesTlsVersion());
        assertEquals(EXPECTED_KUBERNETES_TRUSTSTORE_FILE, registeredService.getKubernetesTruststoreFile());
        assertEquals(EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE, registeredService.getKubernetesTruststorePassphrase());
        assertEquals(EXPECTED_KUBERNETES_KEYSTORE_FILE, registeredService.getKubernetesKeystoreFile());
        assertEquals(EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE, registeredService.getKubernetesKeystorePassphrase());
    }
}
