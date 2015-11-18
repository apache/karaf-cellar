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

import org.apache.karaf.cellar.core.command.ExecutionContext;
import org.apache.karaf.cellar.core.shell.CellarCommandSupport;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

/**
 * Abstract cluster shell command.
 */
public abstract class ClusterCommandSupport extends CellarCommandSupport {

    @Option(name = "-t", aliases = { "--timeout" }, description = "Consumer command timeout (in seconds)", required = false, multiValued = false)
    protected long timeout = 30;

    @Reference
    protected ExecutionContext executionContext;

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

}
