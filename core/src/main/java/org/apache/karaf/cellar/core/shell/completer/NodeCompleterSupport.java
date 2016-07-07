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
package org.apache.karaf.cellar.core.shell.completer;

import org.apache.karaf.cellar.core.ClusterManager;
import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.List;

/**
 * Completer on the node.
 */
public abstract class NodeCompleterSupport implements Completer {

    @Reference
    private ClusterManager clusterManager;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            for (Node node : clusterManager.listNodes()) {
                if (acceptsNode(node)) {
                    if (addId()) {
                        String id = node.getId();
                        if (delegate.getStrings() != null && !delegate.getStrings().contains(id)) {
                            delegate.getStrings().add(id);
                        }
                    }
                    if (addAlias()) {
                        String alias = node.getAlias();
                        if (delegate.getStrings() != null && !delegate.getStrings().contains(alias)) {
                            delegate.getStrings().add(alias);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return delegate.complete(session, commandLine, candidates);
    }

    protected abstract boolean acceptsNode(Node node);

    protected abstract boolean addId();

    protected abstract boolean addAlias();

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

}
