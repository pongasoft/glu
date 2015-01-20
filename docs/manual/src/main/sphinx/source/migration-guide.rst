.. Copyright (c) 2013-2014 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

Migration Guide
===============

.. _migration-guide-overall:

General tips and guidelines
---------------------------

* In order to upgrade glu, you should determine which version you can upgrade to. The following table shows the requirements in terms of java vm for glu

  +----------------+-----------------------------------+
  |glu version     |java version(s)                    |
  +================+===================================+
  | 5.0.0+         |java 1.7                           |
  +----------------+-----------------------------------+
  | 4.7.x          |java 1.6 (any VM) or java 1.7      |
  +----------------+-----------------------------------+
  | 4.6.x and below|java 1.6 (with Sun/Oracle VM only!)|
  +----------------+-----------------------------------+

  As a result, here are the recommended migration paths:

  +----------------------+---------------------------------------+
  |You are running glu...| You should...                         |
  +======================+=======================================+
  | 5.0.0+               |upgrade to 5.3.1                       |
  +----------------------+---------------------------------------+
  | 4.7.x                |switch to java 1.7 and upgrade to 5.3.1|
  +----------------------+---------------------------------------+
  | 4.6.x and below      |upgrade to 4.7.3 (java 1.6),           |
  |                      |switch to java 1.7 and upgrade to 5.3.1|
  +----------------------+---------------------------------------+

  .. tip:: If you are upgrading from a version prior to 5.1.0 to a more recent version of glu, you can follow the :ref:`quick and easy steps <migration-guide-5.0.0-5.1.0-quick-and-easy>` to generate the distributions as it was before the change in setup.

* In general (unless noted otherwise), you should upgrade the console first, then the agents.

* It is highly recommended to use the console ``Upgrade Agents`` functionality to upgrade the agents (under the ``Admin`` tab).

* It is highly recommended to upgrade just a few agents first, make sure the upgrade process is working properly and then upgrade the rest of them in batches: if something goes wrong, it is much easier to address the issue on a smaller set of nodes.

* During the agent upgrade process, the agent is stopped and restarted. During the restart phase, the agent recreates the state as it was prior to being shutdown. In order to do this, some older versions of glu (prior to 4.6.2) may need to fetch the glu scripts they were running from their original location, so you need to make sure that this location is accessible.

.. _migration-guide-5.5.3-5.5.4:

5.5.3 -> 5.5.4
--------------
Only the agent is affected in this release and needs to be upgraded following the usual agent upgrade path.

.. _migration-guide-5.5.2-5.5.3:

5.5.2 -> 5.5.3
--------------
No specific migration steps. Only the console is affected in this release, so no need to upgrade the agents.

.. _migration-guide-5.5.1-5.5.2:

5.5.1 -> 5.5.2
--------------
This upgrade requires only the console to be upgraded.

.. note:: This release actually behaves like the documentation is describing: if you do not specify an LDAP section in the console configuration file, then LDAP will be skipped. Prior behavior was attempting to connect to ``localhost:389`` (which was a bug). If this is what you want, then simply change the configuration file as described :ref:`here <console-user-management>`.

.. _migration-guide-5.5.0-5.5.1:

5.5.0 -> 5.5.1
--------------
This upgrade requires both the console and the agents to be upgraded.

.. _migration-guide-5.4.2-5.5.0:

5.4.2 -> 5.5.0
--------------
No specific migration steps. Only the console is affected in this release, so no need to upgrade the agents.

.. note:: If you depend on glu at the binary/source code level, note that a few internal APIs have changed (ex: ``TransitionPlan``). See `commit <https://github.com/pongasoft/glu/commit/9d9759ac5672bad2db5ed716eb065250ee181f9a>`_.

.. _migration-guide-5.4.1-5.4.2:

5.4.1 -> 5.4.2
--------------

No specific migration steps. Only the setup process is fixed to take into account a different port for the console (so simply re-running the setup flow should fix the issue in the generated package).

.. _migration-guide-5.4.0-5.4.1:

5.4.0 -> 5.4.1
--------------

No specific migration steps. Only the console and the setup flow are affected in this release, so no need to upgrade the agents.

.. _migration-guide-5.3.1-5.4.0:

5.3.1 -> 5.4.0
--------------

No specific migration steps. Only the console is affected in this release, so no need to upgrade the agents.

.. _migration-guide-5.3.0-5.3.1:

5.3.0 -> 5.3.1
--------------

No specific migration steps. Only the console is affected in this release, so no need to upgrade the agents.


.. _migration-guide-5.2.0-5.3.0:

5.2.0 -> 5.3.0
--------------
* In order to benefit fully from the new tail feature, you need to upgrade the agent to 5.3.0. If you upgrade the console only, the tail will simply not refresh. You can use the auto upgrade capability of the agent to upgrade.
* If you have been using the variable ``agentZooKeeper`` in your glu script, it will conflict with the new one introduced in 5.3.0 and it is strongly suggested you rename yours.

.. _migration-guide-5.1.0-5.2.0:

5.1.0 -> 5.2.0
--------------
No specific migration steps.

If you are compiling glu, please refer to the :doc:`dev-setup` page as the quick setup guide has changed.

.. _migration-guide-5.0.0-5.1.0:

5.0.0 -> 5.1.0
--------------
The glu code itself has not changed much between 5.0.0 and 5.1.0. What has really changed is the way glu is packaged and distributed. In order to migrate, you have several approaches.

Recommended
^^^^^^^^^^^
* create a glu meta model that represents your current glu setup

  .. note:: 
     the hardest part will (most likely) be to generate the ``keys`` section: you need to take the values that you generated manually and plug them in your meta model. Here is what this section looks like with the keys that comes bundled with glu::

      def keys = [
        agentKeyStore: [
          uri: 'agent.keystore',
          checksum: 'JSHZAn5IQfBVp1sy0PgA36fT_fD',
          storePassword: 'nacEn92x8-1',
          keyPassword: 'nWVxpMg6Tkv'
        ],

        agentTrustStore: [
          uri: 'agent.truststore',
          checksum: 'CvFUauURMt-gxbOkkInZ4CIV50y',
          storePassword: 'nacEn92x8-1',
          keyPassword: 'nWVxpMg6Tkv'
        ],

        consoleKeyStore: [
          uri: 'console.keystore',
          checksum: 'wxiKSyNAHN2sOatUG2qqIpuVYxb',
          storePassword: 'nacEn92x8-1',
          keyPassword: 'nWVxpMg6Tkv'
        ],

        consoleTrustStore: [
          uri: 'console.truststore',
          checksum: 'qUFMIePiJhz8i7Ow9lZmN5pyZjl',
          storePassword: 'nacEn92x8-1',
        ],
      ]

   .. tip::
      The ``uri`` sections should point to where the keys are actually located on the file system: ``file:/full/path/to/store``)

  .. note::
     Besides the keys, any custom configuration will most likely be either handled through :ref:`meta-model-configTokens` in the model or new :ref:`glu-config-templates`.


* run the setup tool with your model and it will generate the distributions that you need.

.. _migration-guide-5.0.0-5.1.0-quick-and-easy:

Quick and easy 
^^^^^^^^^^^^^^
It is understandable that you may not want to spend the time and effort at this time to migrate using the recommended approach. If that is the case, then simply run the following command::

   $GLU_HOME/bin/setup-pre-510.sh

This will create a familiar folder (``$GLU_HOME/pre-510``) with the same distributions as before::

   agent-cli/
   agent-server/
   bin/
   console-cli/
   console-server/
   org.linkedin.glu.agent-server-upgrade-5.1.0.tgz
   org.linkedin.glu.console-5.1.0.war
   org.linkedin.zookeeper-server-2.0.0/

.. tip::
   You can use ``-d <folder>`` to generate the folder in a different location.

Start from scratch
^^^^^^^^^^^^^^^^^^
If your glu setup is fairly small, it may just be easier to start from scratch, generate a new set of keys and follow the instructions for :doc:`easy-production-setup`.

.. _migration-guide-4.7.2-5.0.0:

4.7.2 -> 5.0.0
--------------
The only migration step required for this upgrade is to make sure that you are using java 1.7.

.. warning::
   In order to migrate to 5.0.0 from an earlier version you should **first** upgrade to ``4.7.2``!

.. _migration-guide-4.7.1-4.7.2:


4.7.1 -> 4.7.2
--------------
No specific migration steps.

.. note::
   Once you have upgraded to ``4.7.2`` using jdk1.6, you should now be able to change java to 1.7 and restart all your components. This is a prerequisite to migrate to 5.0.0!

.. _migration-guide-4.6.2-4.7.1:

4.6.2 -> 4.7.1
--------------
.. warning::
   ``4.7.0`` contains a critical bug and should not be used. It is also recommended to use ``4.7.2`` instead of ``4.7.1`` if you are upgrading from an earlier version.

This release contains a major upgrade of all the libraries used by glu. The purpose of this release is to allow glu to finally be able to run under any java VM including java 1.7 (as java 1.6 is now no longer supported by Oracle). Here are the requirements in terms of VM version(s):

+----------------+-----------------------------------+
|glu version     |java version(s)                    |
+================+===================================+
| 5.0.0+         |java 1.7                           |
+----------------+-----------------------------------+
| 4.7.x          |java 1.6 (any VM) or java 1.7      |
+----------------+-----------------------------------+
| 4.6.x and below|java 1.6 (with Sun/Oracle VM only!)|
+----------------+-----------------------------------+

.. note:: One notable change is the use of the latest version of ZooKeeper (3.4.5). Although the ZooKeeper servers do not need to be upgraded (backward compatible), it is advised to upgrade them and you should follow the procedure described on the ZooKeeper web site.

Besides (optionally) upgrading the ZooKeeper servers, there are no specific migration steps for this release.
