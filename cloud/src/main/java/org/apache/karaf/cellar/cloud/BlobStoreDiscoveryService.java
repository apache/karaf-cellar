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

import org.apache.karaf.cellar.core.discovery.DiscoveryService;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Discovery service that use a cloud blob store.
 */
public class BlobStoreDiscoveryService implements DiscoveryService {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(BlobStoreDiscoveryService.class);

    private String provider;
    private String identity;
    private String credential;
    private String container;
    private Integer validityPeriod = 60;
    private String ipAddress = getIpAddress();

    BlobStoreContext context;
    private BlobStore blobStore;

    public BlobStoreDiscoveryService() {
        LOGGER.debug("CELLAR CLOUD: blob store discovery service initialized");
    }

    public void init() {
        try {
            if (blobStore == null) {
                if (context == null) {
                    context = new BlobStoreContextFactory().createContext(provider, identity, credential);
                }
                blobStore = context.getBlobStore();
                signIn();
            }
        } catch (Exception ex) {
            LOGGER.error("CELLAR CLOUD: error while initializing blob store discovery service", ex);
        }
    }

    public void destroy() {
        signOut();
        context.close();
    }

    public void update(Map<String, Object> properties) {
        LOGGER.debug("CELLAR CLOUD: updating properties");
    }

    /**
     * Returns a {@link Set} of IPs.
     *
     * @return a set of IPs.
     */
    @Override
    public Set<String> discoverMembers() {

        refresh();

        Set<String> members = new HashSet<String>();
        ListContainerOptions opt = new ListContainerOptions();

        PageSet<? extends StorageMetadata> pageSet = blobStore.list(container, opt);
        LOGGER.debug("CELLAR CLOUD: storage contains a page set of size {}", pageSet.size());
		for (StorageMetadata md : pageSet) {
			if (md.getType() != StorageType.BLOB) {
				//skip everything that isn't of type BLOB ...
				continue;
			}
            String ip = md.getName();
            Object obj = readBlob(container, ip);
            if (obj == null) {
                LOGGER.debug("CELLAR CLOUD: no valid object found, skipping it");
                continue;
            }
            // check if the IP hasn't been updated recently
            if (obj instanceof DateTime) {
            	LOGGER.debug("CELLAR CLOUD: retrieved a DateTime from blog store");
                DateTime registeredTime = (DateTime) obj;
                if (registeredTime != null && registeredTime.plusSeconds(validityPeriod).isAfterNow()) {
                	LOGGER.debug("CELLAR CLOUD: adding member {}", ip);
                    members.add(ip);
                } else {
                	LOGGER.debug("CELLAR CLOUD: remove container {}", ip);
                    blobStore.removeBlob(container, ip);
                }
            } else if (obj instanceof ServiceContainer) {
            	LOGGER.debug("CELLAR CLOUD: retrieved a ServiceContainer from blog store");
            	ServiceContainer serviceContainer = (ServiceContainer) obj;
            	DateTime registeredTime = serviceContainer.getRegisteredTime();
            	if (registeredTime != null && registeredTime.plusSeconds(validityPeriod).isAfterNow()) {
            		LOGGER.debug("CELLAR CLOUD: adding member {} for IP {}", serviceContainer.getHostName(), ip);
                    members.add(serviceContainer.getHostIp());
                } else {
                	LOGGER.debug("CELLAR CLOUD: remove container {}", ip);
                    blobStore.removeBlob(container, ip);
                }
            }
        }
        LOGGER.debug("CELLAR CLOUD: returning members {}", members);
        return members;
    }

    /**
     * Sign In member to the {@link DiscoveryService}.
     */
    @Override
    public void signIn() {
        DateTime now = new DateTime();
        createBlob(container, ipAddress, new ServiceContainer(getHostName(), getIpAddress(), now));
    }

    /**
     * Refresh member to the {@link DiscoveryService}.
     */
    @Override
    public void refresh() {
        DateTime now = new DateTime();
        createBlob(container, ipAddress, new ServiceContainer(getHostName(), getIpAddress(), now));
    }

    /**
     * Sign out member to the {@link DiscoveryService}.
     */
    @Override
    public void signOut() {
        if (blobStore.blobExists(container, ipAddress)) {
            blobStore.removeBlob(container, ipAddress);
        } else {
            LOGGER.debug("CELLAR CLOUD: could not find the IP address of the current node in the blob store");
        }
    }

    /**
     * Reads from a {@link BlobStore}.
     *
     * @param container the blob store container.
     * @param blobName the blob store name.
     * @return the Object read from the blob store.
     */
    protected Object readBlob(String container, String blobName) {
        Object result = null;
        ObjectInputStream ois = null;
        blobStore.createContainerInLocation(null, container);

        InputStream is = blobStore.getBlob(container, blobName).getPayload().getInput();

        try {
            ois = new ObjectInputStream(is);
            result = ois.readObject();
        } catch (IOException e) {
            LOGGER.warn("CELLAR CLOUD: error while reading blob", e);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("CELLAR CLOUD: error while reading blob", e);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // nothing to do
                }
            }
        }
        return result;
    }

    /**
     * Create a blob store.
     *
     * @param container the blob store container.
     * @param name the blob store name.
     * @param data the blob store data.
     */
    public void createBlob(String container, String name, Object data) {
        Blob blob;
        if (blobStore != null) {
            if (!blobStore.containerExists(container)) {
                blobStore.createContainerInLocation(null, container);
            }

            if (blobStore.blobExists(container, name)) {
                blob = blobStore.getBlob(container, name);
            } else {
                blob = blobStore.blobBuilder(name).build();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(baos);
                oos.writeObject(data);
                blob.setPayload(baos.toByteArray());
                blobStore.putBlob(container, blob);
            } catch (IOException e) {
                LOGGER.error("CELLAR CLOUD: error while writing blob", e);
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException e) {
                        // nothing to do
                    }
                }

                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException e) {
                        // nothing to do
                    }
                }
            }
        }
    }

    /**
     * Get the IP address of the local node.
     *
     * @return the IP address of the local node.
     */
    protected String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            LOGGER.error("CELLAR CLOUD: can't determine IP address of the local node", ex);
            return null;
        }
    }

    /**
     * Get the hostname of the local node.
     *
     * @return the hostname of the local node.
     */
    protected String getHostName() {
    	try {
			return InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			LOGGER.error("CELLAR CLOUD: unable to determine host name of the node", ex);
            return null;
		}
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public Integer getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(Integer validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

}
