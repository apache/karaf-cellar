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
package org.apache.karaf.cellar.shell;

import org.apache.karaf.cellar.core.Node;
import org.apache.karaf.cellar.core.shell.completer.NodeAliasCompleter;
import org.apache.karaf.cellar.core.shell.completer.NodeIdCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "cluster", name = "node-alias", description = "Set or get node alias")
@Service
public class NodeAliasCommand extends ClusterCommandSupport {

    @Argument(index = 0, name = "alias", description = "The alias to set", required = false, multiValued = false)
    String alias;

    @Option(name = "--id", description = "Look for the alias for a given node ID", required = false, multiValued = false)
    @Completion(NodeIdCompleter.class)
    String idLookup;

    @Option(name = "--alias", description = "Look for the node ID for a given alias", required = false, multiValued = false)
    @Completion(NodeAliasCompleter.class)
    String aliasLookup;

    @Override
    protected Object doExecute() throws Exception {
        if (idLookup != null) {
            Node node = clusterManager.findNodeById(idLookup);
            System.out.println(node.getAlias());
            return null;
        }
        if (aliasLookup != null) {
            Node node = clusterManager.findNodeByAlias(aliasLookup);
            System.out.println(node.getId());
            return null;
        }
        if (alias != null) {
            if (clusterManager.findNodeByAlias(alias) != null) {
                System.err.println("Alias " + alias + " already exists");
                return null;
            }
            clusterManager.setNodeAlias(alias);
        } else {
            Node node = clusterManager.getNode();
            if (node.getAlias() == null) {
                System.out.println("");
            } else {
                System.out.println(node.getAlias());
            }
        }
        return null;
    }

}
