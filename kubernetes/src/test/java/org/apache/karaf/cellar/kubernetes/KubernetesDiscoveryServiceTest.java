package org.apache.karaf.cellar.kubernetes;

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import okhttp3.TlsVersion;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KubernetesDiscoveryServiceTest {
    static final String EXPECTED_KUBERNETES_MASTER = "http://master/";
    static final String EXPECTED_KUBERNETES_API_VERSION = "api version";
    static final String EXPECTED_KUBERNETES_TRUST_CERTIFICATES = "true";
    static final String EXPECTED_KUBERNETES_DISABLE_HOSTNAME_VERIFICATION = "true";
    static final String EXPECTED_KUBERNETES_CERTS_CA_FILE = "certs ca file";
    static final String EXPECTED_KUBERNETES_CERTS_CA_DATA = "certs ca data";
    static final String EXPECTED_KUBERNETES_CERTS_CLIENT_FILE = "certs client file";
    static final String EXPECTED_KUBERNETES_CERTS_CLIENT_DATA = "certs client data";
    static final String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE = "certs client key file";
    static final String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA = "certs client key data";
    static final String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO = "certs client key algo";
    static final String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE = "certs client key passphrase";
    static final String EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME = "auth basic username";
    static final String EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD = "auth basic password";
    static final String EXPECTED_KUBERNETES_OAUTH_TOKEN = "oauth token";
    static final String EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL = "10";
    static final String EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT = "20";
    static final String EXPECTED_KUBERNETES_USER_AGENT = "user agent";
    static final String EXPECTED_KUBERNETES_TLS_VERSION = "TLSv1.3";
    static final String EXPECTED_KUBERNETES_TRUSTSTORE_FILE = "truststore file";
    static final String EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE = "truststore passphrase";
    static final String EXPECTED_KUBERNETES_KEYSTORE_FILE = "keystore file";
    static final String EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE = "keystore passphrase";
    static final String EXPECTED_KUBERNETES_POD_LABEL_KEY = "pod label key";
    static final String EXPECTED_KUBERNETES_POD_LABEL_VALUE = "pod label value";
    static final String EXPECTED_POD_ID = "192.168.0.1";
    private KubernetesClient kubernetesClient = mock(KubernetesClient.class);
    private final MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> pods = mock(MixedOperation.class);
    private final PodList podList = new PodList();
    private final KubernetesDiscoveryService service = new KubernetesDiscoveryService();
    private final Pod pod = new Pod();
    private final ObjectMeta metadata = new ObjectMeta();
    private final PodStatus status = new PodStatus();
    private final List<Pod> items = Arrays.asList(pod);
    private final Map<String, String> labels = new HashMap<>();

    @Before
    public void setup() {
        labels.put(EXPECTED_KUBERNETES_POD_LABEL_KEY, EXPECTED_KUBERNETES_POD_LABEL_VALUE);
        service.setKubernetesPodLabelKey(EXPECTED_KUBERNETES_POD_LABEL_KEY);
        service.setKubernetesPodLabelValue(EXPECTED_KUBERNETES_POD_LABEL_VALUE);
        service.setKubernetesClient(kubernetesClient);

        expect(kubernetesClient.pods()).andReturn(pods);
        expect(pods.list()).andReturn(podList);
        podList.setItems(items);
        pod.setMetadata(metadata);
        pod.setStatus(status);
        metadata.setLabels(labels);
        status.setPodIP(EXPECTED_POD_ID);
        replay(kubernetesClient, pods);
    }

    @Test
    public void createConfig() {
        service.setKubernetesMaster(EXPECTED_KUBERNETES_MASTER);
        service.setKubernetesApiVersion(EXPECTED_KUBERNETES_API_VERSION);
        service.setKubernetesTrustCertificates(EXPECTED_KUBERNETES_TRUST_CERTIFICATES);
        service.setKubernetesDisableHostnameVerification(EXPECTED_KUBERNETES_DISABLE_HOSTNAME_VERIFICATION);
        service.setKubernetesCertsCaFile(EXPECTED_KUBERNETES_CERTS_CA_FILE);
        service.setKubernetesCertsCaData(EXPECTED_KUBERNETES_CERTS_CA_DATA);
        service.setKubernetesCertsClientFile(EXPECTED_KUBERNETES_CERTS_CLIENT_FILE);
        service.setKubernetesCertsClientData(EXPECTED_KUBERNETES_CERTS_CLIENT_DATA);
        service.setKubernetesCertsClientKeyFile(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE);
        service.setKubernetesCertsClientKeyData(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA);
        service.setKubernetesCertsClientKeyAlgo(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO);
        service.setKubernetesCertsClientKeyPassphrase(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE);
        service.setKubernetesAuthBasicUsername(EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME);
        service.setKubernetesAuthBasicPassword(EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD);
        service.setKubernetesOauthToken(EXPECTED_KUBERNETES_OAUTH_TOKEN);
        service.setKubernetesWatchReconnectInterval(EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL);
        service.setKubernetesWatchReconnectLimit(EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT);
        service.setKubernetesUserAgent(EXPECTED_KUBERNETES_USER_AGENT);
        service.setKubernetesTlsVersion(EXPECTED_KUBERNETES_TLS_VERSION);
        service.setKubernetesTruststoreFile(EXPECTED_KUBERNETES_TRUSTSTORE_FILE);
        service.setKubernetesTruststorePassphrase(EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE);
        service.setKubernetesKeystoreFile(EXPECTED_KUBERNETES_KEYSTORE_FILE);
        service.setKubernetesKeystorePassphrase(EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE);

        Config config = service.createConfig();
        assertEquals(EXPECTED_KUBERNETES_MASTER, config.getMasterUrl());
        assertEquals(EXPECTED_KUBERNETES_API_VERSION, config.getApiVersion());
        assertTrue(config.isTrustCerts());
        assertTrue(config.isDisableHostnameVerification());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CA_FILE, config.getCaCertFile());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CA_DATA, config.getCaCertData());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_FILE, config.getClientCertFile());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_DATA, config.getClientCertData());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE, config.getClientKeyFile());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA, config.getClientKeyData());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO, config.getClientKeyAlgo());
        assertEquals(EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE, config.getClientKeyPassphrase());
        assertEquals(EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME, config.getUsername());
        assertEquals(EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD, config.getPassword());
        assertEquals(EXPECTED_KUBERNETES_OAUTH_TOKEN, config.getOauthToken());
        assertEquals(Integer.parseInt(EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL), config.getWatchReconnectInterval());
        assertEquals(Integer.parseInt(EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT), config.getWatchReconnectLimit());
        assertEquals(EXPECTED_KUBERNETES_USER_AGENT, config.getUserAgent());

        TlsVersion[] tlsVersions = config.getTlsVersions();
        assertEquals(1, tlsVersions.length);
        assertEquals(TlsVersion.TLS_1_3, tlsVersions[0]);

        assertEquals(EXPECTED_KUBERNETES_TRUSTSTORE_FILE, config.getTrustStoreFile());
        assertEquals(EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE, config.getTrustStorePassphrase());
        assertEquals(EXPECTED_KUBERNETES_KEYSTORE_FILE, config.getKeyStoreFile());
        assertEquals(EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE, config.getKeyStorePassphrase());
    }

    @Test
    public void discoverMembers() {
        Set<String> memberIps = service.discoverMembers();
        assertEquals(1, memberIps.size());
        assertEquals(EXPECTED_POD_ID, memberIps.iterator().next());
    }

    @Test
    public void discoverMembersUnexpectedPodLabelKey() {
        service.setKubernetesPodLabelKey("unexpected");
        assertTrue(service.discoverMembers().isEmpty());
    }

    @Test
    public void discoverMembersUnexpectedPodLabelValue() {
        service.setKubernetesPodLabelValue("unexpected");
        assertTrue(service.discoverMembers().isEmpty());
    }

    @Test
    public void discoverMembersLogException() {
        reset(kubernetesClient);
        expect(kubernetesClient.pods()).andThrow(new RuntimeException("Test exception"));
        replay(kubernetesClient);

        // Should return empty set because exception was caught and logged
        assertTrue(service.discoverMembers().isEmpty());
    }
}
