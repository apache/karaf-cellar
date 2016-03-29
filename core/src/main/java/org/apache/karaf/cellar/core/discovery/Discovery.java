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
package org.apache.karaf.cellar.core.discovery;

/**
 * Constants for Discovery.
 */
public class Discovery {

    public static final String PID = "org.apache.karaf.cellar.discovery";
    public static final String DISCOVERED_MEMBERS_PROPERTY_NAME = "discoveredMembers";

    public static final String INTERVAL = "interval";
    public static final String TASK_ID = "discovery-task-id";

    public static final Long DEFAULT_DELAY = 1000L;
    public static final Long DEFAULT_PERIOD = 10000L;

}
