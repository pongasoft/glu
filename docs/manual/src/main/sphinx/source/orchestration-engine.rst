.. Copyright (c) 2011-2014 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

.. |orchestration-engine-logo| image:: /images/orchestration-engine-logo-86.png
   :alt: console logo
   :class: header-logo

|orchestration-engine-logo| Orchestration engine
================================================
The orchestration engine is the heart of the whole system. Its responsibility is to listen to ZooKeeper events (coming from the glu agents), compute the :term:`delta` and orchestrate the deployment plans.

.. note:: Currently the orchestration engine is bundled with the console and is not running as a separate process. Please check the :doc:`console <console>` documentation regarding installation and configuration.

Key Components
--------------

.. image:: /images/goe-in-action-413.png
   :align: center
   :alt: orchestration engine key components

.. |tracker-logo| image:: /images/tracker-logo-86.png
   :alt: tracker
   :class: header-logo

.. |live-model-logo| image:: /images/live-model-logo-86.png
   :alt: live model
   :class: header-logo

.. |static-model-logo| image:: /images/static-model-logo-86.png
   :alt: static model
   :class: header-logo

.. |delta-service-logo| image:: /images/delta-service-logo-86.png
   :alt: delta service
   :class: header-logo

.. |delta-logo| image:: /images/delta-logo-79.png
   :alt: delta
   :class: header-logo

.. |visualizer-logo| image:: /images/visualizer-logo-86.png
   :alt: visualizer
   :class: header-logo

.. |planner-logo| image:: /images/planner-logo-86.png
   :alt: planner
   :class: header-logo

.. |plan-logo| image:: /images/plan-logo-79.png
   :alt: plan
   :class: header-logo

.. |deployer-logo| image:: /images/deployer-logo-86.png
   :alt: planner
   :class: header-logo

.. _goe-tracker:

|tracker-logo| Tracker
^^^^^^^^^^^^^^^^^^^^^^
The role of the tracker is to listen to ZooKeeper events. Under the cover, the tracker uses `ZooKeeper watches <http://zookeeper.apache.org/doc/r3.3.1/zookeeperOver.html#Conditional+updates+and+watches>`_. This makes the tracker very leightweight and efficient as it receives notifications only when something changes in the system. 

For example, it gets notified immediately when:

* a new agent is added (case when a new node is added to the system)
* an agent disappears (case when a node crashes or reboot for example)
* a script state changes
* ...

From the information collected (and updated real-time) by the tracker, the orchestration engine can compute the :ref:`live model <goe-live-model>`.

.. _goe-live-model:

|live-model-logo| Live Model
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The :term:`live model` is computed by the orchestration engine from the information collected by the :ref:`tracker <goe-tracker>`. The live model is the representation of the :term:`live state` which is used for computing the :ref:`delta <goe-delta>`. Besides being used as an input to the :ref:`delta service <goe-delta-service>` in order to compute the delta, the live model has several representation that you have access to:

From the console
""""""""""""""""
If you navigate to an agent view page in the console, and for any given entry you click on the ``View Details`` (toggle), you will see something like this:

.. image:: /images/goe-live-model-console-600.gif
   :align: center
   :alt: console entry detail

The information displayed here is exactly what the tracker has captured from ZooKeeper.

.. note:: In this screenshot you can even see how the script running on the remote agent has detected a failure and reported it. Thanks to ZooKeeper watches, the failure is propagated real-time and made available so that the user can fix the problem.

From the command line
"""""""""""""""""""""

From a shell terminal, you can issue::

  ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -b -l status

The important flag is ``-l`` which request the *live* model. You get an output very similar to the one in the console (more detailed though)::

  {
    "entries": [{
      "agent": "agent-1",
      "initParameters": {
        "port": 9000,
        "skeleton": "http://localhost:8080/glu/repository/tgzs/jetty-distribution-7.2.2.v20101205.tar.gz",
        "webapps": [
          {
            "contextPath": "/cp1",
            "monitor": "/monitor",
            "war": "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-2.2.0.war"
          },
          {
            "contextPath": "/cp2",
            "monitor": "/monitor",
            "war": "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-2.2.0.war"
          }
        ]
      },
      "metadata": {
        "cluster": "c1",
        "container": {"name": "sample"},
        "currentState": "stopped",
        "error": "Server down detected. Check the log file for errors.",
        "modifiedTime": 1302883875857,
        "product": "product1",
        "scriptState": {
          "script": {
            "gcLog": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/logs/gc.log",
            "logsDir": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/logs/",
            "port": 9000,
            "serverCmd": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/bin/jetty-ctl.sh",
            "serverLog": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/logs/start.log",
            "serverRoot": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/",
            "version": "2.2.0",
            "webapps": {
              "/cp1": {
                "context": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/contexts/_cp1.xml",
                "contextPath": "/cp1",
                "localWar": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/wars/_cp1.war",
                "monitor": "/monitor",
                "remoteWar": "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-2.2.0.war"
              },
              "/cp2": {
                "context": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/contexts/_cp2.xml",
                "contextPath": "/cp2",
                "localWar": "file:/export/content/glu/org.linkedin.glu.packaging-all-2.2.0/apps/sample/i001/wars/_cp2.war",
                "monitor": "/monitor",
                "remoteWar": "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-2.2.0.war"
              }
            }
          },
          "stateMachine": {
            "currentState": "stopped",
            "error": "Server down detected. Check the log file for errors."
          },
          "timers": [{
            "repeatFrequency": "15s",
            "timer": "serverMonitor"
          }]
        },
        "version": "1.0.0"
      },
      "mountPoint": "/sample/i001",
      "script": "http://localhost:8080/glu/repository/scripts/org.linkedin.glu.script-jetty-2.2.0/JettyGluScript.groovy",
      "tags": [
        "frontend",
        "osx",
        "webapp"
      ]
    }],
    "fabric": "glu-dev-1",
    "metadata": {
      "accuracy": "ACCURATE",
      "emptyAgents": []
    }
  }


.. note::
   without any filter (see :doc:`filtering`), you will get the entire model, not just the single entry. In order to select a single entry you can use the filter shortcut ``-I agent-1:/sample/i001`` like this::

     ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -b -l -I agent-1:/sample/i001 status
   

.. _goe-static-model:

|static-model-logo| Static Model
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The :term:`static model` is an input to the orchestration engine. It describes the state of the entire system (for a given :term:`fabric`). The static model gets *loaded* in the orchestration engine either from the console web application, REST api or command line (``-m`` option). Once loaded, the static model remains the *current* model until a new one is loaded.

.. tip:: Check out the separate entry about the :doc:`static model <static-model>` for more information.

.. _goe-delta-service:

|delta-service-logo| Delta Service
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The delta service is in charge of computing the :ref:`delta <goe-delta>` between the :ref:`live model <goe-live-model>` and the :ref:`static model <goe-static-model>`. As mentionned previously, the static model used is the latest one that was loaded (or selected as current). The live model is coming from the :ref:`tracker <goe-tracker>`.

.. note:: 
   In the event that there is an issue with the connection to ZooKeeper, the delta service will still be able to generate *deltas* but they will be flagged accordingly because the live model is no longer real-time. In the output of the :ref:`live model <goe-live-model>` you can see this in action (in this case there was no problem)::

    "metadata": {
      "accuracy": "ACCURATE",
      ...
    }

.. _goe-delta:

|delta-logo| Delta
^^^^^^^^^^^^^^^^^^

The :term:`delta` is the result of the computation done by the :ref:`delta service <goe-delta-service>`. The delta is fed into the :ref:`visualizer <goe-visualizer>` and/or the :ref:`planner <goe-planner>`.

.. note:: At this moment, the delta is an internal concept not directly available but as it is driving the visualizer and planner, you have the ability to see different representations of it.

.. _goe-visualizer:

|visualizer-logo| Visualizer
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The visualizer is in charge of turning the delta into a *visual* representation. This is represented in the console by the dashboard:

.. image:: /images/goe-visualizer-dashboard.png
   :align: center
   :alt: console dashboard

In this screenshot which represents 3 applications deployed on 1 agent (coming from the :doc:`tutorial <tutorial>`) you can directly see the delta in action: 

* when a row is green (2nd row), it means that there is actually no delta: the static model and the live model are identical (or another way to say it, is that the desired state matches the live state)
* when a row is red (1st and 3rd row), it means that there is a discrepancy between the static model and the live model. The last column (``status``) is giving more information about what kind of delta was detected:
   * for row 1, the application is actually deployed but is not running (this is what you can see in the screenshot for the :ref:`live model <goe-live-model>`): the entry appears in the live model but with a mismatching state
   * for row 3, the application is not deployed at all: the entry is totally absent from the live model

.. _goe-planner:

|planner-logo| Planner
^^^^^^^^^^^^^^^^^^^^^^

The planner is in charge of turning the delta into a :ref:`deployment plan <goe-plan>`.

.. _goe-plan:

|plan-logo| Deployment Plan
^^^^^^^^^^^^^^^^^^^^^^^^^^^

The :term:`deployment plan` is the set of instructions (generated by the :ref:`planner <goe-planner>`) which can be fed to the :ref:`deployer <goe-deployer>` in order to *fix* the delta computed by the :ref:`delta service <goe-delta-service>`. The idea is that if all instructions get executed successfully, then there won't be any delta anymore. 

An instruction is (usually) a call (using the secure REST api) to an agent to perform a particular action (as described in the section ":ref:`agent-glu-script-engine`").

.. note:: 
   As mentionned in the :doc:`index` section, the plan is **never** executed unless **you** give your explicit approval.

The deployment plan has several representations:

From the Console
""""""""""""""""

Using the same delta you could visualize on the :ref:`dashboard <goe-visualizer>`, the planner can compute various plans depending on what you instruct it to do.

From the console, you have the ability to 

* select a sequential plan:

  .. image:: /images/goe-plan-sequential.png
     :align: center
     :alt: console sequential plan

* select a parallel plan:

  .. image:: /images/goe-plan-parallel.png
     :align: center
     :alt: console parallel plan

.. note:: the numbers in front of each instruction show you in which order they will be executed. A parallel plan will execute much faster than a sequential plan since the instructions will run in parallel so you don't have to wait for previous instructions to complete. In a sequential plan, an error in an instruction will abort the entire remaining of the plan.

From the Command Line
"""""""""""""""""""""

The exact same data is also available from the command line (hence the REST api)::

  ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -a -n deploy

will produce the following output (sequential)::

  <?xml version="1.0"?>
  <plan name="origin=rest - action=deploy - filter=all - SEQUENTIAL" fabric="glu-dev-1" 
        systemId="4836742aa34f6915fae3c0f46fbcc86ea381df74" savedTime="1302892719254">
    <sequential origin="rest" action="deploy" filter="all">
      <sequential agent="agent-1" mountPoint="/sample/i001">
        <leaf name="Start agent-1:/sample/i001 on agent-1" />
      </sequential>
      <sequential agent="agent-1" mountPoint="/sample/i003">
        <leaf name="Install script for installation agent-1:/sample/i003 on agent-1" />
        <leaf name="Install agent-1:/sample/i003 on agent-1" />
        <leaf name="Configure agent-1:/sample/i003 on agent-1" />
        <leaf name="Start agent-1:/sample/i003 on agent-1" />
      </sequential>
    </sequential>
  </plan>

And the command (``-p`` means parallel)::

  ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -a -n -p deploy

will produce the following output (parallel)::

  <?xml version="1.0"?>
  <plan name="origin=rest - action=deploy - filter=all - PARALLEL" fabric="glu-dev-1" 
        systemId="4836742aa34f6915fae3c0f46fbcc86ea381df74" savedTime="1302892859008">
    <parallel origin="rest" action="deploy" filter="all">
      <sequential agent="agent-1" mountPoint="/sample/i001">
        <leaf name="Start agent-1:/sample/i001 on agent-1" />
      </sequential>
      <sequential agent="agent-1" mountPoint="/sample/i003">
        <leaf name="Install script for installation agent-1:/sample/i003 on agent-1" />
        <leaf name="Install agent-1:/sample/i003 on agent-1" />
        <leaf name="Configure agent-1:/sample/i003 on agent-1" />
        <leaf name="Start agent-1:/sample/i003 on agent-1" />
      </sequential>
    </parallel>
  </plan>

.. note:: The ``-n`` option means *dry-run* and it allows you to display the deployment plan instead of executing it!

.. _goe-deployer:

|deployer-logo| Deployer
^^^^^^^^^^^^^^^^^^^^^^^^

The deployer is responsible to execute the :ref:`deployment plan <goe-plan>` computed by the :ref:`planner <goe-planner>`. The deployer knows how to deploy arbitrarily complex deployment plans (sequential / parallel nested at any level) and reports its progress as it goes along, allowing the very dynamic view available in the console:

.. image:: /images/goe-deployer-in-action.png
   :align: center
   :alt: deployer in action

.. note:: as you can see on the screenshot, every running action can be cancelled in the event there is a need for it.

.. note:: 
   the command line is also using the reporting capabilities of the deployer to display a progress bar:

   .. image:: /images/tutorial/tutorial-plan-progress-cli.png
      :align: center
      :alt: plan progress from the cli

.. _goe-rest-api:

REST api
--------
The orchestration engine offers a REST api (served by the console webapp).

.. note:: You do not have to use the REST api directly: there is a convenient :ref:`command line interface (cli) <goe-cli>` which is already using it. You would use the REST api directly, in cases when the cli is not enough or you want to have more control for example.

Security / Authorization
^^^^^^^^^^^^^^^^^^^^^^^^
The security model is simply implementing http basic authorization: every request needs to come with an ``Authorization`` header with the following format::

    Authorization: Basic <base64(username:password)>

.. note:: The username and password are only slightly obfuscated (base 64) but it is not an issue because the cli talks to the console over https and as a result the headers are never traveling over an insecure channel.


.. tip:: You can change the way authentication and authorization are handled entirely by writing your own plugin!

A few concepts
^^^^^^^^^^^^^^

You may wonder what is the difference between the 3 notions exposed by the REST API: plans, current deployments and archived deployments.

* A plan (as exposed by the ``/plans*`` APIs) simply represents a list of instructions that glu *will* execute. If you use the console, you can get a good idea of what a plan looks like:

   .. image:: /images/tutorial/tutorial-select-plan-2.png
      :align: center
      :alt: Dashboard shows the delta

  Currently a plan only lives (for a short time) in memory, between the time it gets created (``POST /plans``) and the time it gets executed. There is no persistent storage. 

  .. note:: this may change in the future as it would be convenient to create a plan just once and execute it at a later time or repeatedly.

* Once the plan is created and you execute it (``POST /plan/<planId>/execution``), it becomes a deployment and 2 things happen:

  1. a memory representation of the deployment is created: the current deployment
  2. a database entry is created for this deployment: the archived deployment

* The current deployment lives in memory because it is used by the orchestration engine to execute the steps that needs to happen. It is dynamically updated as steps complete. The current deployment will remain in memory (even after it completes) until either you shutdown the console (so the memory is gone...) or you archive it. The current deployment contains more details than the archived one and can be seen in the console when clicking on the `Plans` tab.

* The archived deployment is created (in the database) when the plan starts executing and then it is updated again **only** when the current deployment completes (whether it was successful or not). The archived deployment is permanent (because it is stored in the database) and can be seen in the console when clicking on the `Plans/Archived` tab.

.. tip:: There is a relationship between a plan execution and the current and archived plan: the ``executionId`` is in fact the 
   ``deploymentId``::

         GET /plans/<planId>/executions/<executionId>

         is equivalent to 

         GET /deployment/current/<executionId>

         and if you want to access the archived version you can issue

         GET /deployment/archived/<executionId>

.. warning:: Currently, the ``/plans*`` APIs are only dealing with the plans that have been created through the REST API. The plans created and executed from the web interface use a different mechanism and won't appear in those calls. This will be adressed in an upcoming release. The ``/deployments*`` calls work whether the REST API or the web interface was used.

API
^^^
Main URI: ``/console/rest/v1/<fabric>`` (all the URIs in the following table start with the main URI)

+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|Method     |URI                                        |Description                       |Details                                   |
+===========+===========================================+==================================+==========================================+
|``GET``    |None                                       |Returns details for fabric        |:ref:`view <goe-rest-api-get-fabric>`     |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``PUT``    |None                                       |Add/Update a fabric               |:ref:`view <goe-rest-api-put-fabric>`     |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``DELETE`` |None                                       |Delete a fabric                   |:ref:`view <goe-rest-api-delete-fabric>`  |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``HEAD``   |``/agents``                                |Returns the number of agents      |:ref:`view <goe-rest-api-head-agents>`    |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/agents``                                |List all the agents               |:ref:`view <goe-rest-api-get-agents>`     |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/agent/<agentName>``                     |View details about the agent      |:ref:`view <goe-rest-api-get-agent>`      |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``DELETE`` |``/agent/<agentName>``                     |Remove all knowledge of an agent  |:ref:`view <goe-rest-api-delete-agent>`   |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``PUT``    |``/agent/<agentName>/fabric``              |Sets the fabric for the agent     |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-put-agent-fabric>`          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``DELETE`` |``/agent/<agentName>/fabric``              |Clears the fabric for the agent   |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-delete-agent-fabric>`       |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/agents/versions``                       |List all the agents versions      |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-get-agents-versions>`       |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``POST``   |``/agents/versions``                       |Upgrade the agents                |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-post-agents-versions>`      |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/plans``                                 |List all the plans                |:ref:`view <goe-rest-api-get-plans>`      |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``POST``   |``/plans``                                 |Create a plan                     |:ref:`view <goe-rest-api-post-plans>`     |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/plan/<planId>``                         |View the plan (as an xml document)|:ref:`view <goe-rest-api-get-plan>`       |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``POST``   |``/plan/<planId>/execution``               |Executes the plan                 |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-post-plan-execution>`       |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/plan/<planId>/executions``              |List all the executions for a plan|:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-get-plan-executions>`       |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``HEAD``   |``/plan/<planId>/execution/<executionId>`` |Returns the status of the         |:ref:`view                                |
|           |                                           |execution                         |<goe-rest-api-head-plan-execution>`       |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/plan/<planId>/execution/<executionId>`` |Returns the execution as an xml   |:ref:`view                                |
|           |                                           |document                          |<goe-rest-api-get-plan-execution>`        |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``DELETE`` |``/plan/<planId>/execution/<executionId>`` |Aborts the execution              |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-delete-plan-execution>`     |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/deployments/current``                   |List all current deployments      |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-get-deployments-current>`   |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``DELETE`` |``/deployments/current``                   |Archive all current deployments   |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-delete-deployments-current>`|
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/deployment/current/<deploymentId>``     |View details about the current    |:ref:`view                                |
|           |                                           |deployment                        |<goe-rest-api-get-deployment-current>`    |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``HEAD``   |``/deployment/current/<deploymentId>``     |View info only about the current  |:ref:`view                                |
|           |                                           |deployment                        |<goe-rest-api-head-deployment-current>`   |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``DELETE`` |``/deployment/current/<deploymentId>``     |Archive the current deployment    |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-delete-deployment-current>` |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``HEAD``   |``/deployments/archived``                  |Returns the total count of        |:ref:`view                                |
|           |                                           |archived deployments              |<goe-rest-api-head-deployments-archived>` |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/deployments/archived``                  |List the archived deployments     |:ref:`view                                |
|           |                                           |(paginated!)                      |<goe-rest-api-get-deployments-archived>`  |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``HEAD``   |``/deployment/archived/<deploymentId>``    |View info about the archived      |:ref:`view                                |
|           |                                           |deployment                        |<goe-rest-api-head-deployment-archived>`  |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/deployment/archived/<deploymentId>``    |View details about the archived   |:ref:`view                                |
|           |                                           |deployment                        |<goe-rest-api-get-deployment-archived>`   |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``POST``   |``/model/static``                          |Loads the (desired) model in the  |:ref:`view                                |
|           |                                           |console                           |<goe-rest-api-post-model-static>`         |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/model/static``                          |Retrieves the current loaded model|:ref:`view                                |
|           |                                           |(aka 'desired' state)             |<goe-rest-api-get-model-static>`          |
|           |                                           |                                  |                                          |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/model/live``                            |Retrieves the current live model  |:ref:`view                                |
|           |                                           |coming from ZooKeeper (aka current|<goe-rest-api-get-model-live>`            |
|           |                                           |state)                            |                                          |
|           |                                           |                                  |                                          |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/model/delta``                           |Retrieves the delta between static|:ref:`view                                |
|           |                                           |model and live model              |<goe-rest-api-get-model-delta>`           |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``POST``   |``/agent/<agentName>/commands``            |Executes a shell command          |:ref:`view                                |
|           |                                           |                                  |<goe-rest-api-post-command-execute>`      |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/command/<commandId>/streams``           |Retrieves the streams (=result)   |:ref:`view                                |
|           |                                           |of the command execution          |<goe-rest-api-get-command-streams>`       |
|           |                                           |                                  |                                          |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+

Any fabric related URI: ``/console/rest/v1/-`` (all the URIs in the following table start with this URI)

+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|Method     |URI                                        |Description                       |Details                                   |
+===========+===========================================+==================================+==========================================+
|``GET``    |``/``                                      |Returns the list of fabrics       |:ref:`view <goe-rest-api-get-fabrics>`    |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+
|``GET``    |``/agents``                                |Returns the map of associations   |:ref:`view                                |
|           |                                           |agent -> fabric                   |<goe-rest-api-get-agents-fabrics>`        |
+-----------+-------------------------------------------+----------------------------------+------------------------------------------+

.. _goe-rest-api-get-fabric:

Fabric details
""""""""""""""

* Description: Retrieve the details about the fabric

* Request: ``GET``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

   * body: json map with the details of the fabric

  * ``404`` (``NOT FOUND``) when there is no such fabric

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1?prettyPrint=true"
     < HTTP/1.1 200 OK
     {
       "color": "#005a87",
       "name": "glu-dev-1",
       "zkConnectString": "localhost:2181",
       "zkSessionTimeout": "30s"
     }

.. _goe-rest-api-put-fabric:

Add/Update a fabric
"""""""""""""""""""

* Description: Add (or update if already exists) a fabric

* Request: ``PUT``

  request parameters:

  * ``zkConnectString=localhost:2181`` the connection string to your ZooKeeper setup

  * ``zkSessionTimeout=30s`` the timeout for ZooKeeper sessions

  * ``color=%23ff00ff`` the color (for the ui top navigation bar), can be anything that is useable as a color in css (ex of valid input: ``red``, ``#ff00ff`` (which must be properly query string encoded => ``#`` gets encoded into ``%23``))

* Response: 

  * ``200`` (``OK``) when the update was successful

  * ``400`` (``BAD REQUEST``) when the parameters are not valid (body will contain details about the error)

* Example::

     curl -v -u "glua:password" -X PUT "http://localhost:8080/console/rest/v1/glu-dev-3?zkConnectString=localhost:2181&zkSessionTimeout=30s&color=%23ff00ff"
     > PUT /console/rest/v1/glu-dev-3?zkConnectString=localhost:2181&zkSessionTimeout=30s&color=%23ff00ff HTTP/1.1
     < HTTP/1.1 200 OK

.. _goe-rest-api-delete-fabric:

Delete a fabric
"""""""""""""""

* Description: Delete a fabric

* Request: ``DELETE``

* Response: 

  * ``200`` (``OK``) when the delete was successful

  * ``404`` (``NOT FOUND``) when the fabric was already deleted

* Example::

     curl -v -u "glua:password" -X DELETE "http://localhost:8080/console/rest/v1/glu-dev-3"
     > DELETE /console/rest/v1/glu-dev-3 HTTP/1.1
     < HTTP/1.1 200 OK


.. _goe-rest-api-head-agents:

Agents count
""""""""""""

* Description: Returns the number of agents (in the fabric specified in the URL)

* Request: ``HEAD /agents``

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` with the number of agents
 
  * ``204`` (``NO CONTENT``) when there is no agent

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agents" --head
     < HTTP/1.1 200 OK
     < X-glu-count: 4

.. _goe-rest-api-get-agents:

List all the agents
"""""""""""""""""""

* Description: List all the agents

* Request: ``GET /agents``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` with the number of agents
    * body: json map where key is agent name and value is another map with all the agents properties (as can be seen in the console when looking at an individual agent and clicking `View Details`).

  * ``204`` (``NO CONTENT``) when there is no agent

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agents?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-count: 4
     {
       "agent-1": {
         "glu.agent.apps": "/export/content/glu/devsetup/apps/agent-1",
         "glu.agent.configURL": "zookeeper:/org/glu/agents/fabrics/glu-dev-1/config/config.properties",
         "glu.agent.dataDir": "/export/content/glu/devsetup/agent-1/data",
         "glu.agent.fabric": "glu-dev-1",
         ...
         "viewURL": "http://localhost:8080/console/rest/v1/glu-dev-1/agent/agent-1"
       },
       "agent-10": {
         ...
         "viewURL": "http://localhost:8080/console/rest/v1/glu-dev-1/agent/agent-10"
       },
       ...
     }

.. _goe-rest-api-get-agent:

View agent details
""""""""""""""""""

* Description: View the details of a single agent

* Request: ``GET /agent/<agentName>``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

   * body: json map with a single key ``details`` containing another map with all the agents properties (as can be seen in the console when looking at an individual agent and clicking `View Details`).
     .. note:: In an upcoming release there will be other keys in the map to represent also the entries deployed on the agent (as can be seen in the console when looking at an individual agent)

  * ``404`` (``NOT FOUND``) when there is no such agent

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agent/agent-1?prettyPrint=true"
     < HTTP/1.1 200 OK
     {"details": {
       "glu.agent.apps": "/export/content/glu/devsetup/apps/agent-1",
       "glu.agent.configURL": "zookeeper:/org/glu/agents/fabrics/glu-dev-1/config/config.properties",
       "glu.agent.dataDir": "/export/content/glu/devsetup/agent-1/data",
       "glu.agent.fabric": "glu-dev-1",
       "glu.agent.homeDir": "/export/content/glu/devsetup/agent-1",
       "glu.agent.hostname": "192.168.0.150",
       "glu.agent.hostnameFactory": ":ip",
       "glu.agent.keystoreChecksum": "JSHZAn5IQfBVp1sy0PgA36fT_fD",
       "glu.agent.keystorePath": "zookeeper:/org/glu/agents/fabrics/glu-dev-1/config/agent.keystore",
       "glu.agent.logDir": "/export/content/glu/devsetup/agent-1/data/logs",
       "glu.agent.name": "agent-1",
       "glu.agent.persistent.properties": "/export/content/glu/devsetup/agent-1/data/config/agent.properties",
       "glu.agent.pid": "4641",
       "glu.agent.port": "13906",
       "glu.agent.rest.nonSecure.port": "12907",
       "glu.agent.rest.server.defaultThreads": "3",
       "glu.agent.scriptRootDir": "/export/content/glu/devsetup/apps/agent-1",
       "glu.agent.scriptStateDir": "/export/content/glu/devsetup/agent-1/data/scripts/state",
       "glu.agent.sslEnabled": "true",
       "glu.agent.tempDir": "/export/content/glu/devsetup/agent-1/data/tmp",
       "glu.agent.truststoreChecksum": "qUFMIePiJhz8i7Ow9lZmN5pyZjl",
       "glu.agent.truststorePath": "zookeeper:/org/glu/agents/fabrics/glu-dev-1/config/console.truststore",
       "glu.agent.version": "3.2.0-SNAPSHOT",
       "glu.agent.zkConnectString": "localhost:2181",
       "glu.agent.zkProperties": "/export/content/glu/devsetup/agent-1/data/config/zk.properties",
       "glu.agent.zkSessionTimeout": "5s",
       "glu.agent.zookeeper.root": "/org/glu"
    }}

.. _goe-rest-api-delete-agent:

Remove all knowledge of an agent
""""""""""""""""""""""""""""""""

* Description: This call should be used in the scenario when you want to `decommission` a node: all data stored in ZooKeeper in regards to this agent will be wiped out.

* Request: ``DELETE /agent/<agentName>``

* Response: 

  * ``200`` (``OK``) if the agent was succesfully cleaned
  * ``404`` (``NOT FOUND``) if the agent did not exist in the first place
  * ``409`` (``CONFLICT``) if the agent is still up and running!

* Example::

    curl -v -u "glua:password" -X DELETE "http://localhost:8080/console/rest/v1/glu-dev-1/agent/agent-1"
    > DELETE /console/rest/v1/glu-dev-1/agent/agent-1 HTTP/1.1
    < HTTP/1.1 200 OK


.. _goe-rest-api-put-agent-fabric:

Assign a fabric to an agent
"""""""""""""""""""""""""""

* Description: Assigns a fabric to an agent (and optionally configures it)

* Request: ``PUT /agent/<agentName>/fabric``

  optional request parameters:

  * ``host=<ip or hostname>`` for configuring the agent using the ip or the hostname of the agent (this assumes that the configuration port is ``12907``)
  * ``uri=<configuration uri>`` for configuring the agent using the given uri (this form allows you to use a different port (ex: ``http://x.x.x.x:13906/config``))

* Response: 

  * ``200`` (``OK``)
  * ``400`` (``BAD REQUEST``) if missing or unknown fabric
  * ``409`` (``CONFLICT``) when the configuration phase failed

* Example::

     curl -v -u "glua:password" -X PUT "http://localhost:8080/console/rest/v1/glu-dev-1/agent/xeon/fabric?host=127.0.0.1"
     < HTTP/1.1 200 OK

.. tip:: "Configuring" the agent means that the orchestration engine will issue a REST call to the agent with its own ZooKeeper connect string as configuration. This allows the use case where the agent is started without a ZooKeeper at all, so the agent is simply waiting for somebody to tell it where is his ZooKeeper.
         This is what you see in the agent log::

             Waiting for glu.agent.zkConnectString (rest:put:http://xeon.local:12907)

.. _goe-rest-api-delete-agent-fabric:

Clears the fabric for an agent
""""""""""""""""""""""""""""""

* Description: Simply clears the fabric previously associated to an agent

* Request: ``DELETE /agent/<agentName>/fabric``

* Response: 

  * ``200`` (``OK``) if it worked
  * ``404`` (``NOT FOUND``) if there was already no fabric
  * ``400`` (``BAD REQUEST``) if missing or unknown fabric

* Example::

     curl -v -u "glua:password" -X DELETE "http://localhost:8080/console/rest/v1/glu-dev-1/agent/xeon/fabric"
     < HTTP/1.1 200 OK

.. _goe-rest-api-get-agents-versions:

List all the agents versions
""""""""""""""""""""""""""""

* Description: List all the agents versions

* Request: ``GET /agents/versions``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` with the number of agents
    * body: json map where key is agent name and value is the agent version

  * ``204`` (``NO CONTENT``) when there is no agent

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agents/versions?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-count: 4
     {
       "agent-1": "3.2.0",
       "agent-10": "3.1.0",
       "agent-4": "3.2.0",
       "agent-7": "3.2.0"
    }

.. _goe-rest-api-post-agents-versions:

Upgrade the agents
""""""""""""""""""

* Description: Create a plan to upgrade the agents. You must then execute the plan as described in :ref:`Executing a plan <goe-rest-api-post-plan-execution>`

* Request: ``POST /agents/versions``

 * The post content type should be ``application/x-www-form-urlencoded``
 * The body of the post should be a well formed query string containing the following parameters:

   * ``version``: the version of the new agent
   * ``coordinates``: the url of the agent upgrade tarball
   * ``agents``: 1 per agent name you want to upgrade

* Response: 

  * ``201`` (``Created``) with:

    * headers: ``Location`` containing the url to POST to in order to execute the plan (see :ref:`Executing a plan <goe-rest-api-post-plan-execution>`)

  * ``204`` (``NO CONTENT``) when there is nothing to do
  * ``400`` (``BAD REQUEST``) when missing ``version`` or ``coordinates``

* Example::

     # 1. create the upgrade plan
     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agents/versions" --data "agents=agent-1&agents=agent-4&version=3.1.0&coordinates=https://github.com/downloads/linkedin/glu/org.linkedin.glu.agent-server-upgrade-3.1.0.tgz"
     > Content-Type: application/x-www-form-urlencoded
     >
     < HTTP/1.1 201 Created
     < Location: http://localhost:8080/console/rest/v1/glu-dev-1/plan/49e69ef4-7c84-4f9b-9838-fce131b69028

     # 2. view the upgrade plan. You can then execute it...
     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/plan/49e69ef4-7c84-4f9b-9838-fce131b69028?prettyPrint=true"
     <?xml version="1.0"?>
     <plan fabric="glu-dev-1" id="49e69ef4-7c84-4f9b-9838-fce131b69028" origin="rest" action="upgradeAgents" version="3.1.0" name="origin=rest - action=upgradeAgents - version=3.1.0 - PARALLEL" savedTime="1312126425812">
       <parallel origin="rest" action="upgradeAgents" version="3.1.0">
         <sequential agent="agent-1" mountPoint="/self/upgrade">
           <leaf agent="agent-1" fabric="glu-dev-1" initParameters="{agentTar=https://github.com/downloads/linkedin/glu/org.linkedin.glu.agent-server-upgrade-3.1.0.tgz, newVersion=3.1.0}" mountPoint="/self/upgrade" name="Install script for [/self/upgrade] on [agent-1]" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
           <leaf agent="agent-1" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [install] phase for [/self/upgrade] on [agent-1]" scriptAction="install" toState="installed" />
           <leaf agent="agent-1" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [prepare] phase for [/self/upgrade] on [agent-1]" scriptAction="prepare" toState="prepared" />
           <leaf agent="agent-1" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [commit] phase for [/self/upgrade] on [agent-1]" scriptAction="commit" toState="upgraded" />
           <leaf agent="agent-1" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [uninstall] phase for [/self/upgrade] on [agent-1]" scriptAction="uninstall" toState="NONE" />
           <leaf agent="agent-1" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Uninstall script for [/self/upgrade] on [agent-1]" scriptLifecycle="uninstallScript" />
         </sequential>
         <sequential agent="agent-4" mountPoint="/self/upgrade">
           <leaf agent="agent-4" fabric="glu-dev-1" initParameters="{agentTar=https://github.com/downloads/linkedin/glu/org.linkedin.glu.agent-server-upgrade-3.1.0.tgz, newVersion=3.1.0}" mountPoint="/self/upgrade" name="Install script for [/self/upgrade] on [agent-4]" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
           <leaf agent="agent-4" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [install] phase for [/self/upgrade] on [agent-4]" scriptAction="install" toState="installed" />
           <leaf agent="agent-4" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [prepare] phase for [/self/upgrade] on [agent-4]" scriptAction="prepare" toState="prepared" />
           <leaf agent="agent-4" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [commit] phase for [/self/upgrade] on [agent-4]" scriptAction="commit" toState="upgraded" />
           <leaf agent="agent-4" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Run [uninstall] phase for [/self/upgrade] on [agent-4]" scriptAction="uninstall" toState="NONE" />
           <leaf agent="agent-4" fabric="glu-dev-1" mountPoint="/self/upgrade" name="Uninstall script for [/self/upgrade] on [agent-4]" scriptLifecycle="uninstallScript" />
         </sequential>
       </parallel>
     </plan>


.. _goe-rest-api-get-plans:

List all the plans
""""""""""""""""""

* Description: List all the plans (that have been created through ``POST /plans``).

* Request: ``GET /plans``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` => number of plans
    * body: json map where key is plan id and value is a link to view it

  * ``204`` (``NO CONTENT``) when there is no plan to list

* Example::

    curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/plans?prettyPrint=true"
    < HTTP/1.1 200 OK
    < X-glu-count: 2
    < Content-Type: text/json
    < Content-Length: 262
    {
      "03714d11-7b44-4717-b426-85d4cbf6c5d4": "http://localhost:8080/console/rest/v1/glu-dev-1/plan/03714d11-7b44-4717-b426-85d4cbf6c5d4",
      "b553f8de-62bc-4000-9c43-2fe869bdb3c4": "http://localhost:8080/console/rest/v1/glu-dev-1/plan/b553f8de-62bc-4000-9c43-2fe869bdb3c4"
    }

.. _goe-rest-api-post-plans:

Create deployment plan
""""""""""""""""""""""

* Description: Create a plan.

* Request: ``POST /plans``

  * view details :ref:`below <goe-rest-api-representing-a-plan>` for the content of body of the ``POST``

* Response: 

  * ``201`` (``CREATED``) with ``Location`` header to access the plan (``/plan/<planId>``)
  * ``204`` (``NO CONTENT``) when no plan created because there is nothing to do

* Example::

    curl -v -u "glua:password" --data "state=stopped&planType=transition" http://localhost:8080/console/rest/v1/glu-dev-1/plans
    > POST /console/rest/v1/glu-dev-1/plans HTTP/1.1
    > Authorization: Basic Z2x1YTpwYXNzd29yZA==
    > Content-Length: 33
    > Content-Type: application/x-www-form-urlencoded
    > 
    < HTTP/1.1 201 Created
    < Location: http://localhost:8080/console/rest/v1/glu-dev-1/plan/8283e25e-f68d-4bbd-8a71-5149f23466ec

.. _goe-rest-api-get-plan:

View a deployment plan
""""""""""""""""""""""

* Description: View the plan (as an xml document)

* Request: ``GET /plan``

  * N/A

* Response:

  * ``200`` (``OK``) with an xml representation of the plan
  * ``404`` (``NOT_FOUND``) if no such plan

.. _goe-rest-api-post-plan-execution:

Execute a deployment plan
"""""""""""""""""""""""""

* Description: Execute the plan.

* Request: ``POST /plan/<planId>/execution``

  * N/A

* Response:

  * ``201`` (``CREATED``) with ``Location`` header to access the plan execution (``/plan/<planId>/ execution/<executionId>``).                                         

    .. note:: it is a non blocking call and it returns right away and you can check the progress thus allowing to have a progress bar!

  * ``404`` (``NOT_FOUND``) if no such plan

.. _goe-rest-api-get-plan-executions:

List all the plan executions
""""""""""""""""""""""""""""

* Description: List all the plan excutions. Once a plan has been created (``POST /plans``) it is possible to execute it multiple times. This call allows you to list all the executions of a previously created plan.

* Request: ``GET /plan/<planId>/executions``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` => number of executions
    * body: json map where key is execution id and value is a link to view it

  * ``204`` (``NO CONTENT``) when no execution

* Example::

    curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/plan/03714d11-7b44-4717-b426-85d4cbf6c5d4/executions"
    < HTTP/1.1 200 OK
    < X-glu-count: 1
    < Content-Type: text/json
    < 
   {"3":"http://localhost:8080/console/rest/v1/glu-dev-1/plan/03714d11-7b44-4717-b426-85d4cbf6c5d4/execution/3"}

.. tip:: The execution id is in fact a deployment id and can be used directly in the ``/deployments*`` APIs

.. _goe-rest-api-head-plan-execution:

Check status of plan execution
""""""""""""""""""""""""""""""

* Description: Return the status of the execution.

* Request: ``HEAD /plan/<planId>/execution/<executionId>``

  * N/A

* Response:

  * ``200`` (``OK``) with ``X-glu-completion`` header with value:

    a. if plan non completed, percentage completion (ex: ``87``)

    b. if completed: ``100:<completion status>`` (ex: ``100:FAILED`` or ``100:COMPLETED``)

  * ``404`` (``NOT_FOUND``) if no such execution

.. _goe-rest-api-get-plan-execution:

View execution plan
"""""""""""""""""""

* Description: Return the execution as an xml document.

* Request: ``GET /plan/<planId>/execution/<executionId>``

  * N/A

* Response:

  * ``200`` (``OK``) with an xml representation of the execution (equivalent to the view in the console)
  * ``404`` (``NOT_FOUND``) if no such execution

.. _goe-rest-api-delete-plan-execution:

Abort execution plan
""""""""""""""""""""

* Description: Abort the execution.

* Request: ``DELETE /plan/<planId>/execution/<executionId>``

  * N/A

* Response:

  * TBD

.. _goe-rest-api-get-deployments-current:

List all current deployments
""""""""""""""""""""""""""""

* Description: List all current deployments.

* Request: ``GET /deployments/current``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` => number of current deployments
    * body: json map where key is deployment id and value is another map with some details about the deployment (equivalent to what you see on the `Plans` tab in the console)

  * ``204`` (``NO CONTENT``) when there is no deployment to list

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployments/current?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-count: 4
     < Content-Type: text/json
     {
       "1": {
         "completedSteps": 16,
         "description": "Deploy - Fabric [glu-dev-1] - PARALLEL",
         "endTime": 1312038165459,
         "startTime": 1312038160946,
         "status": "COMPLETED",
         "totalSteps": 16,
         "username": "glua",
         "viewURL": "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/current/1"
       },
       ...
     }

.. _goe-rest-api-delete-deployments-current:

Archive all current deployments
"""""""""""""""""""""""""""""""

* Description: Archive all current deployments.

* Request: ``DELETE /deployments/current``

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-archived`` => number of deployments that were archived

* Example::

     curl -v -X DELETE -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployments/current"
     < HTTP/1.1 200 OK
     < X-glu-archived: 3

.. _goe-rest-api-get-deployment-current:

View current deployment (details)
"""""""""""""""""""""""""""""""""

* Description: View the current deployment details

* Request: ``GET /deployment/current/<deploymentId>``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-startTime``, ``X-glu-endTime``, ``X-glu-username``, ``X-glu-status``, ``X-glu-description``, ``X-glu-completedSteps``, ``X-glu-totalSteps``
    * body: xml representation of the plan showing the details

  * ``404`` (``NOT FOUND``) when there is no such deployment

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/current/1?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-startTime: 1312038160946
     < X-glu-endTime: 1312038165459
     < X-glu-username: glua
     < X-glu-status: COMPLETED
     < X-glu-description: Deploy - Fabric [glu-dev-1] - PARALLEL
     < X-glu-completedSteps: 16
     < X-glu-totalSteps: 16
     < Content-Type: text/xml
     < 
     <?xml version="1.0"?>
     <plan fabric="glu-dev-1" systemId="47d37bb288b3908f2f3fe5d8b382053e7b13719b" id="f84e4f59-476d-4c17-bf3d-534b975cebf1" name="Deploy - Fabric [glu-dev-1] - PARALLEL">
       <parallel name="Deploy - Fabric [glu-dev-1] - PARALLEL">
         <sequential agent="agent-1" mountPoint="/m1/i001">
       ...
     </plan>

.. _goe-rest-api-head-deployment-current:

View current deployment (info)
""""""""""""""""""""""""""""""

* Description: View the current deployment info (just the headers).


* Request: ``HEAD /deployment/current/<deploymentId>``

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-startTime``, ``X-glu-endTime``, ``X-glu-username``, ``X-glu-status``, ``X-glu-description``, ``X-glu-completedSteps``, ``X-glu-totalSteps``

  * ``404`` (``NOT FOUND``) when there is no such deployment

* Example::

     curl -v --head -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/current/1"
     < HTTP/1.1 200 OK
     < X-glu-startTime: 1312038160946
     < X-glu-endTime: 1312038165459
     < X-glu-username: glua
     < X-glu-status: COMPLETED
     < X-glu-description: Deploy - Fabric [glu-dev-1] - PARALLEL
     < X-glu-completedSteps: 16
     < X-glu-totalSteps: 16

.. _goe-rest-api-delete-deployment-current:

Archive current deployment
""""""""""""""""""""""""""

* Description: Archive a single current deployment.

* Request: ``DELETE /deployment/current/<deploymentId>``

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-archived`` => ``true`` if the deployment was archived, ``false`` if already archived

* Example::

     curl -v -X DELETE -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/current/1"
     < HTTP/1.1 200 OK
     < X-glu-archived: true

.. _goe-rest-api-head-deployments-archived:

Archived deployments count
""""""""""""""""""""""""""

* Description: Return the total count of archived deployments.


* Request: ``HEAD /deployments/archived``

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-totalCount``

* Example::

     curl -v --head -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployments/archived"
     < HTTP/1.1 200 OK
     < X-glu-totalCount: 4

.. _goe-rest-api-get-deployments-archived:

List archived deployments (paginated)
"""""""""""""""""""""""""""""""""""""

* Description: List archived deployments according to the parameters provided to determine which `page` to return.

* Request: ``GET /deployments/archived``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output
  * ``max=xxx`` how many entries to return max (max cannot exceed a limit which defaults to 25 which is also the value used if not provided)
  * ``offset=xxx`` which entry to start (default is 0)

    .. note:: ``offset`` represents the index in the list (not a deployment id!). To go from page to page, the offset simply increments by ``max``. Example with ``max=10``, ``offset=0`` will return page 1, ``offset=10`` will return page 2, etc...

  * ``sort`` which `column` to sort on (default is ``startDate``) 
  * ``order`` which order to sort the list (default is ``desc``)

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-max``, ``X-glu-offset``, ``X-glu-sort``, ``X-glu-order``, which are the values provided/defaulted/adjusted from the request and ``X-glu-count`` which is the number of entries returned and ``X-glu-totalCount`` which is the total number of archived deployments
    * body: json map where key is deployment id and value is another map with some details about the deployment (equivalent to what you see on the `Plans/Archived` tab in the console)

  * ``204`` (``NO CONTENT``) when there is no deployment to list

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployments/archived?prettyPrint=true&max=2"
     < HTTP/1.1 200 OK
     < X-glu-count: 2
     < X-glu-totalCount: 4
     < X-glu-max: 2
     < X-glu-offset: 0
     < X-glu-sort: startDate
     < X-glu-order: desc
     < Content-Type: text/json
     {
       "3": {
         "description": "origin=rest - action=bounce - filter=all - SEQUENTIAL",
         "endTime": 1312039222813,
         "startTime": 1312039220934,
         "status": "COMPLETED",
         "username": "glua",
         "viewURL": "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/archived/3"
       },
       "4": {
         "description": "origin=rest - action=bounce - filter=all - PARALLEL",
         "endTime": 1312039238723,
         "startTime": 1312039237598,
         "status": "COMPLETED",
         "username": "glua",
         "viewURL": "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/archived/4"
       }
     }

.. _goe-rest-api-get-deployment-archived:

View archived deployment (details)
""""""""""""""""""""""""""""""""""

* Description: View the archived deployment details

* Request: ``GET /deployment/archived/<deploymentId>``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-startTime``, ``X-glu-endTime``, ``X-glu-username``, ``X-glu-status``, ``X-glu-description``
    * body: xml representation of the plan showing the details

  * ``404`` (``NOT FOUND``) when there is no such deployment

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/archived/1?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-startTime: 1312038160946
     < X-glu-endTime: 1312038165459
     < X-glu-username: glua
     < X-glu-status: COMPLETED
     < X-glu-description: Deploy - Fabric [glu-dev-1] - PARALLEL
    < Content-Type: text/xml
     < 
     <?xml version="1.0"?>
     <plan fabric="glu-dev-1" systemId="47d37bb288b3908f2f3fe5d8b382053e7b13719b" id="f84e4f59-476d-4c17-bf3d-534b975cebf1" name="Deploy - Fabric [glu-dev-1] - PARALLEL">
       <parallel name="Deploy - Fabric [glu-dev-1] - PARALLEL">
         <sequential agent="agent-1" mountPoint="/m1/i001">
       ...
     </plan>

.. _goe-rest-api-head-deployment-archived:

View archived deployment (info)
"""""""""""""""""""""""""""""""

* Description: View the archived deployment info (just the headers).


* Request: ``HEAD /deployment/archived/<deploymentId>``

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-startTime``, ``X-glu-endTime``, ``X-glu-username``, ``X-glu-status``, ``X-glu-description``

  * ``404`` (``NOT FOUND``) when there is no such deployment

* Example::

     curl -v --head -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/deployment/archived/1"
     < HTTP/1.1 200 OK
     < X-glu-startTime: 1312038160946
     < X-glu-endTime: 1312038165459
     < X-glu-username: glua
     < X-glu-status: COMPLETED
     < X-glu-description: Deploy - Fabric [glu-dev-1] - PARALLEL

.. _goe-rest-api-post-model-static:

Load static model
"""""""""""""""""

* Description: Load the (desired) model in the console.

* Request: ``POST /model/static``

  Body can be of 2 types depending on the ``Content-Type`` header:

  1. ``application/x-www-form-urlencoded`` then body should contain ``modelUrl=xxx`` with the url pointing to the model (the console will 'download' it)
  2. ``text/json`` or ``text/json+groovy`` then body should be the model itself (`example <https://gist.github.com/755981>`_)

* Response:

  * ``201`` (``CREATED``) when loaded successfully
  * ``204`` (``NO_CONTENT``) if model was loaded successfully and is equal to the previous one
  * ``400`` (``BAD_REQUEST``) if the model is not valid (should be a properly json formatted document)
  * ``404`` (``NOT_FOUND``) when error (note error handling needs to be revisited)

* Example::

    # 1. using modelUrl
    curl -v -u "glua:password" --data "modelUrl=file%3A%2FUsers%2Fypujante%2Fgithub%2Forg.pongasoft%2Fglu%2Fconsole%2Forg.linkedin.glu.console-server%2Fsrc%2Fcmdline%2Fresources%2Fglu%2Frepository%2Fsystems%2Fhello-world-system.json" http://localhost:8080/console/rest/v1/glu-dev-1/model/static
    > POST /console/rest/v1/glu-dev-1/model/static HTTP/1.1
    > Content-Type: application/x-www-form-urlencoded
    > ...
    >
    < HTTP/1.1 201 Created
    < ...
    id=facc4ef65539a5c558436f034b5e63e5ba1fd0ef

    # 2. using input stream
    curl -v -u "glua:password" -H "Content-Type: text/json" --data-binary @/Users/ypujante/github/org.pongasoft/glu/console/org.linkedin.glu.console-server/src/cmdline/resources/glu/repository/systems/hello-world-system.json http://localhost:8080/console/rest/v1/glu-dev-1/model/static
    > POST /console/rest/v1/glu-dev-1/model/static HTTP/1.1
    > Content-Type: text/json
    > ...
    >
    < HTTP/1.1 201 Created
    < ...
    id=facc4ef65539a5c558436f034b5e63e5ba1fd0ef

.. _goe-rest-api-get-model-static:

View static model
"""""""""""""""""

* Description: Retrieve the current loaded model (aka *expected* state).

  .. note:: this is what you loaded using ``POST /model/static``

* Request: ``GET /model/static``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output
  * ``systemFilter=...`` for filtering (see :ref:`goe-filter-syntax` for the syntax)

* Response:  

  * ``200`` (``OK``) with a json representation of the model

.. _goe-rest-api-get-model-live:

View live model
"""""""""""""""

* Description: Retrieve the current live model coming from ZooKeeper (aka *current* state).

* Request: ``GET /model/live``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output
  * ``systemFilter=...`` for filtering (see :ref:`goe-filter-syntax` for the syntax)

* Response:

  * ``200`` (``OK``) with a json representation of the live model

    .. note:: the metadata contains information like ``currentState``

.. _goe-rest-api-get-model-delta:

View delta
""""""""""

* Description: Retrieve the delta (similar to what can be seen on the dashboard) which is the difference between the static and live model (expected vs current)

* Request: ``GET /model/delta``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output
  * ``systemFilter=...`` for filtering (see :ref:`goe-filter-syntax` for the syntax)
  * ``errorsOnly=true`` filter to show only errors 
  * ``flatten=true`` flatten the output (similar to dashboard view) 

* Response:

  * ``200`` (``OK``) with a :ref:`json representation <goe-rest-api-delta-formal-definition>` of the delta

.. _goe-rest-api-get-fabrics:

List all the fabrics
""""""""""""""""""""

* Description: List all the fabrics

* Request: ``GET /``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` with the number of fabrics
    * body: json array of fabric names

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/-?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-count: 2
     < Content-Type: text/json
     [
       "glu-dev-1",
       "glu-dev-2"
     ]

.. _goe-rest-api-get-agents-fabrics:

List all the agents fabrics
"""""""""""""""""""""""""""

* Description: Returns the map of association agent -> fabric

* Request: ``GET /agents``

  optional request parameters:

  * ``prettyPrint=true`` for human readable output

* Response: 

  * ``200`` (``OK``) with:

    * headers: ``X-glu-count`` with the number of entries in the map
    * body: json map where the key is agent name and the value is the fabric

* Example::

     curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/-/agents?prettyPrint=true"
     < HTTP/1.1 200 OK
     < X-glu-count: 2
     < Content-Type: text/json
     {
       "agent-1": "glu-dev-1",
       "xeon.local": "glu-dev-1"
     }

.. warning:: This api (``/rest/v1/-/agents``) which returns a map of association agent -> fabric should not be mistaken with the api ``/rest/v1/<fabric>/agents`` which specifies the fabric and list all agents in a given fabric!

.. _goe-rest-api-post-command-execute:

Execute a shell command
"""""""""""""""""""""""

* Description: Execute a shell command on the agent. Note that this call is non blocking and return right away. In order to wait for the command to complete and read the result of the command, you need to use the :ref:`other api <goe-rest-api-get-command-streams>`

* Request: ``POST /agent/<agentName>/commands``

  required request parameters:

  * ``command=...``: the command to execute (ex: ``uptime``)

  optional request parameters:

  * ``type=shell`` the type of command to run (at this moment only ``shell`` is supported)
  * ``redirectStderr=true`` whether stderr should be redirected to stdout (default to ``false``)

  optional input stream: if you want to provide ``stdin`` to your command, simply put it in the body of the post

* Response: 

  * ``201`` (``Created``) with:

    * headers: ``X-glu-command-id`` the id of the command created
    * body: json map containing the same id

  * ``404`` (``Not found``) if no such agent

* Example::

      curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agent/agent-1/commands?command=cat%20-" -H "Content-Type: application/octet-stream" --data-binary 'abcdef'
      > POST /console/rest/v1/glu-dev-1/agent/agent-1/commands?command=cat%20- HTTP/1.1
      > ...
      > Content-Type: application/octet-stream
      > Content-Length: 6
      > 
      * upload completely sent off: 6 out of 6 bytes
      < HTTP/1.1 201 Created
      < ...
      < X-glu-command-id: 13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536
      < Content-Type: text/json
      < Content-Length: 57
      < 
      {"id":"13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536"}

.. _goe-rest-api-get-command-streams:

Read/Wait for command result
""""""""""""""""""""""""""""

* Once a command has been started, this api will retrieve the result(s) and optionally wait. If you request more than one stream, then you will get a muliplexed stream of results (the format is fairly simple and you can take a look at the java code on how to demultiplex it. TODO: add python code for demultiplexing/example).

* Request: ``GET /commands/<commandId>/streams``

  optional request parameters:

  * ``stdoutStream=true`` include stdout stream (default: ``false``)
  * ``stdoutOffset=...`` where to start in stdout stream in number of bytes (negative number counts backward) (default: ``0``)
  * ``stdoutLen=...`` how many bytes max to read for stdout (default: ``-1`` => read all)
  * ``stderrStream=true`` include stderr stream (default: ``false``)
  * ``stderrOffset=...`` where to start in stderr stream in number of bytes (negative number counts backward) (default: ``0``)
  * ``stderrLen=...`` how many bytes max to read for stderr (default: ``-1`` => read all)
  * ``stdinStream=true`` include stdin stream (default: ``false``)
  * ``stdinOffset=...`` where to start in stdin stream in number of bytes (negative number counts backward) (default: ``0``)
  * ``stdinLen=...`` how many bytes max to read for stdin (default: ``-1`` => read all)
  * ``exitValueStream=true`` include the exit value (which is what the shell command returned: ``$?``) (default: ``false``)
  * ``exitValueStreamTimeout=...`` if the command is not completed yet, how long to wait (meaning block) until the result is returned (0 means wait until completed). Can be any timespan (ex: ``5s``, ``10m``, ``50`` for 50 milliseconds) (default: don't wait!)
  * ``exitErrorStream=true`` include the error stream (when an exception is generated for any reason, usually communication) (default: ``false``)

* Response: 

  * ``200`` (``OK``) with:

    * headers: 

      * ``X-glu-command-id`` the id of the command 
      * ``X-glu-command-startTime`` the time when the command was started 
      * ``X-glu-command-completionTime`` the time when the command was completed (if completed) 

    * body: the stream(s) requested

  * ``404`` (``Not found``) if no such command

* Example::

      curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/command/13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536/streams?stdoutStream=true"
      > GET /console/rest/v1/glu-dev-1/command/13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536/streams?stdoutStream=true HTTP/1.1
      > ...
      > 
      < HTTP/1.1 200 OK
      < X-glu-command-id: 13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536
      < X-glu-command-startTime: 1353009232768
      < X-glu-command-completionTime: 1353009232917
      < Content-Type: application/octet-stream
      < ...
      < 
      abcdef

      curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/command/13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536/streams?stdoutStream=true&exitValueStream=true"
      > GET /console/rest/v1/glu-dev-1/command/13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536/streams?stdoutStream=true&exitValueStream=true HTTP/1.1
      > ...
      > 
      < HTTP/1.1 200 OK
      < X-glu-command-id: 13b05a27b80-d527ccf3-166a-4986-90b2-41be8c2ba536
      < X-glu-command-startTime: 1353009232768
      < X-glu-command-completionTime: 1353009232917
      < Content-Type: application/octet-stream
      < ...
      < 
      MISV1.0=V=O

      V=1
      0

      O=6
      abcdef



API Examples
^^^^^^^^^^^^
* Sending the model to glu in `java <https://gist.github.com/756465>`_
* `Python example <https://github.com/pongasoft/glu/blob/REL_1.5.0/console/org.linkedin.glu.console-cli/src/cmdline/resources/lib/python/gluconsole/rest.py>`_ (part of the cli)

.. _goe-rest-api-delta-formal-definition:

Delta Formal Definition
-----------------------

TBD: add formal delta definition

.. note:: In the meantime, you may want to check this `forum thread <http://glu.977617.n3.nabble.com/formal-definition-of-a-delta-td4024766.html>`_

.. _goe-rest-api-representing-a-plan:

Representing a plan
-------------------

Query String based
^^^^^^^^^^^^^^^^^^
The ``POST`` you issue must be of Content-Type ``application/x-www-form-urlencoded``

The body then contains a query string with the following parameters:

+--------------------------+----------------------+--------------------+-----------------------------------------------------+
|name                      |value                 |required            |example                                              |
+--------------------------+----------------------+--------------------+-----------------------------------------------------+
|``planAction``            |``start``, ``stop``,  |One of              |``planAction=start``                                 |
|                          |``bounce``,           |``planAction`` or   |                                                     |
|                          |``deploy``,           |``planType``        |                                                     |
|                          |``undeploy``,         |                    |                                                     |
|                          |``redeploy``          |                    |                                                     |
+--------------------------+----------------------+--------------------+-----------------------------------------------------+
|``planType``              |``deploy``,           |One of              |``planType=transition&state=stopped``                |
|                          |``undeploy``,         |``planAction`` or   |                                                     |
|                          |``redeploy``,         |``planType``        |                                                     |
|                          |``bounce``,           |                    |                                                     |
|                          |``transition`` or     |                    |                                                     |
|                          |anything custom       |                    |                                                     |
|                          |                      |                    |                                                     |
+--------------------------+----------------------+--------------------+-----------------------------------------------------+
|``systemFilter``          |a system filter as    |No. It simply means |``systemFilter=and%7bagent%3d'ei2-app3-zone5.qa'%7d``|
|                          |described in the      |don't filter at all.|                                                     |
|                          |previous section      |                    |                                                     |
|                          |(remember that it     |                    |                                                     |
|                          |**must** be properly  |                    |                                                     |
|                          |url encoded.          |                    |                                                     |
|                          |                      |                    |                                                     |
+--------------------------+----------------------+--------------------+-----------------------------------------------------+
|``order``                 |``parallel`` or       |No. Default to      |``order=parallel``                                   |
|                          |``sequential``        |``sequential``      |                                                     |
+--------------------------+----------------------+--------------------+-----------------------------------------------------+
|``maxParallelStepsCount`` |a positive integer    |No.                 |``maxParallelStepsCount=2`` defines parallel plan    |
|                          |                      |                    |with no more than 2 steps in parallel                |
+--------------------------+----------------------+--------------------+-----------------------------------------------------+

.. note:: ``planAction=stop`` is equivalent to ``planType=transition&state=stopped``. The ``planType`` notation is more generic and should be used when using your own state machine or creating your own custom plan types (in which case you can also pass as many parameters as you want).

Json/DSL Based
--------------
Coming soon

.. _goe-plugins:

Plugins
-------

The orchestration engine offers a plugin mechanism which allows you to hook into some specific phases to tweak and/or change the behavior. For example, the ``UserService_pre_authenticate`` hook allows you to bypass entirely the login/authentication mechanism that comes built-in and instead provide your own.

What is a plugin?
^^^^^^^^^^^^^^^^^

A plugin is simply a class with (groovy) closures, each of them defining a specific hook. You can define as many or as little of those closures as you want and you can *spread* them accross different classes (for example, you can have a plugin class handling only the *deployment* related plugin hooks, and another handling the hooks for a different part of the system). If a closure is missing, then it will simply not be called.

.. tip:: The class `DocumentationPlugin <https://github.com/pongasoft/glu/tree/master/orchestration/org.linkedin.glu.orchestration-engine/src/main/groovy/org/linkedin/glu/orchestration/engine/plugins/builtin/DocumentationPlugin.groovy>`_ is meant to be an hexaustive list of all the hooks available as well as a documentation on how to use them. It also serves as an example of how to write a plugin!

How do you install a plugin?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In order to install your plugins(s), you need to first package them as jar file(s). Then it is just a matter of editing the appropriate section in the :ref:`meta model <meta-model-console>` to let glu know where they are. For example::

  consoles << [
    host: 'localhost',
    plugins: [
      [
        fqcn: 'org.acme.glu.plugin.MyPlugin', 
        classPath: ['http://repository/myplugin.jar', 'http://repository/dependency.jar']
      ],
      // ... more plugins
    ],
  ]

* ``fqcn`` is the fully qualified class name of your plugin

  .. note::
     The class of your plugin **must** be present in one of the jar files specified in the ``classPath`` section

* ``classPath`` is a list of URIs containing the various jars comprising your plugin (base class + dependencies).

List of hooks available
^^^^^^^^^^^^^^^^^^^^^^^

.. tip:: For full documentation and typical use cases, refer to the class `DocumentationPlugin <https://github.com/pongasoft/glu/tree/master/orchestration/org.linkedin.glu.orchestration-engine/src/main/groovy/org/linkedin/glu/orchestration/engine/plugins/builtin/DocumentationPlugin.groovy>`_.

* Initialization

  * ``PluginService_initialize``: called for initializing your plugin

* User management

  * ``UserService_pre_authenticate``: called before authentication
  * ``UserService_post_authenticate``: called after authentication
  * ``UserService_pre_authorize``: called before authorization
  * ``UserService_post_authorize``: called after authorization
  * ``UserService_pre_restAuthenticateAndAuthorize``: called before REST authentication and authorization flow
  * ``UserService_post_restAuthenticateAndAuthorize``: called after REST authentication and authorization flow

* Model

  * ``SystemService_pre_parseSystemModel``: called before parsing the system model
  * ``SystemService_post_parseSystemModel``: called after parsing the system model

* Plans

  * ``PlannerService_pre_computePlans``: called before computing a deployment plan
  * ``PlannerService_post_computePlans``: called after computing a deployment plan

* Deployment

  * ``DeploymentService_pre_executeDeploymentPlan``: called before executing a deployment plan
  * ``DeploymentService_onStart_executeDeploymentPlan``: called when the deployment plan starts executing
  * ``DeploymentService_post_executeDeploymentPlan``: called after executing a deployment plan  

* Commands

  * ``CommandsService_pre_executeCommand``: called before executing a command
  * ``CommandsService_post_executeCommand``: called when the command is complete
  * ``FileSystemCommandExecutionIOStorage_createInputStream``: called to create the input stream to read the IO of the command from the filesystem
  * ``FileSystemCommandExecutionIOStorage_createOutputStream``:called to create the output stream to write the IO of the command from the filesystem

Future hooks
^^^^^^^^^^^^

Now that the plugin mechanism is in place, adding new hooks is not very complicated. If you have specific requirements for a new hook, please open a ticket on the `glu ticketing system <https://github.com/pongasoft/glu/issues>`_.

.. _goe-cli:


Command Line
------------
The orchestration engine also comes with a cli which is essentially a wrapper around the REST api which can be used in several ways:

* directly to issue commands
* to build higher level cli(s) which use this cli as a base
* as an example for how to talk to the REST api directly

The cli currently requires python 2.6 to run.

Help
^^^^
This is the output when using the ``-h`` option

Usage::

 console-cli.py -f <fabric> <start|stop|bounce|deploy|undeploy|redeploy|load|status> [flags]

.. note:: The orchestration engine cli is currently called ``console-cli`` because of the fact that the orchestration engine is currently running inside the console.

Options:

.. option:: --version

   show program's version number and exit

.. option:: -h, --help

   show this help message and exit

.. option:: -d, --debug

   Turn on debug output

.. option:: -c CONSOLEURL, --console=CONSOLEURL

   Url to glu Console for the given fabric.

.. option:: -f FABRIC, --fabric=FABRIC

   Perform action on a fabric

.. option:: -u USER, --user=USER

   glu user to use for authentication, defaults to ``$USER``

.. option:: -x PASSWORD, --xpassword=PASSWORD

   Password. Warning password will appear in clear in ``ps`` output. Use only for testing.

.. option:: -X PASSWORDFILE, --xpasswordfile=PASSWORDFILE

   Read user password from passwordfile specified. Make sure to protect this file using unix permissions

.. option:: -a, --all 

   Perform action on all entries

.. option:: -A AGENT, --agent=AGENT

   Perform action on one or more agent(s)

.. option:: -t ALLTAGS, --allTags=ALLTAGS

   Shortcut for querying by tags (all tags must be present): frontend;backend

.. option:: -T ANYTAG, --anyTag=ANYTAG 

   Shortcut for querying by tags (any of the tags need to be present): frontend;backend

.. option:: -I INSTANCE, --instance=INSTANCE

   Perform action on one or more instance(s)

.. option:: -p, --parallel

   Perform action on all instances in parallel. Default is serial.

.. option:: -n, --dryrun

   Do a dry run of your plan. No changes will be made. Default is false.

.. option:: -s FILTER, --systemFilter=FILTER

   Filter in DSL sytax for filtering the model. Applicable only with 'status' command. See :ref:`goe-filter-syntax`.

.. option:: -S FILTERFILE, --systemFilterFile=FILTERFILE

   Filter file with filters in DSL sytax for filtering the model. Applicable only with 'status' command. See :ref:`goe-filter-syntax`.

.. option:: -m MODEL, --model=MODEL

   Loads the model pointed to by model (should be a url!)

.. option:: -M MODELFILE, --modelFile=MODELFILE

   Loads the model from the file

.. option:: -l, --live

   Show current model instead of expected model. Applicable only with 'status' command.

.. option:: -b, --beautify

   Pretty print the model.

Unless you run the cli on the same host as the console you will need to specify its location:

    Example::

       -c https://glu.console.host:8443/console
    
Here are some examples of usage::

    # View the current loaded model (and format it for readability):
    ./bin/console-cli.py -f glu-dev-1 -u admin -x admin -b status
    
    # View the live model only for applications in cluster cluster-1 (and format it for readability):
    ./bin/console-cli.py -f glu-dev-1 -u admin -x admin -b -l -s "metadata.cluster='cluster-1'" status
    
    # Show me what will happen if I bounce all applcations in cluster cluster-1 (but don't do it!):
    ./bin/console-cli.py -f glu-dev-1 -u admin -x admin -s "metadata.cluster='cluster-1'" -n bounce

    # Bounce all applcations in cluster cluster-1:
    ./bin/console-cli.py -f glu-dev-1 -u admin -x admin -s "metadata.cluster='cluster-1'" bounce
    
