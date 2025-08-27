<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->

# Apache Karaf Cellar

Apache Karaf Cellar is an Apache Karaf subproject. It provides a clustering solution for Apache
Karaf powered by Hazelcast.

## Overview

Cellar allows you to manage a cluster of several Karaf instances, providing synchronization between instances.

Here is a short list of features:

* **Discovery**: when you install Cellar into a Karaf instance, it automatically tries to discover other Cellar
  instances and join the cluster. There is no configuration required to join the cluster, the discovery is made
  behind the scene. You can use multicast or unicast for discovery.
* **Cluster group**: a Karaf node could be part of one or more cluster group. In Cellar, you can define cluster group
  depending on your requirements. The resources will be sync between nodes of the same group.
* **Distributed Configuration Admin**: Cellar distributes the configuration data. The distribution is event driven and
  filtered by group. You can tune the configuration replication using blacklist/whitelist on the configuration
  ID (PID).
* **Distributed Features Service**: Cellar distributes the features/repositories info. It's also event-driven.
* **Provisioning**: Cellar provides shell commands for basic provisioning. It can also use an OBR backend or another
  provisioning tool such as Apache ACE.

## Getting Started

For an Apache Karaf Cellar source distribution, please read
BUILDING for instructions on building Apache Karaf.

To install Apache Karaf Cellar, first you have to register the Cellar features descriptor:

```shell
karaf@root()> feature:repo-add mvn:org.apache.karaf.cellar/apache-karaf-cellar/4.4.8/xml/features
```

Now, you can install the Cellar feature simply by typing:

```shell
karaf@root()> feature:install cellar
```

The PDF manual is the right place to find any information about Karaf Cellar.

Alternatively, you can also find out how to get started here:

https://karaf.apache.org/projects.html

If you need more help try talking to us on our mailing lists

https://karaf.apache.org/community.html

If you find any issues with Apache Karaf, please submit reports
with JIRA here:

http://issues.apache.org/jira/browse/KARAF

We welcome contributions, and encourage you to get involved in the
Karaf community. If you'd like to learn more about how you can
contribute, please see:

https://karaf.apache.org/community.html

Thanks for using Apache Karaf Decanter !

**The Apache Karaf Team**
