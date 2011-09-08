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
package org.apache.karaf.cellar.samples.dosgi.greeter.service;

import org.apache.karaf.cellar.samples.dosgi.greeter.api.Greet;
import org.apache.karaf.cellar.samples.dosgi.greeter.api.GreetResponse;
import org.apache.karaf.cellar.samples.dosgi.greeter.api.Greeter;

/**
 * Implementation of the Greeter servicce.
 */
public class GreeterImpl implements Greeter {

    private int counter=0;
    private String id;

    public GreeterImpl(String id) {
        this.id = id;
    }

    @Override
    public GreetResponse greet(Greet greet) {
        String message = greet.getMessage();
        String response = message+"." +String.format("Hello from node %s count %s.",id,counter++);
        GreetResponse greetResponse = new GreetResponse(greet,response);
        return greetResponse;
    }

}
