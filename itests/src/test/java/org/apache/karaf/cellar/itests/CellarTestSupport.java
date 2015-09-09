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

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.concurrent.*;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.security.auth.Subject;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.ops4j.pax.exam.*;
import org.ops4j.pax.exam.karaf.options.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class CellarTestSupport {

    static final Long COMMAND_TIMEOUT = 10000L;
    static final Long DEFAULT_TIMEOUT = 20000L;
    static final Long SERVICE_TIMEOUT = 30000L;
    static final String GROUP_ID = "org.apache.karaf";
    static final String ARTIFACT_ID = "apache-karaf";

    static final String INSTANCE_STARTED = "Started";
    static final String INSTANCE_STARTING = "Starting";

    static final String DEBUG_OPTS = " --java-opts \"-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=%s\"";

    ExecutorService executor = Executors.newCachedThreadPool();

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected SessionFactory sessionFactory;


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
     * This method configures Hazelcast TcpIp discovery for a given number of members.
     * This configuration is required, when working with karaf instances.
     *
     * @param members
     */
    protected void configureLocalDiscovery(int members) {
        StringBuilder membersBuilder = new StringBuilder();
        membersBuilder.append("config:propset tcpIpMembers ");
        membersBuilder.append("localhost:5701");
        for (int i = 1; i < members; i++) {
            membersBuilder.append(",").append("localhost:").append(String.valueOf(5701 + i));
        }

        String editCmd = "config:edit org.apache.karaf.cellar.discovery";
        String propsetCmd = membersBuilder.toString();
        String updateCmd = "config:update";

        executeCommand(editCmd);
        executeCommand(propsetCmd);
        executeCommand(updateCmd);
    }

    /**
     * Installs the Cellar feature
     */
    protected void installCellar() {
        System.err.println(executeCommand("feature:repo-add " + System.getProperty("cellar.feature.url")));
        System.err.println(executeCommand("feature:repo-list"));
        System.err.println(executeCommand("feature:list"));
        executeCommand("feature:install cellar");
    }

    protected void unInstallCellar() {
        System.err.println(executeCommand("feature:uninstall cellar"));
    }

    /**
     * Creates a child instance that runs cellar.
     */
    protected void createCellarChild(String name) {
        createCellarChild(name, false, 0);
    }

    protected void createCellarChild(String name, boolean debug, int port) {
        int instances = 0;
        String createCommand = "instance:create --featureURL " + System.getProperty("cellar.feature.url") + " --feature cellar ";
        if (debug && port > 0) {
            createCommand = createCommand + String.format(DEBUG_OPTS, port);
        }
        System.err.println(executeCommand(createCommand + " " + name));
        System.err.println(executeCommand("instance:start " + name));

        //Wait till the node is listed as Starting
        System.err.print("Waiting for " + name + " to start ");
        for (int i = 0; i < 5 && instances == 0; i++) {
            String response = executeCommand("instance:list | grep " + name + " | grep -c " + INSTANCE_STARTED, COMMAND_TIMEOUT, true);
            instances = Integer.parseInt(response.trim());
            System.err.print(".");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                //Ignore
            }
        }

        if (instances > 0) {
            System.err.println(".Started!");
        } else {
            System.err.println(".Timed Out!");
        }

    }

    /**
     * Destroys the child node.
     */
    protected void destroyCellarChild(String name) {
        System.err.println(executeCommand("instance:connect " + name + " feature:uninstall cellar"));
        System.err.println(executeCommand("instance:stop " + name));
    }

    /**
     * Returns the node id of a specific child instance.
     */
    protected String getNodeIdOfChild(String name) {
        String node;
        String nodesList = executeCommand("instance:connect " + name + " cluster:node-list | grep \\\\*", COMMAND_TIMEOUT, true);
        int stop = nodesList.indexOf(']');
        node = nodesList.substring(0, stop);
        int start = node.lastIndexOf('[');
        node = node.substring(start + 1);
        node = node.trim();
        return node;
    }

    protected Option cellarDistributionConfiguration() {
        return karafDistributionConfiguration().frameworkUrl(
                maven().groupId(GROUP_ID).artifactId(ARTIFACT_ID).versionAsInProject().type("tar.gz"))
                .karafVersion(MavenUtils.getArtifactVersion(GROUP_ID, ARTIFACT_ID)).name("Apache Karaf").unpackDirectory(new File("target/paxexam/"));
    }

    @Configuration
    public Option[] config() {
        Option[] options = new Option[]{
                cellarDistributionConfiguration(), keepRuntimeFolder(), logLevel(LogLevelOption.LogLevel.INFO),
                editConfigurationFileExtend("etc/system.properties", "cellar.feature.url", maven().groupId("org.apache.karaf.cellar").artifactId("apache-karaf-cellar").versionAsInProject().classifier("features").type("xml").getURL()),
                editConfigurationFileExtend("etc/config.properties", "org.apache.aries.blueprint.synchronous", "true")
        };
        String debug = System.getProperty("debugMain");
        if (debug != null) {
            int l = options.length;
            options = Arrays.copyOf(options, l + 1);
            options[l] = KarafDistributionOption.debugConfiguration();
        }
        return options;
    }

    protected String executeCommand(final String command, Principal ... principals) {
        return executeCommand(command, COMMAND_TIMEOUT, false, principals);
    }


    protected String executeCommand(final String command, final Long timeout, final Boolean silent, final Principal... principals) {
        waitForCommandService(command);

        String response;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArrayOutputStream);
        final SessionFactory sessionFactory = getOsgiService(SessionFactory.class);
        final Session session = sessionFactory.create(System.in, printStream, System.err);

        final Callable<String> commandCallable = new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    if (!silent) {
                        System.err.println(command);
                    }
                    session.execute(command);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                printStream.flush();
                return byteArrayOutputStream.toString();
            }
        };

        FutureTask<String> commandFuture;
        if (principals.length == 0) {
            commandFuture = new FutureTask<String>(commandCallable);
        } else {
            // If principals are defined, run the command callable via Subject.doAs()
            commandFuture = new FutureTask<String>(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Subject subject = new Subject();
                    subject.getPrincipals().addAll(Arrays.asList(principals));
                    return Subject.doAs(subject, new PrivilegedExceptionAction<String>() {
                        @Override
                        public String run() throws Exception {
                            return commandCallable.call();
                        }
                    });
                }
            });
        }

        try {
            executor.submit(commandFuture);
            response = commandFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            e.printStackTrace(System.err);
            response = "SHELL COMMAND TIMED OUT: ";
        } catch (ExecutionException e) {
            Throwable cause = e.getCause().getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return response;
    }

    private void waitForCommandService(String command) {
        // the commands are represented by services. Due to the asynchronous nature of services they may not be
        // immediately available. This code waits the services to be available, in their secured form. It
        // means that the code waits for the command service to appear with the roles defined.

        if (command == null || command.length() == 0) {
            return;
        }

        int spaceIdx = command.indexOf(' ');
        if (spaceIdx > 0) {
            command = command.substring(0, spaceIdx);
        }
        int colonIndx = command.indexOf(':');
        String scope = (colonIndx > 0) ? command.substring(0, colonIndx) : "*";
        String name  = (colonIndx > 0) ? command.substring(colonIndx + 1) : command;
        try {
            long start = System.currentTimeMillis();
            long cur   = start;
            while (cur - start < SERVICE_TIMEOUT) {
                if (sessionFactory.getRegistry().getCommand(scope, name) != null) {
                    return;
                }
                Thread.sleep(100);
                cur = System.currentTimeMillis();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Explodes the dictionary into a ,-delimited list of key=value pairs
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
     *
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
     *
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
