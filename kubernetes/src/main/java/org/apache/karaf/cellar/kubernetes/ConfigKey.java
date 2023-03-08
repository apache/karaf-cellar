package org.apache.karaf.cellar.kubernetes;

import io.fabric8.kubernetes.client.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * Constant names must be the same as the envvar names specified on
 * <a href="https://github.com/fabric8io/kubernetes-client">GitHub</a>
 */
public enum ConfigKey {
    KUBERNETES_ENABLE_AUTOCONFIGURE("kubernetes.autoConfig"),
    KUBERNETES_MASTER(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY),
    KUBERNETES_API_VERSION(Config.KUBERNETES_API_VERSION_SYSTEM_PROPERTY),
    KUBERNETES_NAMESPACE(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY),
    KUBERNETES_TRUST_CERTIFICATES(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY),
    KUBERNETES_DISABLE_HOSTNAME_VERIFICATION(Config.KUBERNETES_DISABLE_HOSTNAME_VERIFICATION_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CA_FILE(Config.KUBERNETES_CA_CERTIFICATE_FILE_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CA_DATA(Config.KUBERNETES_CA_CERTIFICATE_DATA_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CLIENT_FILE(Config.KUBERNETES_CLIENT_CERTIFICATE_FILE_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CLIENT_DATA(Config.KUBERNETES_CLIENT_CERTIFICATE_DATA_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CLIENT_KEY_FILE(Config.KUBERNETES_CLIENT_KEY_FILE_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CLIENT_KEY_DATA(Config.KUBERNETES_CLIENT_KEY_DATA_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CLIENT_KEY_ALGO(Config.KUBERNETES_CLIENT_KEY_ALGO_SYSTEM_PROPERTY),
    KUBERNETES_CERTS_CLIENT_KEY_PASSPHRASE(Config.KUBERNETES_CLIENT_KEY_PASSPHRASE_SYSTEM_PROPERTY),
    KUBERNETES_AUTH_BASIC_USERNAME(Config.KUBERNETES_AUTH_BASIC_USERNAME_SYSTEM_PROPERTY),
    KUBERNETES_AUTH_BASIC_PASSWORD(Config.KUBERNETES_AUTH_BASIC_PASSWORD_SYSTEM_PROPERTY),
    KUBERNETES_AUTH_TOKEN(Config.KUBERNETES_OAUTH_TOKEN_SYSTEM_PROPERTY),
    KUBERNETES_WATCH_RECONNECTINTERVAL(Config.KUBERNETES_WATCH_RECONNECT_INTERVAL_SYSTEM_PROPERTY),
    KUBERNETES_WATCH_RECONNECTLIMIT(Config.KUBERNETES_WATCH_RECONNECT_LIMIT_SYSTEM_PROPERTY),
    KUBERNETES_USER_AGENT(Config.KUBERNETES_USER_AGENT),
    KUBERNETES_TLS_VERSIONS(Config.KUBERNETES_TLS_VERSIONS),
    KUBERNETES_TRUSTSTORE_FILE(Config.KUBERNETES_TRUSTSTORE_FILE_PROPERTY),
    KUBERNETES_TRUSTSTORE_PASSPHRASE(Config.KUBERNETES_TRUSTSTORE_PASSPHRASE_PROPERTY),
    KUBERNETES_KEYSTORE_FILE(Config.KUBERNETES_KEYSTORE_FILE_PROPERTY),
    KUBERNETES_KEYSTORE_PASSPHRASE(Config.KUBERNETES_KEYSTORE_PASSPHRASE_PROPERTY);
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigKey.class);
    String propertyName;

    ConfigKey(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getValue(Dictionary properties) {
        // Highest priority
        String value = (String)properties.get(propertyName);
        LOGGER.debug("Properties : {}", value);

        if (value == null) {
            // Second priority
            value = System.getProperty(propertyName);
            LOGGER.debug("System properties : {}", value);

            if (value == null) {
                value = System.getenv(name());
                LOGGER.debug("Environment variables : {}", value);
            }
        }

        return value;
    }
}
