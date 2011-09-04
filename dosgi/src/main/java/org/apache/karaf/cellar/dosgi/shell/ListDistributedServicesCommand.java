package org.apache.karaf.cellar.dosgi.shell;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.cellar.dosgi.Constants;
import org.apache.karaf.cellar.dosgi.EndpointDescription;

import java.util.Map;
import java.util.Set;

@Command(scope = "cluster", name = "list-distributed-services", description = "List the remote services")
public class ListDistributedServicesCommand extends CellarCommandSupport {

    private static final String LIST_FORMAT = "%-80s %-20s";

    @Override
    protected Object doExecute() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Map<String, EndpointDescription> remoteEndpoints = clusterManager.getMap(Constants.REMOTE_ENDPOINTS);
            if (remoteEndpoints != null && !remoteEndpoints.isEmpty()) {
                System.out.println(String.format(LIST_FORMAT, "Service Class", "Provider Node"));
                for (Map.Entry<String, EndpointDescription> entry : remoteEndpoints.entrySet()) {
                    EndpointDescription endpointDescription = entry.getValue();
                    String serviceClass = endpointDescription.getServiceClass();
                    Set<String> nodes = endpointDescription.getNodes();
                    for (String nodeId : nodes) {
                        System.out.println(String.format(LIST_FORMAT, serviceClass, nodeId));
                        serviceClass = "";
                    }
                }

            } else {
                System.out.println("There are no distributed services.");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
        return null;
    }
}
