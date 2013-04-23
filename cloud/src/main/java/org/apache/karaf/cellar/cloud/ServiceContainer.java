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

import java.io.Serializable;

import org.joda.time.DateTime;

/**
 * A cloud service container.
 */
public class ServiceContainer implements Serializable {

	private DateTime registeredTime;
	private String hostName;
	private String hostIp;
	private String hostPort;
	
	public DateTime getRegisteredTime() {
		return registeredTime;
	}

	public String getHostName() {
		return hostName;
	}
	
	public String getHostIp() {
		return hostIp;
	}
	
	public String getHostPort() {
		return hostPort;
	}

	public ServiceContainer(String hostName, String hostIp, DateTime registeredTime) {
		this(hostName, hostIp, null, registeredTime);
	}

	public ServiceContainer(String hostName, String hostIp, String hostPort, DateTime registeredTime) {
		this.registeredTime = registeredTime;
		this.hostName = hostName;
		this.hostIp = hostIp;
		this.hostPort = hostPort;
	}

}
