/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.features;

import java.net.URI;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class FeatureFinder {

    private ConfigurationAdmin configurationAdmin;

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public URI getUriFor(String name, String version) throws Exception {
        Configuration configuration = configurationAdmin.getConfiguration("org.apache.karaf.features.repos", null);
        if (configuration == null) {
            return null;
        }
        Dictionary properties = configuration.getProperties();
        if (properties == null) {
            return null;
        }
        String url = (String) properties.get(name);
        if (url == null) {
            return null;
        }
        if (version != null) {
            url = replaceVersion(url, version);
        }
        return URI.create(url);
    }

    private static String replaceVersion(String url, String version) {
        if (url.startsWith("mvn:")) {
            // mvn:groupId/artifactId/version...
            int index = url.indexOf('/');
            index = url.indexOf('/', index + 1);

            String first = url.substring(0, index);
            index = url.indexOf('/', index + 1);
            String second = url.substring(index + 1);

            return first + "/" + version + "/" + second;
        }
        return url;
    }

}
