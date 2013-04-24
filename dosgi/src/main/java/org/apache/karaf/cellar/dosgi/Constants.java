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

/**
 * DOSGi Constants.
 */
public abstract class Constants {

    public static final String SEPARATOR = "/";
    public static final String ALL_INTERFACES = "*";
    public static final String INTERFACE_SEPARATOR = ",";
    public static final String INTERFACE_PREFIX = "org.apache.karaf.cellar.dosgi";
    public static final String REQUEST_PREFIX = "org.apache.karaf.cellar.dosgi.request";
    public static final String RESULT_PREFIX = "org.apache.karaf.cellar.dosgi.result";
    public static final String REMOTE_ENDPOINTS = "org.apache.karaf.cellar.dosgi.endpoints";
    public static final String EXPORTED_INTERFACES = "service.exported.interfaces";
    public static final String ENDPOINT_FRAMEWORK_UUID = "frameworkUUID";

}
