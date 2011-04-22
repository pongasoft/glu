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

What is glu?
============
glu is a free/open source deployment and monitoring automation platform.

What problems does glu solve?
-----------------------------

glu is solving the following problems:

* deploy (and monitor) applications to an arbitrary large set of nodes: 

 * efficiently
 * with minimum/no human interaction
 * securely
 * in a reproducible manner

* ensure consistency over time (prevent *drifting*)
* detect and troubleshoot quickly when problems arise

How does it work?
-----------------

glu takes a very **declarative** approach, in which you describe/model what you want, and glu can then:

* compute the set of actions to deploy/upgrade your applications
* ensure that it remains consistent over time
* detect and alert you when there is a mismatch

The following diagram represents a system with the various components (which will be defined in greater details later):

.. |smallagent| image:: /images/agent-logo-28.png
   :alt: glu agent
   :class: logo

.. image:: /images/goe-step-0.png
   :align: center
   :width: 547
   :height: 642
   :scale: 66
   :alt: Overral system

* the nodes/hosts (bottom of the diagram) represent where applications will be deployed on
* a glu agent |smallagent| is running on each of those nodes
* ZooKeeper is used to maintain the *live* state as reported by the glu agents (blue arrows)
* the glu orchestration engine is the heart of the system

1. You define a model
^^^^^^^^^^^^^^^^^^^^^

The model is a json document in which you declare what/how/where to deploy an application::

 {
   "fabric": "prod-chicago",
   "entries": [
   {
    "agent": "node01.prod",
    "mountPoint": "/search/i001",

    "script": "http://repository.prod/scripts/webapp-deploy-1.0.0.groovy",
    "initParameters": {
       "container": {
         "skeleton": "http://repository.prod/tgzs/jetty-7.2.2.v20101205.tgz",
         "config": "http://repository.prod/configs/search-container-config-2.1.0.json",
         "port": 8080,
       },
       "webapp": {
         "war": "http://repository.prod/wars/search-2.1.0.war",
         "contextPath": "/",
         "config": "http://repository.prod/configs/search-config-2.1.0.json"
       }
    }
   }
   ]
 }

* where to deploy it: ``agent`` and ``mountPoint`` (unique key)
* how to deploy it: ``script``
* what to deploy: ``initParameters`` (which is a map made available to the ``script``)

.. tip::
   If you want to achieve reproducibility, the model should properly be versionned in a source control management system like git or svn.

2. You load the model in the glu orchestration engine
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. image:: /images/goe-step-2.1.png
   :align: center
   :width: 696
   :height: 676
   :scale: 66
   :alt: Step 2

You load the (previously defined) model in the glu orchestration engine which:

#. compares the model you defined (*desired* state) with what is currently deployed (*live* state)
#. generates a deployment plan which consists of a set of commands to run (only in the event that there is a difference between the 2 states).

3. You tell glu to execute the deployment plan
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. image:: /images/goe-step-3.1.png
   :align: center
   :width: 696
   :height: 676
   :scale: 66
   :alt: Step 3

.. note:: 
   It is important to note that glu will never do anything without **your** explicit approval: after you load the model in the orchestration engine, **you** must instruct glu to actually perform the operations (after you have had a chance to **review** them).

4. The glu agent executes the instructions and updates the state
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. image:: /images/goe-step-4.1.png
   :align: center
   :width: 696
   :height: 676
   :scale: 66
   :alt: Step 4

The glu agent then follows the instructions coming from the glu orchestration engine (over a secure HTTP/REST channel). It then propagates the new state to ZooKeeper which in turns makes it back to the orchestration engine.

5. The system is stable
^^^^^^^^^^^^^^^^^^^^^^^

.. image:: /images/goe-step-5.png
   :align: center
   :width: 696
   :height: 676
   :scale: 66
   :alt: Step 5

The desired state (coming from the static model) and the live state (computed from ZooKeeper) are now the same: the system is stable. 

The system will remain stable until something happens on either side:

* a new (different) model is loaded in the glu orchestration engine
* the live state changes because for example a machine or application went down

Key Components
--------------

Agent
^^^^^

.. sidebar:: Agent

  .. image:: /images/agent-logo-86.png
     :alt: glu agent
     :class: sidebar-logo

  More information about the :doc:`agent <agent>`.

The glu agent runs on every node/host in the system and is responsible for:

* listening to the glu orchestration engine (through a secure REST api).
* running glu scripts (the ``script`` entry defined in the model) which defines what it means to deploy and monitor an application.
* reporting its state to ZooKeeper.

Model
^^^^^

.. sidebar:: Model

  .. image:: /images/static-model-logo-86.png
     :alt: static model
     :class: sidebar-logo

  More information about the :ref:`model <goe-static-model>`.

The model is a json document which describes:

* which applications need to run 
* on which hosts 
* how to deploy and monitor them (through a glu script). 

This document is typically properly version controlled in an scm (source control management).

glu script
^^^^^^^^^^

.. sidebar:: glu script

  .. image:: /images/script-logo-86.png
     :alt: glu script
     :class: sidebar-logo

  More information about :doc:`glu script <glu-script>`.

A glu script is a set of instructions decribing how to deploy and run an application. Typically there is one glu script per type of application (for example, there is a glu script that describes how to deploy and run a webapp in a jetty container, another one that describes how to deploy and run memcache, etc...). The glu script runs in the agent on the target host and is parameterized by the init parameters found in the model.

Orchestration engine
^^^^^^^^^^^^^^^^^^^^

.. sidebar:: Orchestration Engine

  .. image:: /images/orchestration-engine-logo-86.png
     :alt: orchestration engine
     :class: sidebar-logo

  More information about the :doc:`orchestration engine <orchestration-engine>`.

The orchestration engine is a separate process responsible for:

* listening to the agent updates (through ZooKeeper) to build the *live* state
* compare the *live* state with the *desired* state (the model)
* generate the delta for visualization and deployment plan
* orchestrate the execution of the deployment plan accross the nodes (in parallel or sequentially)

.. note::
   Currently the orchestration engine is bundled inside the console (which is a webapp).

Console
^^^^^^^

.. sidebar:: glu console

  .. image:: /images/console-logo-86.png
     :alt: glu console
     :class: sidebar-logo

  More information about the :doc:`console <console>`.

The console is a web application that allows you to control glu using a web browser.

Here is a list of key features offered by the console:

* user authentication and management (ldap or console password)
* auditing (to keep track of who does what and when)
* access to all agents functionalities (like viewing log files and displaying folders, killing processes…)
* configurable to suit your needs in terms of what gets displayed and in which order
* parallel deployment accross any kinds of node 
* powerful filtering capabilities (allow to create notions like cluster for example)


ZooKeeper
^^^^^^^^^

`ZooKeeper <http://hadoop.apache.org/zookeeper/>`_ is used to maintain the state in a central location and is used for its powerful notification capabilities (ephemeral nodes and watchers). ZooKeeper is required if you are also using the console otherwise it is optional if you use only the glu agent.

Is glu really working?
----------------------

glu is **not** an academic exercise. glu has been built and successfully deployed at LinkedIn in early 2010 and then released as open source in November 2010. glu helps LinkedIn manage the complexity of releasing hundreds of applications/services on a 1000+ node environment (as of December 2010, LinkedIn had 4 different environments, from a small integration environment to 2 large production environments).

glu in practice
---------------

* `Building a monitoring solution with glu <http://www.pongasoft.com/blog/yan/glu/2011/03/18/building-monitoring-solution-with-glu/>`_
* `Continuous Deployment at outbrain <http://prettyprint.me/2011/01/24/continuous-deployment-at-outbrain/>`_


What to do next?
----------------

You may want to check the :doc:`tutorial <tutorial>` which will allow you to actually try the system quickly.
