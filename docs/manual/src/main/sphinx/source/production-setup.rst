.. Copyright (c) 2011 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

Production Setup
================

This document describes how to setup glu for production.

Requirements
------------
Currently glu requires a UNIX like platform (tested on Mac OS X and Solaris) and Java 1.6.

.. warning::
   Due to a `bug with OpenJDK <https://github.com/linkedin/glu/issues/74>`_ glu currently requires the Sun Java VM.

Step 1: Download
----------------

Download the binary called ``org.linkedin.glu.packaging-all-<version>.tgz`` from the `downloads <https://github.com/linkedin/glu/downloads>`_ section on github.
  
Untar/Unzip in a location of your choice::

  agent-cli/
  agent-server/
  bin/
  console-cli/
  console-server/
  org.linkedin.zookeeper-server-1.2.2/
  setup/

.. tip::
   This documentation is available under ``console-server/glu/docs/html/index.html``

.. _production-setup-zookeeper:

Step 2: Setup ZooKeeper
-----------------------

In a production environment you need to install ZooKeeper in a more robust setup. The recommended way is to install ZooKeeper on 3 different nodes as a replicated group of servers (the number 3 is coming directly from discussions with the ZooKeeper authors). You may want to check the `Running Replicated ZooKeeper <http://zookeeper.apache.org/doc/trunk/zookeeperStarted.html#sc_RunningReplicatedZooKeeper>`_ section on the Apache ZooKeeper documentation web site.

You have 2 options there:

1. You can download and install ZooKeeper directly from the `Apache ZooKeeper <http://zookeeper.apache.org/>`_ website (there is nothing specific in glu: it uses plain vanilla ZooKeeper)
2. You can install the one bundled in the tar file you downloaded: ``org.linkedin.zookeeper-server-<version>/`` and configure it properly according to the instructions from the `ZooKeeper <http://zookeeper.apache.org/doc/trunk/zookeeperStarted.html#sc_RunningReplicatedZooKeeper>`_ web site:

   * each server must have a file ``org.linkedin.zookeeper-server-<version>/data/myid``

     .. warning:: the content is different for each server!

   * the file ``org.linkedin.zookeeper-server-<version>/conf/zoo.cfg`` must be changed to add the ``server.X`` section

     .. note:: this file is the same for each server

.. _production-keys:

Step 3: Generate your own set of keys
-------------------------------------

The security in glu relies on the fact that the glu :doc:`agent <agent>` exposes a REST api over a secure https connection configured in client authentication mode.

.. note:: it is possible to configure the agent (and the console) to not use this security level (see :ref:`agent-configuration`, in particular the property called ``glu.agent.sslEnabled``)

   .. warning:: it is **not** recommended to disable security for a production setup

At this moment there is no automated ways to generate the keys and you need to follow the manual steps (TODO: provide a more automated way)

.. warning:: The tar file you downloaded comes with a set of keys. It is not safe to use them for production as they are obviously readily available. 

Generate keystore for the agent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. note:: we don't care about this one but it is required for SSL

You use the ``keytool`` utility (bundled with java)::

  keytool -genkey -alias agent -keystore agent.keystore -keyalg RSA -keysize 2048 -validity 2000

  Enter keystore password:  XXXXXXX
  Re-enter new password: XXXXXXX
  What is your first and last name?
    [Unknown]:  localhost
  What is the name of your organizational unit?
    [Unknown]:  Dev
  What is the name of your organization?
    [Unknown]:  LinkedIn
  What is the name of your City or Locality?
    [Unknown]:  Mountain View
  What is the name of your State or Province?
    [Unknown]:  CA
  What is the two-letter country code for this unit?
    [Unknown]:  US
  Is CN=localhost, OU=Dev, O=LinkedIn, L=Mountain View, ST=CA, C=US correct?
    [no]:  yes

  Enter key password for <agent>
	  (RETURN if same as keystore password): YYYYYYY
  Re-enter new password: YYYYYYY

.. note:: there are 2 passwords:

   1. the password for the keystore (``XXXXXXX``) (``glu.agent.keystorePassword`` in the agent configuration)
   2. the password for the key in the keystore (``YYYYYYY``) (``glu.agent.keyPassword`` in the agent configuration)

Export the RSA certificate
^^^^^^^^^^^^^^^^^^^^^^^^^^

You use the ``keytool`` utility (bundled with java)::

  keytool -export -keystore agent.keystore -alias agent -file /tmp/test.cert

  Enter keystore password:  XXXXXXX
  Certificate stored in file </tmp/test.cert>

Import the RSA certificate in the trustore
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You use the ``keytool`` utility (bundled with java)::

  keytool -import -alias agent -keystore agent.truststore -file /tmp/test.cert

  Enter keystore password:  AAAAAAA
  Re-enter new password: AAAAAAA
  Owner: CN=localhost, OU=Dev, O=LinkedIn, L=Mountain View, ST=CA, C=US
  Issuer: CN=localhost, OU=Dev, O=LinkedIn, L=Mountain View, ST=CA, C=US
  Serial number: 4a98415a
  Valid from: Fri Aug 28 13:43:06 PDT 2009 until: Wed Feb 18 12:43:06 PST 2015
  Certificate fingerprints:
	   MD5:  EC:68:E1:DA:CF:74:FC:9B:F3:5A:31:CF:8A:C8:18:EB
	   SHA1: A5:A9:5B:D1:68:9C:F6:E4:34:95:54:A6:B1:4A:5B:E8:2C:96:9F:1F
	   Signature algorithm name: SHA1withRSA
	   Version: 3
  Trust this certificate? [no]:  yes
  Certificate was added to keystore

.. note:: this trustore will be used in the console and the password (``AAAAAAA``) will be assigned to ``console.truststorePassword``

Generate keystore for the console
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. warning:: this keystore is very important and must remain protected: ``ZZZZZZZ`` must be strong!

You use the ``keytool`` utility (bundled with java)::

  keytool -genkey -alias console -keystore console.keystore -keyalg RSA -keysize 2048 -validity 2000

  Enter keystore password:  WWWWWWW
  Re-enter new password: WWWWWWW
  What is your first and last name?
    [Unknown]:  Console
  What is the name of your organizational unit?
    [Unknown]:  Dev
  What is the name of your organization?
    [Unknown]:  LinkedIn
  What is the name of your City or Locality?
    [Unknown]:  Mountain View
  What is the name of your State or Province?
    [Unknown]:  CA
  What is the two-letter country code for this unit?
    [Unknown]:  US
  Is CN=Console, OU=Dev, O=LinkedIn, L=Mountain View, ST=CA, C=US correct?
    [no]:  yes
  Enter key password for <console>
	  (RETURN if same as keystore password):  ZZZZZZZ
  Re-enter new password: ZZZZZZZ

.. note:: there are 2 passwords:

   1. the password for the keystore (``WWWWWWW``) (``console.keystorePassword`` in the console configuration)
   2. the password for the key in the keystore (``ZZZZZZZ``) (``console.keyPassword`` in the console configuration)

Secret keystore for the console (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. sidebar:: Use of secret keystore

             The secret keystore is a keystore that is used to store encrypted passwords that are automatically fed to the glu scripts. Typical usage is for configuration in order not to store plain text passwords.

The secret keystore is originally an empty keystore. The one that comes bundled with the console (called ``console.secretkeystore``) is using the default/dev password.

.. warning:: If you generate your own set of keys, you should make sure that the file pointing to by the configuration property ``console.secretkeystorePath`` does **not** exist. In other words if you use the console server that comes with glu, make sure to delete the file ``keys/console.secretkeystore``. The console will automatically create the file when it boots with the proper password.

.. tip:: If you do not want to use this feature at all, you can simply change the configuration file 
         this way::

           console.secretkeystorePath="/dev/null"


Export the RSA certificate
^^^^^^^^^^^^^^^^^^^^^^^^^^

You use the ``keytool`` utility (bundled with java)::

  keytool -export -keystore console.keystore -alias console -file /tmp/test.cert

  Enter keystore password:  WWWWWWW
  Certificate stored in file </tmp/test.cert>

Import the RSA certificate in the trustore
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You use the ``keytool`` utility (bundled with java)::

  keytool -import -alias console -keystore console.truststore -file /tmp/test.cert

  Enter keystore password:  BBBBBBB
  Re-enter new password: BBBBBBB
  Owner: CN=Console, OU=Dev, O=LinkedIn, L=Mountain View, ST=CA, C=US
  Issuer: CN=Console, OU=Dev, O=LinkedIn, L=Mountain View, ST=CA, C=US
  Serial number: 4a89a060
  Valid from: Mon Aug 17 11:24:32 PDT 2009 until: Sun Nov 15 10:24:32 PST 2009
  Certificate fingerprints:
	   MD5:  0B:B1:1A:E4:83:13:26:FF:90:8E:7A:15:78:AF:3B:27
	   SHA1: 87:12:E6:F3:A6:11:04:14:0F:C3:A0:96:B6:D5:20:83:28:CA:0E:E6
	   Signature algorithm name: SHA1withRSA
	   Version: 3
  Trust this certificate? [no]:  yes
  Certificate was added to keystore

.. note:: this trustore will be used in the agent and the password (``BBBBBBB``) will be assigned to ``glu.agent.truststorePassword``

.. _production-setup-passwords:

Step 4: Encrypt passwords and compute checksums
-----------------------------------------------

To encrypt the passwords you use a little utility bundled with glu::

  ./agent-cli/bin/password.sh

  [Password to encrypt:] AAAAAAA
  [Encrypting key:] gluos2way
  mmAikmAikm

.. note:: there is no feedback on purpose (the password is not printed on the output)

.. note:: the encrypting key is always the same and is ``gluos2way``. At this time, the only way to change it is to override the `AgentMain.groovy <https://github.com/linkedin/glu/blob/master/agent/org.linkedin.glu.agent-server-impl/src/main/groovy/org/linkedin/glu/agent/server/AgentMain.groovy>`_ class and override the ``getTwoWayCodec()`` method and provide your own main class during agent boot (``MAIN_CLASS``). See :ref:`agent-configuration` for more details on how to configure the agent.

To generate the checksum you use the same utility but you provide the file you want to compute the checksum for as an argument::

  ./agent-cli/bin/password.sh agent.keystore

  [SHA1 password:] gluos1way1
  [Encrypting key:] gluos2way
  zGt96nK2xNepHqx0OtefQf6m-3K

.. note:: the 2 values ``gluos1way1`` and ``gluos2way`` are defined in the code and the only way to change them at this point in time is to follow the steps about overriding the ``AgentMain`` class

Summary
^^^^^^^

At the end of this step, it may be a little confusing so let's recap what you should have:

+--------------------------+--------------------+--------------------+------------------------------------------------------+
|File                      |Consumer            |Default storage     |Configuration properties                              |
+==========================+====================+====================+======================================================+
|``agent.keystore``        |Agent               |ZooKeeper           |* ``glu.agent.keystorePath`` (where is the file       |
|                          |                    |                    |  located)                                            |
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``glu.agent.keystoreChecksum`` (computed)           |
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``glu.agent.keystorePassword`` (``XXXXXXX``         |
|                          |                    |                    |  encrypted)                                          |
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``glu.agent.keyPassword`` (``ZZZZZZZ`` encrypted)   |
+--------------------------+--------------------+--------------------+------------------------------------------------------+
|``agent.truststore``      |All clients of the  |locally to the      |* ``console.truststorePath`` (where is the file       |
|                          |agent (console and  |client              |  located)                                            |
|                          |agent cli)          |                    |                                                      |
|                          |                    |                    |* ``console.truststorePassword`` (``AAAAAAA``         |
|                          |                    |                    |  encrypted)                                          |
+--------------------------+--------------------+--------------------+------------------------------------------------------+
|``console.keystore``      |Console             |local to the console|* ``console.keystorePath`` (where is the file located)|
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``console.keystorePassword`` (``WWWWWWW`` encrypted)|
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``console.keyPassword`` (``ZZZZZZZ`` encrypted)     |
|                          |                    |                    |                                                      |
+--------------------------+--------------------+--------------------+------------------------------------------------------+
|``console.truststore``    |Agent               |ZooKeeper           |* ``glu.agent.truststorePath`` (where is the file     |
|                          |                    |                    |  located)                                            |
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``glu.agent.truststoreChecksum`` (computed)         |
|                          |                    |                    |                                                      |
|                          |                    |                    |* ``glu.agent.truststorePassword`` (``BBBBBBB``       |
|                          |                    |                    |  encrypted)                                          |
+--------------------------+--------------------+--------------------+------------------------------------------------------+
|``console.secretkeystore``|Console             |local to the console|* ``console.secretkeystorePath`` (where is the file   |
|                          |                    |**but** this file   |  located)                                            |
|                          |                    |should **not** exist|                                                      |
|                          |                    |if you generate your|                                                      |
|                          |                    |own keys (the       |                                                      |
|                          |                    |console **will**    |                                                      |
|                          |                    |create it)          |                                                      |
+--------------------------+--------------------+--------------------+------------------------------------------------------+

.. _production-setup-prepare-zookeeper:

Step 5: Prepare ZooKeeper
-------------------------

By now you should have ZooKeeper up and running (if you have followed :ref:`production-setup-zookeeper`).

1. Copy ``agent.keystore`` and ``console.trustore`` into ``setup/zookeeper-config``
2. Edit ``setup/zookeeper-config/config.properties`` to put your own values for the passwords and checksums (see :ref:`production-setup-passwords`)
   .. note:: you can also add/modify most of the configuration properties for the agent (see :ref:`agent-configuration`).

Use the tool provided to create a :term:`fabric`, load the keys in ZooKeeper as well as the agent configuration::

  ./bin/setup-zookeeper.sh -z <zkConnectionString> -f <fabricName>

.. note:: the ``zkConnectionString`` is of the form hostname:port (ex: ``zk01.acme.com:2181``)

.. tip:: if you want to create more than one fabric, you can reuse the same tool

.. _production-setup-agent:

Step 6: Install the agent
-------------------------

You can now install the agent on each host you will want to do deployment. The agent is contained in the folder called ``agent-server``. Check the :ref:`agent-configuration` for details on how to configure the agent. What is important is to provide the following configuration to the agent:

* the fabric (as set in :ref:`production-setup-prepare-zookeeper`)
* the ZooKeeper connection string (which, if you have followed the recommendations in :ref:`production-setup-zookeeper`, will contain a semi-colon separated list of servers (example: ``zk01.acme.com:2181;zk02.acme.com:2181;zk03.acme.com:2181``))
* the agent name (unless the default is fine)

.. tip:: Once the agent is installed, you can use the :ref:`auto upgrade <agent-auto-upgrade>` capability built into the agent

Step 7: Start the agents
------------------------

After installing the agents you can start them.

There is a way to test at this point that everything is working fine by using the agent cli. In order to do that:

1. Copy ``agent.truststore`` and ``console.keystore`` into ``agent-cli/conf/keys``
2. Edit ``agent-cli/conf/clientConfig.properties`` to put your own values for the passwords (see :ref:`production-setup-passwords`)

You can then issue the following command::

  ./bin/agent-cli.sh -s https://<agent>:12906
  [/]

If the keys, passwords and everything is fine, you will get ``[/]`` which is a list of all the mount points currently installed on the agent (all agents have a root :term:`mount point`).

.. _production-setup-console:

Step 8: Install the console
---------------------------

.. warning:: The default configuration uses HSQLDB for the database. In a production setup, it is **not** recommended to use HSQLDB. Check the section :ref:`console-configuration-database-mysql` for details on how to configure a different database.

Option 1: Install the war file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The console is packaged as a regular webapp (war file) and can simply be dropped in any servlet container (tested with tomcat). In order to run, the console requires a configuration file. See :ref:`console-configuration`. The war file is available under ``console-server/glu/repository/wars/org.linkedin.glu.console-webapp-<version>.war``.

.. note:: do not forget to copy your own version of ``agent.truststore`` and ``console.keystore`` and to put your own passwords in the config file.

.. note:: there is an example of configuration file under ``console-server/conf/glu-console-webapp.groovy``

Option 2: Use the server
^^^^^^^^^^^^^^^^^^^^^^^^

The console is also packaged as a server (using jetty) (``console-server/``) and comes with a default configuration file (under ``console-server/conf/glu-console-webapp.groovy``)

1. Copy ``agent.truststore`` and ``console.keystore`` into ``console-server/keys``
2. Edit ``conf/glu-console-webapp.groovy`` to put your own values for the passwords (see :ref:`production-setup-passwords`)

In order to start the console simply issue::

    ./bin/consolectl.sh start

The console will output a log file called ``console.log`` under ``jetty-distribution-<version>/logs``

.. warning:: Since the console has a login screen asking for user credentials, it is **strongly** recommended to run the console under https

   .. note:: Option 2 is currently **not** configured to run under https, so is **not** recommended for production use (this will be addressed in an upcoming release)

.. warning:: The first time you start the console, it will create an administrator user (``admin``/``admin``). It is **strongly** recommended to change the password immediately.

.. tip::
   If you use this option, the documentation is automatically available when you start the server, under ``http://<consolehost>:8080/glu/docs/html/index.html``

