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

API
^^^
Main URI: ``/console/rest/v1/<fabric>`` (all the URIs in the following tables starts with the main URI)

+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|Method     |URI                                        |Description         |Request                                             |Response                                                    |
+===========+===========================================+====================+====================================================+============================================================+
|``GET``    |``/plans``                                 |List all the plans  |N/A                                                 |TBD                                                         |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``POST``   |``/plans``                                 |Create a plan       |view details below for the content of body of the   |* ``201`` (``CREATED``) with ``Location`` header to access  |
|           |                                           |                    |``POST``                                            |  the plan (``/plan/ <planId>``)                            |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |                                                    |* ``204`` (``NO CONTENT``) when no plan created because     |
|           |                                           |                    |                                                    |  there is nothing to do                                    |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``GET``    |``/plan/<planId>``                         |View the plan (as an|N/A                                                 |* ``200`` (``OK``) with an xml representation of the plan   |
|           |                                           |xml document)       |                                                    |                                                            |
|           |                                           |                    |                                                    |* ``404`` (``NOT_FOUND``) if no such plan                   |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``POST``   |``/plan/<planId>/execution``               |Executes the plan   |N/A                                                 |* ``201`` (``CREATED``) with ``Location`` header to access  |
|           |                                           |                    |                                                    |  the plan execution (``/plan/ <planId>/ execution/         |
|           |                                           |                    |                                                    |  <executionId>``).                                         |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |                                                    |.. note:: it is a non blocking call and it returns right    |
|           |                                           |                    |                                                    |    away and you can check the progress thus allowing to    |
|           |                                           |                    |                                                    |    have a progress bar!                                    |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |                                                    |* ``404`` (``NOT_FOUND``) if no such plan                   |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``HEAD``   |``/plan/<planId>/execution/<executionId>`` |Returns the status  |N/A                                                 |* ``200`` (``OK``) with ``X-LinkedIn-GLU-Completion`` header|
|           |                                           |of the execution    |                                                    |  with value:                                               |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |                                                    |  a. if plan non completed, percentage completion (ex:      |
|           |                                           |                    |                                                    |  ``87``)                                                   |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |                                                    |  b. if completed: ``100:<completion status>`` (ex:         |
|           |                                           |                    |                                                    |  ``100:FAILED`` or ``100:COMPLETED``)                      |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |                                                    |* ``404`` (``NOT_FOUND``) if no such execution              |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``GET``    |``/plan/<planId>/execution/<executionId>`` |Returns the         |N/A                                                 |* ``200`` (``OK``) with an xml representation of the        |
|           |                                           |execution as an xml |                                                    |  execution (equivalent to the view in the console)         |
|           |                                           |document            |                                                    |                                                            |
|           |                                           |                    |                                                    |* ``404`` (``NOT_FOUND``) if no such execution              |
|           |                                           |                    |                                                    |                                                            |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``DELETE`` |``/plan/<planId>/execution/<executionId>`` |Aborts the execution|N/A                                                 |TBD                                                         |
|           |                                           |                    |                                                    |                                                            |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``POST``   |``/system/model``                          |Loads the (desired) |Body can be of 2 types depending on the             |* ``201`` (``CREATED``) when loaded successfully            |
|           |                                           |model in the console|``Content-Type`` header:                            |                                                            |
|           |                                           |                    |                                                    |* ``204`` (``NO_CONTENT``) if model was loaded successfully |
|           |                                           |                    |``application/x-www-form-urlencoded`` then body     |  and is equal to the previous one                          |
|           |                                           |                    |should contain ``modelUrl=xxx`` with the url        |                                                            |
|           |                                           |                    |pointing to the model (the console will 'download'  |* ``400`` (``BAD_REQUEST``) if the model is not valid       |
|           |                                           |                    |it)                                                 |  (should be a properly json formatted document)            |
|           |                                           |                    |                                                    |                                                            |
|           |                                           |                    |``text/json`` then body should be the model itself  |* ``404`` (``NOT_FOUND``) when error (note error handling   |
|           |                                           |                    |(`example <https://gist.github.com/755981>`_)       |  needs to be revisited)                                    |
|           |                                           |                    |                                                    |                                                            |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``GET``    |``/system/model``                          |Retrieves the       |optional request parameters:                        |* ``200`` (``OK``) with a json representation of the model  |
|           |                                           |current loaded model|                                                    |                                                            |
|           |                                           |(aka 'desired'      |``prettyPrint=true`` for human readable output      |                                                            |
|           |                                           |state)              |                                                    |                                                            |
|           |                                           |                    |``systemFilter=...`` for filtering (see             |                                                            |
|           |                                           |                    |:ref:`goe-filter-syntax` for the syntax)            |                                                            |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+
|``GET``    |``/system/live``                           |Retrieves the       |optional request parameters:                        |* ``200`` (``OK``) with a json representation of the live   |
|           |                                           |current live model  |                                                    |  model                                                     |
|           |                                           |coming from         |``prettyPrint=true`` for human readable output      |                                                            |
|           |                                           |ZooKeeper (aka      |                                                    |.. note:: the metadata contains information like            |
|           |                                           |current state)      |``systemFilter=...`` for filtering (see             |   ``currentState``                                         |
|           |                                           |                    |:ref:`goe-filter-syntax` for the syntax)            |                                                            |
+-----------+-------------------------------------------+--------------------+----------------------------------------------------+------------------------------------------------------------+

API Examples
^^^^^^^^^^^^
* Sending the model to glu in `java <https://gist.github.com/756465>`_
* `Python example <https://github.com/linkedin/glu/blob/REL_1.5.0/console/org.linkedin.glu.console-cli/src/cmdline/resources/lib/python/gluconsole/rest.py>`_ (part of the cli)

Representing a plan
-------------------

Query String based
^^^^^^^^^^^^^^^^^^
The ``POST`` you issue must be of Content-Type ``application/x-www-form-urlencoded``

The body then contains a query string with the following parameters:

+--------------------+----------------------+--------------------+-----------------------------------------------------+
|name                |value                 |required            |example                                              |
+--------------------+----------------------+--------------------+-----------------------------------------------------+
|``planAction``      |``start``, ``stop``,  |Yes                 |``planAction=start``                                 |
|                    |``bounce``,           |                    |                                                     |
|                    |``deploy``,           |                    |                                                     |
|                    |``undeploy``,         |                    |                                                     |
|                    |``redeploy``          |                    |                                                     |
+--------------------+----------------------+--------------------+-----------------------------------------------------+
|``systemFilter``    |a system filter as    |No. It simply means |``systemFilter=and%7bagent%3d'ei2-app3-zone5.qa'%7d``|
|                    |described in the      |don't filter at all.|                                                     |
|                    |previous section      |                    |                                                     |
|                    |(remember that it     |                    |                                                     |
|                    |**must** be properly  |                    |                                                     |
|                    |url encoded.          |                    |                                                     |
|                    |                      |                    |                                                     |
+--------------------+----------------------+--------------------+-----------------------------------------------------+
|``order``           |``parallel`` or       |No. Default to      |``order=parallel``                                   |
|                    |``sequential``        |``sequential``      |                                                     |
+--------------------+----------------------+--------------------+-----------------------------------------------------+

Json/DSL Based
--------------
Coming soon

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
    
