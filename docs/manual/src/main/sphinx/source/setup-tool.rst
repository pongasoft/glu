.. Copyright (c) 2013 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

 
.. _setup-tool:

``setup.sh`` tool
=================
The setup tool is used during the :doc:`production setup <easy-production-setup>`. It comes bundled with glu under ``$GLU_HOME/bin`` (assuming that ``$GLU_HOME`` is the root of the glu distribution. In order to get more help, simply issue::

  $GLU_HOME/bin/setup.sh -h

.. tip::
   By default, the setup tool uses the current directory for its output. It is then recommended to ``cd`` into the target directory and issue ``$GLU_HOME/bin/setup.sh`` commands. Note that you can also use the ``-o xxxx`` to specify the target directory, or enter it when prompted.

.. tip::
   When you run with the ``-h`` option, the output will show where the defaults are located on your drive.

The tool has essentially 3 modes which are used during the setup process.

1. Generating keys ``[-K]``
---------------------------

.. image:: /images/glu-setup-K-800.png
   :align: center
   :alt: Generating keys

This command is used to generate the keys for glu (securing the communication to the agents).

Example usage::

  setup.sh -o /tmp/keys --keys-dname "CN=Yan P., OU=IT, O=Acme, L=Mountain View, S=CA, C=US"

* ``-K`` (or ``--gen-keys``): to generate the keys
* ``-o``: the directory in which to generate the set of keys 
* ``--keys-dname``: to provide your own `X.500 distinguished name <http://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html#DName>`_ for the certificates.

2. Generating the distributions ``[-D]``
----------------------------------------

.. image:: /images/glu-setup-D-800.png
   :align: center
   :alt: Generating the distributions

This command is used to generate the distributions, which are ready to install/run packaged tailored to **your** environment.

Example usage::

  setup.sh -o /tmp/distributions/staging --config-templates "<default>" \
           --config-templates /tmp/glu-meta-model/config-templates /tmp/glu-meta-model/*.json.groovy

* ``-D`` (or ``--gen-dist``): to generate the distributions
* ``-o``: the directory in which to generate the distributions
* ``--config-templates "<default>"``: use the config templates that comes with glu
* ``--config-templates xxx``: use your own (cumulative)
* ``--agents-only``: generate the distributions only for agents
* ``--consoles-only``: generate the distributions only for consoles
* ``--zookeeper-clusters-only``: generate the distributions only for ZooKeeper clusters
* ``--keys-root``: only used if you define relative uris for the keys in the meta model (rarely used)
* ``--packages-root``: only used if you do not want to use the packages that come with glu (rarely used)

.. note::
   ``config-templates`` is cumulative, meaning you can define as many as you want and they will all be processed. The order is **important**. It is strongly recommended to always use ``--config-templates "<default>"`` first, then your own.

3. Configure ZooKeeper clusters ``[-Z]``
----------------------------------------

.. image:: /images/glu-setup-Z-800.png
   :align: center
   :alt: Configure ZooKeeper clusters

This command is used to upload the configuration (that was generated during the ``-D`` phase) to the ZooKeeper clusters. This means that you should run ``-D`` prior to using this command.

.. note::
   In order for this command to succeed, the ZooKeeper clusters must be up and running.

Example usage::

  setup.sh -o /tmp/distributions/staging -Z

* ``-Z`` (or ``--configure-zookeeper-clusters``): to configure the ZooKeeper clusters
* ``-o``: the directory in which the distributions were generated (provided during ``-D``)
* ``--zookeeper-cluster-name``: only configure the cluster with the provided name (cumulative)

.. note::
   For simplicity and consistency, the ``-o`` option is being reused although in this case it is not an output folder, but an input one: Nothing will be written in this folder during this step.

.. note::
   ``zookeeper-cluster-name`` is cumulative so you can provide more than one.