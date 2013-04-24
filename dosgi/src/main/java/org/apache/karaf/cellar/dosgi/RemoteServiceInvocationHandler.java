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
package org.apache.karaf.cellar.dosgi;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.command.ExecutionContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Handler for cluster remote service invocation event.
 */
public class RemoteServiceInvocationHandler implements InvocationHandler {

    private String endpointId;
    private String serviceClass;
    private ClusterManager clusterManager;
    private ExecutionContext executionContext;

    public RemoteServiceInvocationHandler(String endpointId,String serviceClass, ClusterManager clusterManager, ExecutionContext executionContext) {
        this.endpointId = endpointId;
        this.serviceClass = serviceClass;
        this.clusterManager = clusterManager;
        this.executionContext = executionContext;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] arguments) throws Throwable {
        RemoteServiceCall remoteServiceCall = new RemoteServiceCall(clusterManager.generateId());
        remoteServiceCall.setEndpointId(endpointId);
        remoteServiceCall.setMethod(method.getName());
        remoteServiceCall.setServiceClass(serviceClass);
        List argumentList = new LinkedList();

        if(arguments != null && arguments.length > 0) {
            for(Object arg:arguments) {
                argumentList.add(arg);
            }
        }

        remoteServiceCall.setArguments(argumentList);
        Map<Node,RemoteServiceResult> results =  executionContext.execute(remoteServiceCall);

        if(results != null) {
            for(Map.Entry<Node,RemoteServiceResult> entry:results.entrySet()) {
                RemoteServiceResult result = entry.getValue();
                return result.getResult();
            }
        }
        return null;
    }

}
