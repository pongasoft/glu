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

.. |script-logo| image:: /images/script-logo-86.png
   :alt: script logo
   :class: header-logo

|script-logo| glu script
========================
A glu script is a set of instructions backed by a state machine that the agent knows how to run. In general, and by default, a glu script represents the set of instructions which defines the lifecycle of what it means to ``install``, ``configure``, ``start``, ``stop``, ``unconfigure`` and ``uninstall`` an application.

Groovy Class
------------

.. image:: /images/MyGluScript.png
   :align: center
   :alt: MyGluScript.groovy

A glu script is a groovy class which contains a set of closures where the name of each :term:`closure` matches the name of the actions defined in the state machine. This example shows the default closure names. The script can also store state in attributes (like ``port`` and ``pid`` in this example). 

.. tip:: The code of each closure can be any arbitrary groovy/java code but remember that the agent offers :ref:`some capabilities <agent-capabitites>` to help you in writing more concise code.

.. _glu-script-state-machine:

State machine
-------------
Each glu script is backed by a state machine which is an instance of ``org.linkedin.groovy.util.state.StateMachine`` (`StateMachine api <https://github.com/linkedin/linkedin-utils/blob/master/org.linkedin.util-groovy/src/main/groovy/org/linkedin/groovy/util/state/StateMachine.groovy>`_). The default state machine is the following:

.. image:: /images/state_machine_diagram.png
   :align: center
   :width: 800
   :height: 213
   :scale: 85
   :alt: State Machine diagram

This is how the default state machine is defined.

.. image:: /images/state_machine.png
   :align: center
   :width: 977
   :height: 151
   :scale: 70
   :alt: State Machine Definition

You can define your own state machine by defining a static attribute called ``stateMachine``

This is how the ``AutoUpgradeScript`` (`AutoUpgradeScript source <https://github.com/linkedin/glu/blob/master/agent/org.linkedin.glu.agent-impl/src/main/groovy/org/linkedin/glu/agent/impl/script/AutoUpgradeScript.groovy>`_) glu script defines different states and actions::

    class AutoUpgradeScript {
      def static stateMachine =
      [
          NONE: [ [to: 'installed', action: 'install'] ],
          installed: [ [to: 'NONE', action: 'uninstall'], [to: 'prepared', action: 'prepare'] ],
          prepared: [ [to: 'upgraded', action: 'commit'], [to: 'installed', action: 'rollback'] ],
          upgraded: [ [to: 'NONE', action: 'uninstall'] ]
      ]
      ...
   }

The minimum (usefull) state machine that you can define could look like::

    def static stateMachine =
    [
        NONE: [ [to: 'running', action: 'start'] ],
        running: [ [to: 'NONE', action: 'stop'] ]
    ]

.. note:: If an action is empty you don't even have to define its equivalent action but you still need to call all prior actions to satisfy the state machine.

Capabilities
------------
As described in the section :ref:`agent-capabitites`, a glu script can use all the capabilities provided by the agent.

.. tip:: 
   Implicitely (at runtime), all glu scripts implement the `GluScript <https://github.com/linkedin/glu/blob/master/agent/org.linkedin.glu.agent-impl/src/main/groovy/org/linkedin/glu/agent/impl/GluScript.groovy>`_ interface.

Table of all the properties usable from a ``GluScript``:

+---------------------------------------------------+---------------------------------------------------------+
|Name                                               |Usage                                                    |
+===================================================+=========================================================+
|:ref:`children <agent-capabilities-children>`      |Access to the children of this glu script                |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`log <agent-capabilities-log>`                |Write log messages in agent log file                     |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`mountPoint <agent-capabilities-mountPoint>`  |The mountPoint on which this script was *mounted*        |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`params <agent-capabilities-params>`          |Access to the model :ref:`initParameters                 |
|                                                   |<static-model-entries-initParameters>` section           |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`parent <agent-capabilities-parent>`          |Access to the parent glu script                          |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`shell <agent-capabilities-shell>`            |Access to all shell like capabilities (mv, ls, etc...)   |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`shell.env <agent-capabilities-shell-env>`    |Access to environment variables set at agent boot time   |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`stateManager                                 |Manage/Query the state                                   |
|<agent-capabilities-stateManager>`                 |                                                         |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`state <agent-capabilities-state>`            |Shortcut to current state                                |
+---------------------------------------------------+---------------------------------------------------------+
|:ref:`timers <agent-capabilities-timers>`          |Schedule/Cancel timers                                   |
+---------------------------------------------------+---------------------------------------------------------+


Conventions
-----------

Logs
^^^^
In order to be able to see (in the console) log files produced by an application deployed by the glu script, you can follow the convention described in the ":ref:`console-script-log-files`" section.

Fields
^^^^^^
All fields in a glu script are stored (locally on the agent) and exported (remotely to ZooKeeper). Check the ":ref:`agent-integration-zookeeper`" section.


An example of glu script
------------------------

.. image:: /images/glu_script_example.png
   :align: center
   :width: 800
   :height: 581
   :scale: 85
   :alt: glu script example

Real life example
-----------------
You can find a real life example of a glu script called `JettyGluScript <https://github.com/linkedin/glu/blob/master/scripts/org.linkedin.glu.script-jetty/src/main/groovy/JettyGluScript.groovy>`_ which shows how to deploy a webapp container (jetty), install web applications in it and monitor it.
