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

import java.io.Serializable;

/**
 * OBR bundle info wrapper to be store in a cluster group.
 */
public class ObrBundleInfo implements Serializable {

    private String presentationName;
    private String symbolicName;
    private String version;

    public ObrBundleInfo(String presentationName, String symbolicName, String version) {
        this.presentationName = presentationName;
        this.symbolicName = symbolicName;
        this.version = version;
    }

    public String getPresentationName() {
        return this.presentationName;
    }

    public void setPresentationName(String presentationName) {
        this.presentationName = presentationName;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public void setSymbolicName(String symbolicName) {
        this.symbolicName = symbolicName;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObrBundleInfo info = (ObrBundleInfo) o;

        if (presentationName != null ? !presentationName.equals(info.presentationName) : info.presentationName != null) return false;
        if (symbolicName != null ? !symbolicName.equals(info.symbolicName) : info.symbolicName != null) return false;
        if (version != null ? !version.equals(info.version) : info.version != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = presentationName != null ? presentationName.hashCode() : 0;
        result = 31 * result + (symbolicName != null ? symbolicName.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

}
