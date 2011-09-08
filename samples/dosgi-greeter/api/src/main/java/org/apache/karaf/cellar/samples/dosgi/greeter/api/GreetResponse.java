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
package org.apache.karaf.cellar.samples.dosgi.greeter.api;

import java.io.Serializable;

/**
 * Object returned by the Greeter interface/service.
 */
public class GreetResponse implements Serializable {

    private Greet greet;
    private String response;

    public GreetResponse(Greet greet, String response) {
        this.greet = greet;
        this.response = response;
    }

    public Greet getGreet() {
        return greet;
    }

    public void setGreet(Greet greet) {
        this.greet = greet;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

}
