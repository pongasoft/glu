.. Copyright (c) 2011-2013 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

Easy Production Setup
=====================

This document describes how to setup glu for production.

.. note:: Since 5.1.0, this page describes the *easy* production setup using the now built-in setup tool. If you want to revert to manual configuration (or are using a version prior to 5.1.0), please refer to the :doc:`old page <production-setup>`.

Requirements
------------
Currently glu requires a UNIX like platform (tested on Mac OS X) and the following version of java:

+----------------+-----------------------------------+
|glu version     |java version(s)                    |
+================+===================================+
| 5.0.0+         |java 1.7                           |
+----------------+-----------------------------------+
| 4.7.x          |java 1.6 (any VM) or java 1.7      |
+----------------+-----------------------------------+
| 4.6.x and below|java 1.6 (with Sun/Oracle VM only!)|
+----------------+-----------------------------------+


.. warning::
   Due to a `bug with OpenJDK <https://github.com/pongasoft/glu/issues/74>`_ glu prior to 4.7.0 requires the Sun Java VM.

Download glu
------------

Download the binary called ``<version>/org.linkedin.glu.packaging-all-<version>.tgz`` from `bintray <https://bintray.com/pkg/show/general/pongasoft/glu/releases>`_ glu repository.
  
Untar/Unzip in a location of your choice::

  bin/
  lib/
  models/
  packages/

.. tip::
   This documentation is available under ``packages/org.linkedin.glu.console-server-<version>/glu/docs/html``

.. note::
   The instructions on this page will assume that ``$GLU_HOME`` refers to the location of your choice.

Use the setup tool
------------------
The tool ``$GLU_HOME/bin/setup.sh`` is used for all the steps and you can get help by issuing ``$GLU_HOME/bin/setup.sh -h`` as well as checking out the :doc:`dedicated section <setup-tool>`.

.. _easy-production-setup-target-directory-tip:

.. tip::
   By default, the setup tool uses the current directory for its output. It is then recommended to ``cd`` into the target directory and issue ``$GLU_HOME/bin/setup.sh`` commands. Note that you can also use the ``-o xxxx`` to specify the target directory, or enter it when prompted.

Easy Steps
----------
If you are trying out glu and you want to be up and running quickly (especially in a distributed environment) without digging too much into the details of all possible ways of configuring glu), then simply follow these quick and easy instructions. Otherwise simply skip to :ref:`easy-production-setup-detailed-steps`.

* create a folder to generate all glu distribution (feel free to change!)::

    mkdir /tmp/glu-trial
    cd /tmp/glu-trial

* generate the keys (you will be prompted for a password)::

    $GLU_HOME/bin/setup.sh -K -o keys

* copy quick production model locally (since we are going to edit it)::

    mkdir models
    cp $GLU_HOME/models/quick-production/glu-meta-model.json.groovy models/

* edit the model (you just copied) ``models/glu-meta-model.json.groovy``:

  1. copy/paste the section (keys) from the output of the previous command
  2. change the various hosts to the hosts where you want to deploy the glu components
  3. pay attention to the ``mysqlHost`` section (check :ref:`console-configuration-database-mysql` for details for MySql)
  4. pay attention to the ``installPath`` which is where the distributions will be installed

* generate the distributions::

    $GLU_HOME/bin/setup.sh -D -o dists models/glu-meta-model.json.groovy

* execute the script to install glu (uses ``scp`` for remote, ``cp`` for ``localhost``): you may want to take a look at the script first (this uses ``installPath``)::

    ./dists/bin/install-all.sh

* start ZooKeeper cluster

  * login on each machine where there is a ZooKeeper instance and start it::

      ./bin/zookeeperctl.sh start

* configure the cluster (will work only if you have started it!)::

    $GLU_HOME/bin/setup.sh -Z -o dists models/glu-meta-model.json.groovy

* start the agents

  * login on each machine where you installed an agent and start it::

      ./bin/agentctl.sh start

* start the console

  * login on the machine where you installed the console and start it (if you are using mysql, you need to :ref:`create the user and start the database <console-configuration-database-mysql>`) first::

      ./bin/consolectl.sh start

You should now have glu up and running.

.. _easy-production-setup-detailed-steps:

Detailed steps
--------------

.. _easy-production-setup-gen-keys:

Step 1: Generate the keys ``[-K]``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The keys generated in this step are used for the communication to the agents (using REST) which happens over ssl: this allows the agents to trust the originator of the calls. In the event you do not care about security, you can simply skip this step.

.. tip::
   Prior to glu 5.1.0, setting up the keys was cumbursome and confusing. Using an automated tool that will do it for you makes it more compelling. As a result, even if you do not care about security, it is still strongly advised to generate keys.

Issue the following command (see :ref:`tip <easy-production-setup-target-directory-tip>` above on target directory)::

  > $GLU_HOME/bin/setup.sh -K

You will be prompted for a master password.

.. note::
   The master password should be strong and you should remember it. Although at this time, you will never be prompted for it again, future versions of glu may require it (for enhanced security).

.. tip::
   You may want to provide your own `X.500 distinguished name <http://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html#DName>`_ for the certificates, in which case simply provide the ``--keys-dname "CN=cName, OU=orgUnit, O=org, L=city, S=state, C=countryCode"`` parameter when you issue the command.

The output will look like this (with obviously different values)::

  > cd /tmp/prod-1
  > $GLU_HOME/bin/setup.sh -K
  Enter the output directory [/tmp/prod-1]: keys
  Generating keys...
  Enter a master password:
  2013/07/08 17:25:48.761 INFO [KeysGenerator] Created agent.keystore
  2013/07/08 17:25:49.505 INFO [KeysGenerator] Created agent.truststore
  2013/07/08 17:25:50.389 INFO [KeysGenerator] Created console.keystore
  2013/07/08 17:25:51.032 INFO [KeysGenerator] Created console.truststore
  Keys have been generated in the following folder: /private/tmp/prod-1/keys
  Copy the following section in your meta model (see comment in meta model)
  ////////////////////////////////////////
  def keys = [
    agentKeyStore: [
      uri: 'file:/private/tmp/prod-1/keys/agent.keystore',
      checksum: 'jtD9Qfs4tm8C15ZU5qmPdWYDzCl',
      storePassword: 'D_wyb-Sg3-SpD_fubdm06R93R5W2tse79y7-',
      keyPassword: 'o_-T3pW1xlmExnA0MKkl6kw55TaEJecctKt_'
    ],
    agentTrustStore: [
      uri: 'file:/private/tmp/prod-1/keys/agent.truststore',
      checksum: 'JdVhmMzJvqJKZXIZWE_HBlljoQY',
      storePassword: 't0EKxg-I9_6v6TkRAi9pMiw-J5-83pwf35NV'
    ],
    consoleKeyStore: [
      uri: 'file:/private/tmp/prod-1/keys/console.keystore',
      checksum: 'yLo5GNNYizecWIzKWYgTTzc-bx3',
      storePassword: 'bgmZ9lwF3r-n6e7oAT9BZywk9g7lt0W-i57-',
      keyPassword: '9R_L3ykZJk6goTkEo8eGDKcFMn7R6ikCM5N0'
    ],
    consoleTrustStore: [
      uri: 'file:/private/tmp/prod-1/keys/console.truststore',
      checksum: 'uFo9Io68OUy4UNs--G_WbmBngAi',
      storePassword: 'EkWVRkEpW0Wq65th9dMFEkiaEitV30wrtkhv'
    ],
  ]
  ////////////////////////////////////////

The section between the ``///`` sections will need to be copy/pasted as-is into your glu meta model (see below).

Step 2: Create your own meta-model
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The input to the next step (Step 3) is a glu :doc:`meta model <meta-model>`. The glu meta model is a file (or set of files) which describes where the various components of glu will be installed and how they will be configured.

.. tip::
   The glu distribution comes with a sample meta model under ``$GLU_HOME/models/sample-production`` which you can use as a starting point to define your own model. Also check the documentation about the :doc:`meta model <meta-model>` for more details on syntax and options.

.. note::
   The block of code related to keys generated in Step 1, needs to be copied into your meta model.

Step 3: Configuring glu (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
glu is very configurable and offers many ways of configuring:

 * simple tweaks like port numbers in the meta model
 * more advanced tweaks, like jvm parameters, in the meta model (``configTokens`` section)
 * configs roots which lets you add/delete/modify any file in the distributions that will be generated in Step 3
 * console plugins to extend/modify the behavior of the console

Check the documentation :doc:`configuring glu <glu-config>` for more details.

.. tip::
   If it is your first time deploying glu, the defaults are usually sensible and you should be good without tweaking anything.

   .. warning::
      Make sure though that you use a 'real' database for production setups as is demonstrated in the sample production meta model.

.. _easy-production-setup-gen-dist:

Step 4: Generate the distributions ``[-D]``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Now that you have created your meta model, the setup tool will generate the set of distributions ready to be installed on the various hosts.

Issue the following command (see :ref:`tip <easy-production-setup-target-directory-tip>` above on target directory)::

  > $GLU_HOME/bin/setup.sh -D <path_to_meta_model>

For example (using the sample meta model with the keys generated in Step 1)::

  > cd /tmp/prod-1
  > $GLU_HOME/bin/setup.sh -o distributions/staging -D models/staging-glu-meta-model.json.groovy
  2013/07/11 09:49:19.235 INFO [SetupMain] Generating distributions
  2013/07/11 09:49:21.702 INFO [GluPackager] Generated agent package file:/private/tmp/prod-1/distributions/staging/agents/org.linkedin.glu.agent-server-stgZkCluster-5.1.0/ => agent-host1:12906
  2013/07/11 09:49:21.707 INFO [GluPackager] Skipped agent package file:/private/tmp/prod-1/distributions/staging/agents/org.linkedin.glu.agent-server-stgZkCluster-5.1.0/ => agent-host-2:12906
  2013/07/11 09:49:21.709 INFO [GluPackager] Skipped agent package file:/private/tmp/prod-1/distributions/staging/agents/org.linkedin.glu.agent-server-stgZkCluster-5.1.0/ => agent-host3:12906
  2013/07/11 09:49:31.642 INFO [GluPackager] Generated console package file:/private/tmp/prod-1/distributions/staging/consoles/org.linkedin.glu.console-server-stgConsole-5.1.0/ => console-host1:8080
  2013/07/11 09:49:32.964 INFO [GluPackager] Generated ZooKeeper instance [1] file:/private/tmp/prod-1/distributions/staging/zookeeper-clusters/zookeeper-cluster-stgZkCluster/org.linkedin.zookeeper-server-zk-host1-2.0.0/ => zk-host1:2181
  2013/07/11 09:49:32.965 INFO [GluPackager] Generated ZooKeeper instance [2] file:/private/tmp/prod-1/distributions/staging/zookeeper-clusters/zookeeper-cluster-stgZkCluster/org.linkedin.zookeeper-server-zk-host2-2.0.0/ => zk-host2:2181
  2013/07/11 09:49:32.965 INFO [GluPackager] Generated ZooKeeper instance [3] file:/private/tmp/prod-1/distributions/staging/zookeeper-clusters/zookeeper-cluster-stgZkCluster/org.linkedin.zookeeper-server-zk-host3-2.0.0/ => zk-host3:2181
  2013/07/11 09:49:32.965 INFO [GluPackager] Generated ZooKeeper cluster [stgZkCluster] file:/private/tmp/prod-1/distributions/staging/zookeeper-clusters/zookeeper-cluster-stgZkCluster/
  2013/07/11 09:49:33.680 INFO [GluPackager] Generated agent cli package file:/private/tmp/prod-1/distributions/staging/agent-cli/org.linkedin.glu.agent-cli-5.1.0/
  2013/07/11 09:49:33.709 INFO [GluPackager] Generated console cli package file:/private/tmp/prod-1/distributions/staging/console-cli/org.linkedin.glu.console-cli-5.1.0/
  2013/07/11 09:49:33.725 INFO [GluPackager] Generated install script /private/tmp/prod-1/distributions/staging/bin/install-zookeepers.sh
  2013/07/11 09:49:33.729 INFO [GluPackager] Generated install script /private/tmp/prod-1/distributions/staging/bin/install-agents.sh
  2013/07/11 09:49:33.732 INFO [GluPackager] Generated install script /private/tmp/prod-1/distributions/staging/bin/install-consoles.sh
  2013/07/11 09:49:33.735 INFO [GluPackager] Generated install script /private/tmp/prod-1/distributions/staging/bin/install-agent-cli.sh
  2013/07/11 09:49:33.738 INFO [GluPackager] Generated install script /private/tmp/prod-1/distributions/staging/bin/install-console-cli.sh
  2013/07/11 09:49:33.741 INFO [GluPackager] Generated install script /private/tmp/prod-1/distributions/staging/bin/install-all.sh
  2013/07/11 09:49:33.741 INFO [SetupMain] All distributions generated successfully.

.. _easy-production-setup-install:

Step 5: Install the distributions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
All the distributions that were generated during Step 4 now need to be installed on each host. There are million different ways to install (and start) the distributions on each host:

  * building an image (solaris, vm, etc...) which contains the distributions
  * using chef
  * using scp
  * many many more...

Step 4 generates the distributions that are ready to be installed as-is and tell you where they are, and on which host to install them. Example::

  2013/07/11 09:49:32.964 INFO [GluPackager] Generated ZooKeeper instance [1] file:/private/tmp/prod-1/distributions/staging/zookeeper-clusters/zookeeper-cluster-stgZkCluster/org.linkedin.zookeeper-server-zk-host1-2.0.0/ => zk-host1:2181

Step 4 also generates a set of convenient install scripts using the information from the meta model (especially the ``host`` and ``install`` entries). The install scripts are convenient scripts that you can look at/tweak. They should work essentially as-is if you use ``scp`` (provided the fact that you already have the proper (ssh) credentials on the target host).

.. tip::
   The scripts use the variables ``SCP_CMD``, ``SCP_OPTIONS`` and ``SCP_USER`` so you may want to override them to make the script behave differently. For example::

     SCP_CMD="echo scp" distributions/staging/bin/install-all.sh

   will simply display what it would do without doing it.

.. note::
   ``install-all.sh`` is essentially a script that combines all the others.

.. tip::
   The install script itself can also be part of the :ref:`template processing phase <glu-config-setup-workflow>` that happens during the generation distribution and as a result you can also have your own::

      # create a file under /tmp/myFolder/config-templates/bin/install-@install.script.name@.sh.gtmpl
      # the content of this file is a template which has access to the packagedArtifacts 
      # variable (see the one built-in)
      # run the setup tool this way
      $GLU_HOME/bin/setup.sh -D -o xxxx --config-templates "<default>" --config-templates /tmp/myFolder/config-templates my-model.json.groovy

.. _easy-production-setup-zooKeeper:

Step 6: Configuring ZooKeeper ``[-Z]``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
.. note::
   Although Step 4 generates a ZooKeeper distribution, if you already have a ZooKeeper cluster installed that you want to reuse, feel free to do so. There is nothing special about the ones that comes with glu except that the configuration (which is a bit hairy!) is done for you...

After installing all the components, start each ZooKeeper instance on each host where you have installed one (there are many ways to do this as well depending on your OS provisioning choices). But the ultimate command that needs to be run is::

  > /<path to zookeeper instance install>/bin/zookeeperctl.sh start

Once the cluster is up, you can now configure it which essentially means *uploading* the ``conf`` directory (that was created with the distribution under each ZooKeeper cluster) to ZooKeeper. For this you issue the command (see :ref:`tip <easy-production-setup-target-directory-tip>` above on target directory)::

  > $GLU_HOME/bin/setup.sh -Z <path_to_meta_model>


For example (using the sample meta model with the keys generated in Step 1)::

  > cd /tmp/prod-1
  > $GLU_HOME/bin/setup.sh -o distributions/staging -Z models/staging-glu-meta-model.json.groovy
  2013/07/11 11:06:45.156 INFO [SetupMain] Configuring ZooKeeper clusters
  2013/07/11 11:06:46.400 INFO [SetupMain] Configuring ZooKeeper cluster [stgZkCluster]

.. tip::
   The command you issue should be the same you did in Step 4 with ``-Z`` instead of ``-D``

Step 7: Starting the agents
^^^^^^^^^^^^^^^^^^^^^^^^^^^
You can now start the agents::

  # for each agent
  /<path to agent install>/bin/agentctl.sh start

Step 8: Test the setup so far (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
This optional step lets you check that everything is fine so far: the ``agent-cli`` that was generated part of the distribution should be able to talk to all the agents you have installed (and started). Issue the command::

  > /<path to outputFolder>/agent-cli/org.linkedin.glu.agent-cli-<version>/bin/agent-cli.sh -s https://<agent host>:12906/ -m /
  {"mountPoints":["/"]}

Step 9: Start the console
^^^^^^^^^^^^^^^^^^^^^^^^^
.. warning::
   Prior to starting the console, you need to make sure that the database that it is going to use (which you defined in the meta model) is up and running and that the proper (database) user has been created. Check :ref:`console-configuration-database-mysql` for details for MySql.

   .. note:: 
      if you use the built-in HSQLDB, then you don't have anything to do, but it is not recommended for production setup.

You can now start the console(s)::

  /<path to console install>/bin/consolectl.sh start

.. warning:: The first time you start the console, it will create an administrator user (``admin``/``admin``). It is **strongly** recommended to change the password immediately.

.. tip::
   During boostrap, the console will automatically create the fabrics that were defined in your meta model, so you are ready to go!

.. tip::
   The documentation is automatically available when you start the server, under ``http://<consolehost>:8080/glu/docs/html/index.html``

.. note::
   If you want to deploy the console in a different web application server then check the section :ref:`console-as-a-war`.

You can now log in to the console using ``admin/admin`` for credentials and change the password.

Upgrade
-------

Check the :ref:`section <agent-auto-upgrade>` on how to upgrade the agents.
