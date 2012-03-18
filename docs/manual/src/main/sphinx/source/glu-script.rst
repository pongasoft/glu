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

The minimum (usefull) state machine that you can define could look like::

    [
        NONE: [ [to: 'running', action: 'start'] ],
        running: [ [to: 'NONE', action: 'stop'] ]
    ]

.. note:: If an action is empty you don't even have to define its equivalent action but you still need to call all prior actions to satisfy the state machine.


Defining your own state machine
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. sidebar:: Advanced Feature

             This section is for advanced users only. You can safely skip it and come back later if you feel like you need to change the default state machine.

In the event when the default state machine does not match your needs, you can define your own (system wide) state machine and configure glu to use it.

For this you need to create a jar file which contains a file called ``glu/DefaultStateMachine.groovy``. This file needs to look like this::

    defaultTransitions =
    [
      NONE: [[to: 's1', action: 'noneTOs1']],
      s1: [[to: 'NONE', action: 's1TOnone'], [to: 's2', action: 's1TOs2']],
      s2: [[to: 's1', action: 's2TOs1']]
    ]

    defaultEntryState = 's2'

* ``defaultTransitions`` defines the transitions for the state machine
* ``defaultEntryState`` defines which state in the state machine is the one that is considered to not be in error (``running`` in the case of the default state machine)

Making it available to glu
^^^^^^^^^^^^^^^^^^^^^^^^^^

In order for glu to use your state machine you simply need to drop the jar file you created in the previous step in the following locations:

* for the console, in the ``console-server/glu/repository/plugins`` folder
* for the agent, in the ``agent-server/<version>/lib`` folder
* for the agent cli, in the ``agent-cli/lib`` folder

.. note:: 
   Those folder are relative to the *standard* distribution of glu. If you package it yourself, then make sure that the jar file is in the classpath of your server.

Configuring the console
^^^^^^^^^^^^^^^^^^^^^^^

You will need to configure the console (UI) to display your own actions if you want to. Check the :ref:`console-configuration-plans` and :ref:`console-configuration-mountPointActions` sections for more details.

.. tip::
   In addition to your own state machine you can also use the :ref:`plugin hook <goe-plugins>` ``PlannerService_pre_computePlans`` to define your own custom actions!

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

.. _glu-script-parent-script:

Parent Script
-------------
When you define the parent glu script (for use in the :ref:`static model <static-model-entries-parent>`), you **must** add the following closure to the glu script::

   def createChild = { args ->
     return args.script
   }

This closure takes a map with 3 arguments:

* ``mountPoint``: the mountPoint on which the child script will be mounted
* ``script``: the raw child script just after it has been instantiated
* ``initParameters``: the init parameters that will be provided to the child

This closure **must** return the actual script to use. In its simplest form, the closure does nothing besides returning the script itself untouched.

.. tip:: This closure allows you to customize the child including returning a completely different one!
   For example::

	class JettyParentGluScript
        {
          def deployHotDir

          def install = { 
            deployHotDir = ... // compute hot dir
          }

          def createChild = { args ->
            args.script.deployHotDir = deployHotDir // 'inject' deployHotDir in child
            return args.script
          }
        }

In addition to this required closure, you *may* define 3 others to do custom work::

   def onChildAdded = { args -> // child
     // note that the child you are getting here is different from the script you got in createChild
     // in createChild you get literally the instance of the class of the script
     // in onChildAdded you get an instance of GluScript which is the wrapped script
   }

   // symmetric of onChildAdded
   def onChildRemoved = { args -> // child
   }

   // symmetric of createChild
   def destroyChild = { args -> // mountPoint, script
   }

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

Developing and unit testing a glu script
----------------------------------------
The glu script test framework allows you to develop and unit test your glu script without having to worry about setting up all the components. To write a unit test for a glu script, you can simply inherit from the `GluScriptBaseTest <https://github.com/linkedin/glu/blob/master/utils/org.linkedin.glu.scripts-test-fwk/src/main/groovy/org/linkedin/glu/scripts/testFwk/GluScriptBaseTest.groovy>`_, setup a couple of parameters and run the convenient methods provided by the framework::

  class TestMyGluScript extends GluScriptBaseTest
  {
    public void setUp() {
      super.setUp()
      initParameters = [ p1: 'v1' ]
    }

    // this method is not required if you follow the conventions
    public String getScriptClass() {
      return MyGluScript.getClass().getName()
    }

    public void testHappyPath() {
      deploy()
      undeploy()
    }
  }

In order to compile the script and the unit test, you need the following dependencies (make sure you use the appropriate versions which may differ from this example!)::

    // gradle format
    dependencies {
      compile "org.linkedin:org.linkedin.util-groovy:1.7.0"
      compile "org.linkedin:org.linkedin.glu.agent-api:3.1.0"
      groovy  "org.codehaus.groovy:groovy:1.7.5"

      testCompile "org.linkedin:org.linkedin.glu.scripts-test-fwk:3.1.0"
      testCompile "junit:junit:4.4"
    }

.. note:: You can use maven or any other dependency management system as long you include the proper dependencies.

.. tip:: For more information and examples, you can check the following:

   * `GluScriptBaseTest <https://github.com/linkedin/glu/blob/master/utils/org.linkedin.glu.scripts-test-fwk/src/main/groovy/org/linkedin/glu/scripts/testFwk/GluScriptBaseTest.groovy>`_ to check what the framework has to offer (javadoc is fairly comprehensive)
   * `TestJettyGluScript <https://github.com/linkedin/glu/blob/master/scripts/org.linkedin.glu.script-jetty/src/test/groovy/test/script/jetty/TestJettyGluScript.groovy>`_ for a real life example of unit testing a glu script
   * `glu-scripts-contrib <https://github.com/linkedin/glu-scripts-contrib>`_ is the project that contains glu script contributed by the community as well as a sample
   * `sample <https://github.com/linkedin/glu-scripts-contrib/tree/master/scripts/org.linkedin.glu-scripts-contrib.sample>`_ is a sample glu script and unit test with comprehensive documentation demonstrating several features about writing and unit testing a glu script

.. _glu-script-packaging:

Packaging a glu script
----------------------
A glu script can be packaged in 2 different ways:

* as a simple groovy file, in which case the ``script`` entry in the model is a URI pointing directly to the groovy file. 
  Example::

    "script": "http://host:port/x/c/v/MyGluScript.groovy"

* already compiled and packaged in a jar file (new since 4.2.0), in which case the ``script`` entry in the model is a special 
  URI of the form::

    class:/<FQCN>?cp=<URI to jar>&cp=<URI to jar>...

  Example::

    "script": "class:/com.acme.MyGluScript?cp=http%3A%2F%2Facme.com%2Fjars%2Fscript.jar&cp=http%3A%2F%2Facme.com%2Fjars%2Fdependency.jar"

  .. tip:: In this second form, the script can be split into multiple files and have external dependencies (as long as they are provided as classpath elements)

  .. warning:: Every classpath element (``cp``) being a query string paramater must be properly URL encoded!


Inheritance
-----------
New since 4.2.0, a glu script can now inherit from another one (in which case you should use the second packaging technique so that you can distribute the base script as a dependency). Here is an example:

The base script::

  package test.agent.base

  class BaseScript
  {
    def base1
    def base2
    def base3

    def install = { args ->
      log.info "base.install"
      base1 = params.base1Value
      log.info "base.install.\${args.sub}.\${subValue}"
    }

    def baseConfigure = { args ->
      base2 = args.base2Value
      return "base.baseConfigure.\${args.sub}.\${subValue}"
    }

    protected def getSubValue()
    {
      return "fromBaseScript"
    }
  }

The subclass script::

  package test.agent.sub

  import test.agent.base.BaseScript

  class SubScript extends BaseScript
  {
    String sub1

    def configure = { args ->
      sub1 = baseConfigure(args)
      base3 = params.base3Value
    }

    protected def getSubValue()
    {
      return "fromSubScript"
    }
  }


A few words about the example:

* all attributes defined in the base script will automatically be exported to ZooKeeper as if they were defined in the subclass!

* since glu uses closures (and not methods), you cannot `override` a lifecycle method. Instead you should use a technique similar to the example in which the base class defines a closure (``baseConfigure``) that gets called directly by the subclass.
