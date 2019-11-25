package org.apache.karaf.cellar.kubernetes;

import io.fabric8.kubernetes.client.Config;
import okhttp3.TlsVersion;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KubernetesDiscoveryServiceTest {
    private static String EXPECTED_KUBERNETES_MASTER = "http://master/";
    private static String EXPECTED_KUBERNETES_API_VERSION = "api version";
    private static String EXPECTED_KUBERNETES_TRUST_CERTIFICATES = "true";
    private static String EXPECTED_KUBERNETES_DISABLE_HOSTNAME_VERIFICATION = "true";
    private static String EXPECTED_KUBERNETES_CERTS_CA_FILE = "certs ca file";
    private static String EXPECTED_KUBERNETES_CERTS_CA_DATA = "certs ca data";
    private static String EXPECTED_KUBERNETES_CERTS_CLIENT_FILE = "certs client file";
    private static String EXPECTED_KUBERNETES_CERTS_CLIENT_DATA = "certs client data";
    private static String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_FILE = "certs client key file";
    private static String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_DATA = "certs client key data";
    private static String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_ALGO = "certs client key algo";
    private static String EXPECTED_KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE = "certs client key passphrase";
    private static String EXPECTED_KUBERNETES_AUTH_BASIC_USERNAME = "auth basic username";
    private static String EXPECTED_KUBERNETES_AUTH_BASIC_PASSWORD = "auth basic password";
    private static String EXPECTED_KUBERNETES_OAUTH_TOKEN = "oauth token";
    private static String EXPECTED_KUBERNETES_WATCH_RECONNECT_INTERVAL = "10";
    private static String EXPECTED_KUBERNETES_WATCH_RECONNECT_LIMIT = "20";
    private static String EXPECTED_KUBERNETES_USER_AGENT = "user agent";
    private static String EXPECTED_KUBERNETES_TLS_VERSION = "TLSv1.3";
    private static String EXPECTED_KUBERNETES_TRUSTSTORE_FILE = "truststore file";
    private static String EXPECTED_KUBERNETES_TRUSTSTORE_PASSPHRASE = "truststore passphrase";
    private static String EXPECTED_KUBERNETES_KEYSTORE_FILE = "keystore file";
    private static String EXPECTED_KUBERNETES_KEYSTORE_PASSPHRASE = "keystore passphrase";
    private static String EXPECTED_KUBERNETES_POD_LABEL_KEY = "pod label key";
    private static String EXPECTED_KUBERNETES_POD_LABEL_VALUE = "pod label value";
    private KubernetesDiscoveryService service = new KubernetesDiscoveryService();

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
}
