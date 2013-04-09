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
package org.apache.karaf.cellar.cloud;

import org.joda.time.DateTime;

public class ServiceContainer {

	private DateTime registeredTime;
	private String hostName;

	public DateTime getRegisteredTime() {
		return registeredTime;
	}

	public String getHostName() {
		return hostName;
	}
	
	public ServiceContainer(String hostName, DateTime registeredTime) {
		this.registeredTime = registeredTime;
		this.hostName = hostName;
	}
}
