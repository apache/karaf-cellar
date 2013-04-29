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
package org.apache.karaf.cellar.obr;

/**
 * Cellar OBR configuration constants.
 */
public class Constants {

    public static final String BUNDLES_DISTRIBUTED_SET_NAME = "org.apache.karaf.cellar.obr.bundles";
    public static final String URLS_DISTRIBUTED_SET_NAME = "org.apache.karaf.cellar.obr.urls";

    public static final String BUNDLES_CONFIG_CATEGORY = "obr.bundles";
    public static final String URLS_CONFIG_CATEGORY = "obr.urls";

    public static final int URL_ADD_EVENT_TYPE = 1;
    public static final int URL_REMOVE_EVENT_TYPE = 0;

    public static final int BUNDLE_START_EVENT_TYPE = 1;

}
