/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.cellar.itests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import javax.inject.Inject;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;

public class CellarTestSupport {

    static final Long DEFAULT_TIMEOUT = 20000L;
    static final Long SERVICE_TIMEOUT = 30000L;
    static final String GROUP_ID = "org.apache.karaf";
    static final String ARTIFACT_ID = "apache-karaf";

    static final String CELLAR_FEATURE_URL = String.format("mvn:org.apache.karaf.cellar/apache-karaf-cellar/%s/xml/features","3.0.0-SNAPSHOT");

    @Inject
    protected BundleContext bundleContext;

    /**
     * @param probe
     * @return
     */
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");

        return probe;
    }

    /**
     * This method configures Hazelcast TcpIp discovery for a given number of memebers.
     * This configuration is required, when working with karaf instances.
     * @param members
     */
    protected void configureLocalDiscovery (int members) {
        StringBuilder membersBuilder = new StringBuilder();
        membersBuilder.append("config:propset tcpIpMembers ");
        membersBuilder.append("localhost:5701");
        for (int i = 1; i < members; i++) {
            membersBuilder.append(",").append("localhost:").append(String.valueOf(5701 + i));
        }

        String editCmd = "config:edit org.apache.karaf.cellar.instance";
        String propsetCmd = membersBuilder.toString();
        String updateCmd = "config:update";

        executeCommands(editCmd,propsetCmd,updateCmd);
    }

    /**
     * Installs the Cellar feature
     */
    protected void installCellar() {
        System.err.println(executeCommand("features:addurl " + CELLAR_FEATURE_URL));
        System.err.println(executeCommand("features:listurl"));
        System.err.println(executeCommand("features:list"));
        executeCommand("features:install cellar");
    }

    protected void unInstallCellar() {
        executeCommand("features:uninstall cellar");
    }

    /**
     * Creates a child instance that runs cellar.
     */
    protected void createCellarChild(String name) {
        System.err.println(executeCommand("admin:create --featureURL " + CELLAR_FEATURE_URL + " --feature cellar -r " +
                getFreePort(1100)+ " -rs " + getFreePort(44445) + " -s " + getFreePort(8102) + " " + name));
        System.err.println(executeCommand("admin:start " + name));
    }

    /**
     * Destorys the child node.
     */
    protected void destroyCellarChild(String name) {
        System.err.println(executeCommand("admin:stop " + name));
        System.err.println(executeCommand("admin:destroy " + name));
    }

    /**
     * Create an {@link org.ops4j.pax.exam.Option} for using a .
     *
     * @return
     */
    protected Option cellarDistributionConfiguration() {
        return karafDistributionConfiguration().frameworkUrl(
                maven().groupId(GROUP_ID).artifactId(ARTIFACT_ID).versionAsInProject().type("tar.gz"))
                .karafVersion(MavenUtils.getArtifactVersion(GROUP_ID, ARTIFACT_ID)).name("Apache Karaf").unpackDirectory(new File("target/paxexam/"));
    }

    /**
     * Executes a shell command and returns output as a String.
     *
     * @param command
     * @return
     */
    protected String executeCommand(String command) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
        try {
            System.err.println(command);
            commandSession.execute(command);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return byteArrayOutputStream.toString();
    }

    /**
     * Executes multiple commands inside a Single Session.
     * @param commands
     * @return
     */
    protected String executeCommands(String ...commands) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
        CommandSession commandSession = commandProcessor.createSession(System.in, printStream, printStream);
        try {
            for (String cmd : commands) {
                System.err.println(cmd);
                commandSession.execute(cmd);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return byteArrayOutputStream.toString();
    }

    protected Bundle getInstalledBundle(String symbolicName) {
        for (Bundle b : bundleContext.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        for (Bundle b : bundleContext.getBundles()) {
            System.err.println("Bundle: " + b.getSymbolicName());
        }
        throw new RuntimeException("Bundle " + symbolicName + " does not exist");
    }

    /*
    * Explode the dictionary into a ,-delimited list of key=value pairs
    */
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuffer result = new StringBuffer();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    protected <T> T getOsgiService(Class<T> type, long timeout) {
        return getOsgiService(type, null, timeout);
    }

    protected <T> T getOsgiService(Class<T> type) {
        return getOsgiService(type, null, SERVICE_TIMEOUT);
    }

    protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
        ServiceTracker tracker = null;
        try {
            String flt;
            if (filter != null) {
                if (filter.startsWith("(")) {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                } else {
                    flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                }
            } else {
                flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
            }
            Filter osgiFilter = FrameworkUtil.createFilter(flt);
            tracker = new ServiceTracker(bundleContext, osgiFilter, null);
            tracker.open(true);
            // Note that the tracker is not closed to keep the reference
            // This is buggy, as the service reference may change i think
            Object svc = type.cast(tracker.waitForService(timeout));
            if (svc == null) {
                Dictionary dic = bundleContext.getBundle().getHeaders();
                System.err.println("Test bundle headers: " + explode(dic));

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                    System.err.println("ServiceReference: " + ref);
                }

                for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                    System.err.println("Filtered ServiceReference: " + ref);
                }

                throw new RuntimeException("Gave up waiting for service " + flt);
            }
            return type.cast(svc);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid filter", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds a free port starting from the give port numner.
     * @return
     */
    protected int getFreePort(int port) {
        while (!isPortAvailable(port)) {
            port++;
        }
        return port;
    }

    /**
     * Returns true if port is available for use.
     * @param port
     * @return
     */
    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }


    /*
     * Provides an iterable collection of references, even if the original array is null
     */
    private static Collection<ServiceReference> asCollection(ServiceReference[] references) {
        return references != null ? Arrays.asList(references) : Collections.<ServiceReference>emptyList();
    }


}
