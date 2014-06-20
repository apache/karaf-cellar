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
package org.apache.karaf.cellar.samples.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.IdGenerator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator that start a Hazelcast topic.
 */
public class Activator implements BundleActivator {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private String registrationId;
    private ITopic topic;
    private MessageListener messageListener = new MessageListener();

    @Override
    public void start(BundleContext context) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        ServiceReference reference = context.getServiceReference("com.hazelcast.core.HazelcastInstance");
        HazelcastInstance instance = (HazelcastInstance) context.getService(reference);
        context.ungetService(reference);
        try {
            IdGenerator idGenerator = instance.getIdGenerator("cellar-sample-generator");
            Long id = idGenerator.newId();
            topic = instance.getTopic("cellar-sample-topic");
            registrationId = topic.addMessageListener(messageListener);
            topic.publish(new Message("id="+id));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        topic.removeMessageListener(registrationId);
    }

}
