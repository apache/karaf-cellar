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
package org.apache.karaf.cellar.samples.dosgi.greeter.client;

import org.apache.karaf.cellar.samples.dosgi.greeter.api.Greet;
import org.apache.karaf.cellar.samples.dosgi.greeter.api.GreetResponse;
import org.apache.karaf.cellar.samples.dosgi.greeter.api.Greeter;

/**
 * Call a remote Greeter service.
 */
public class GreeterClient {

    private Greeter greeter;
    private String greetMessage;
    private int count;

    public GreeterClient(Greeter greeter, String greetMessage, int count) {
        this.greeter = greeter;
        this.greetMessage = greetMessage;
        this.count = count;
    }

    public void start() {
        Greet greet  = new Greet(greetMessage);
        for (int i = 0; i < count; i++) {
            GreetResponse greetResponse = greeter.greet(greet);
            if(greetResponse != null) {
                System.out.println(greetResponse.getResponse());
            } else System.out.println("Time out!");
        }
    }

}
