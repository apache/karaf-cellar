package org.apache.karaf.cellar.kubernetes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigKeyTest {
    private static final String EXPECTED_PROP_VALUE = "properties value";
    private static final String EXPECTED_SYSPROP_VALUE = "system properties value";
    private static final String EXPECTED_ENVVAR_VALUE = "envvar value";
    private Properties originalProperties = new Properties();

    public static class GetValueTestRun {

        public static void main(String[] args) {
            if (args.length == 0) {
                System.exit(1);
            }

            String constantName = args[0];
            String expectedValue = args[1];
            ConfigKey key = ConfigKey.valueOf(constantName);

            if (!expectedValue.equals(key.getValue(new Hashtable<>()))) {
                System.exit(2);
            }
        }

    }

    @Before
    public void setup() {
        originalProperties.putAll(System.getProperties());
    }

    @After
    public void tearDown() {
        System.setProperties(originalProperties);
    }

    private void verifyEnvvar(ConfigKey key) throws Exception {
        startTestProc(key, EXPECTED_ENVVAR_VALUE, null);
    }

    private void verifySysProp(ConfigKey key) throws Exception {
        startTestProc(key, EXPECTED_ENVVAR_VALUE, EXPECTED_SYSPROP_VALUE);
    }

    private void startTestProc(ConfigKey key, String expectedEnvvarValue, String expectedSyspropValue) throws Exception {
        String expectedValue = expectedSyspropValue == null ? expectedEnvvarValue : expectedSyspropValue;

        String javaHome = System.getProperty("java.home");
        String extension = System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : "";
        String javaBin = String.format("%s%sbin%sjava%s", javaHome, File.separator, File.separator, extension);
        String classpath = System.getProperty("java.class.path");
        List<String> command = new LinkedList<>();
        command.add(javaBin);

        if (expectedSyspropValue != null) {
            command.add(String.format("-D%s=%s", key.propertyName, expectedSyspropValue));
        }

        command.add("-cp");
        command.add(classpath);
        command.add(GetValueTestRun.class.getName());
        command.add(key.name());
        command.add(expectedValue);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.inheritIO();
        Map<String, String> env = builder.environment();
        env.put(key.name(), expectedEnvvarValue);
        Process proc = builder.start();
        int rc = proc.waitFor();

        if (rc == 1) {
            Assert.fail("An unexpected exception occurred!");
        }
        if (rc == 2) {
            Assert.fail(String.format("Actual value differs from %s", expectedValue));
        }
    }

    @Test
    public void valueFromEnvvar() throws Exception {
        for (ConfigKey key : ConfigKey.values()) {
            verifyEnvvar(key);
        }
    }

    @Test
    public void valueFromSystemProps() throws Exception {
        for (ConfigKey key : ConfigKey.values()) {
            verifySysProp(key);
        }
    }

    @Test
    public void valueFromProperties() {
        for (ConfigKey key : ConfigKey.values()) {
            System.setProperty(key.propertyName, EXPECTED_SYSPROP_VALUE);
            Dictionary<String, String> properties = new Hashtable<>();
            properties.put(key.propertyName, EXPECTED_PROP_VALUE);
            assertEquals(EXPECTED_PROP_VALUE, key.getValue(properties));
        }
    }

    @Test
    public void verifyConfigKeyUniquelyAssignedToEnvvar() {
        Set<String> configKeys = new HashSet<>();
        for (ConfigKey key : ConfigKey.values()) {
            assertTrue(configKeys.add(key.propertyName));
        }
    }
}
