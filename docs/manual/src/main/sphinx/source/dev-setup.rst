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

Compiling glu
=============

Compilation
-----------
In order to compile the code you need

* java 1.7
* Unix (tested on Mac OS X and Solaris)

At the top simply run::

    ./gradlew test

which should compile and run all the tests (glu uses the gradle wrapper which will download the proper version of gradle as well as the grails wrapper which will as well download the proper version of grails).

IDE Support
-----------
You can issue the command (at the top)::

    ./gradlew cleanIdea idea

which will use the gradle IDEA plugin to create the right set of modules in order to open the
project in IntelliJ IDEA.

Directory structure
-------------------
* ``agent/org.linkedin.glu.agent-api``:
  Agent api (like Agent class)

* ``agent/org.linkedin.glu.agent-impl``:
  Implementation of the agent

* ``agent/org.linkedin.glu.agent-rest-resources``:
  REST resources (endpoint) (used in the agent)

* ``agent/org.linkedin.glu.agent-rest-client``:
  REST client (which talks to the resources) (used in both agent-cli and console-webapp)

* ``agent/org.linkedin.glu.agent-cli``:
  The command line which can talk to the agent directly.

* ``agent/org.linkedin.glu.agent-cli-impl``:
  Contains the implementation of the agent cli

* ``agent/org.linkedin.glu.agent-tracker``
  Listens to ZooKeeper events to track the agent writes (used in console-webapp).

* ``agent/org.linkedin.glu.agent-server-impl``:
  Contains the implementation of the agent server cli.

* ``agent/org.linkedin.glu.agent-server-upgrade``:
  Create the upgrade package (to be used when uprading an already installed agent).

* ``agent/org.linkedin.glu.agent-server``:
  The actual server.

* ``console/org.linkedin.glu.console-webapp``:
  The console webapp (grails application)::

        ../../gradlew lib // compile all the dependencies and put them in the lib folder
        ../../gradlew run-app // after running gradle lib, you can simply run grails directly as it will use the
                              // libraries in lib to boot the app

* ``console/org.linkedin.glu.console-cli``:
  The cli for the console (written in python) to use the REST api of the console.

* ``console/org.linkedin.glu.console-server``:
  The ``console/org.linkedin.glu.console-webapp`` project generates the war (or is used in dev through grails). This project creates a ready to run console embedding jetty.

* ``docs/manual``:
  The manual/documentation. To build simply issue::

        ../../gradlew doc

  .. note:: You need to have sphinx installed in order to build the documentation.

  .. note:: The various screenshots have been taken from the live running webapp and using ImageMagick to 
            rescale them::

              convert xxx.png -resize 600 xxx.png
      

* ``packaging/org.linkedin.glu.packaging-all``:
  Creates a package which contains all prebuilt packages and is also used for the tutorial::

        ../../gradlew package // create the package
        ../../gradlew package-install // to install locally (for dev)

        ../../gradlew dev-setup     // generate a distribution with 1 agent in 1 fabric
                                    // shell script (devsetupctl.sh) allows to start/stop
        ../../gradlew dev-setup-x-y // generate a distribution with x agents in y fabrics
                                    // shell script (devsetupctl.sh) allows to start/stop

        ../../gradlew clean-dev-setup // to delete the setup

* ``packaging/org.linkedin.glu.packaging-setup``:
  Creates a package with convenient shell scripts to setup the keys and agent in ZooKeeper::

        ../../gradlew package // create the package
        ../../gradlew package-install // to install locally (for dev)

* ``dev-keys``:
  Contains the keys used in dev (check `key_generation.txt <https://github.com/pongasoft/glu/blob/master/dev-keys/key_generation.txt>`_) for instructions on how to generate a different set of keys)

Build configuration
-------------------
The project uses the `org.linkedin.userConfig <https://github.com/pongasoft/gradle-plugins/blob/master/README.md>`_ plugin and as such can be configured

Example::

    ~/.userConfig.properties
    top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
    top.install.dir="/export/content/${userConfig.project.name}"
    top.release.dir="/export/content/repositories/release"
    top.publish.dir="/export/content/repositories/publish"

Quick Setup Guide
-----------------
This is a quick setup guide that shows you how to bring all the stack up (step 2 and 3 are optional and are just meant to verify that the agents are up and help you get familiar with the tools).

1. Bring the (dev) stack up
^^^^^^^^^^^^^^^^^^^^^^^^^^^
Issue::

    cd packaging/org.linkedin.glu.packaging-all

    ../../gradlew dev-setup-2-2

This will automatically create a full stack setup (ZooKeeper, console and 2 agents in 2 fabrics).

Go back to checkout root then issue::

    cd ../..

Go to the dist ``dev-setup`` folder::

    cd out/build/packaging/org.linkedin.glu.packaging-all/install/dev-setup

.. tip::
   The build output shows you were the folder is located::

     Created dev setup: /Users/ypujante/github/org.pongasoft/glu/out/build/packaging/org.linkedin.glu.packaging-all/install/dev-setup/bin/devsetupctl.sh

and start the stack::

    ./bin/devsetupctl.sh start

You can now issue::

    ./bin/devsetupctl.sh tail

which will automatically tail the log files of all components of the stack.

2. Try the agent cli (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
You can try the ``agent-cli`` and make sure that you can communicate with the agents::

    ./dists/agent-cli/org.linkedin.glu.agent-cli-<version>/bin/agent-cli.sh -s https://localhost:13906
    
which returns (list all mountpoints on agent-1)::

    [/]

then::

    ./dists/agent-cli/org.linkedin.glu.agent-cli-<version>/bin/agent-cli.sh -s https://localhost:13908

which returns (list all mountpoints on agent-2)::

    [/]

then::

    ./dists/agent-cli/org.linkedin.glu.agent-cli-<version>/bin/agent-cli.sh -s https://localhost:13906 -m /

which returns (details about the mountPoint '/' on agent-1)::

    [scriptDefinition:[initParameters:[:], mountPoint:/, scriptFactory:[class:org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory, className:org.linkedin.glu.agent.impl.script.RootScript]], scriptState:[stateMachine:[currentState:installed], script:[rootPath:/]]]

Note that when issuing this command you should see an entry in the log file of the agent (if you continued the tail started in step 1).

.. warning::
   You need to replace ``<version>`` by the appropriate version of the build!

3. Try the REST api directly (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Issue the command which is doing a ``GET /mountPoint/`` on agent-2 using the right keys::

    curl -k https://localhost:13906/mountPoint/ -E dev-keys/console.pem

    {"fullState":{"scriptDefinition":{"initParameters":{},"mountPoint":"/","scriptFactory": {"class":"org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory","className": "org.linkedin.glu.agent.impl.script.RootScript"}},"scriptState":{"stateMachine":{"currentState":"installed"},"script":{"rootPath":"/"}}}}

.. note::
   What you get back is a json string!

4. Stopping the stack
^^^^^^^^^^^^^^^^^^^^^
In order to stop the stack, simply issue::

  ./bin/devsetupctl.sh stop

5. Generating a new stack
^^^^^^^^^^^^^^^^^^^^^^^^^
In order to generate a new stack, it is important to clean the previous one, which can be done by simply doing::

   ../../gradlew clean-dev-setup dev-setup-2-2

.. tip::
   If there is already a stack up and running, this command will stop it first.

6. Working on the console
^^^^^^^^^^^^^^^^^^^^^^^^^
Step 1 generates a full stack, which means at this stage, you have ZooKeeper, the agent(s) and the console up and running. If you want to work on the console, you should generate a partial stack by following these steps instead.

Go to checkout root then issue::

    cd packaging/org.linkedin.glu.packaging-all

    ../../gradlew -Pno.console-server clean-dev-setup dev-setup-2-2

This will generate a stack with only ZooKeeper and the agent(s) but not the console. You start the stack the same way described in Step 1. Then in order to start the console, you do the following:

Go to checkout root then issue::

    cd console/org.linkedin.glu.console-webapp

    ../../gradlew -i run-app

The ``-i`` option is a bit verbose but if you don't gradle is very silent and you don't see the output coming from grails::

    [ant:exec] Server running. Browse to http://localhost:8080/console

Note that if you prefer you can run::

    ../../gradlew lib
    ./grailsw run-app

This way you run grails command directly. ``gradle lib`` is used to populate the ``lib`` folder with the
right set of dependencies and bootstrap information for the app.

At this stage you are all setup!!!!

Check the section :doc:`tutorial` for a quick walkthrough the console.

7. Setup configuration
^^^^^^^^^^^^^^^^^^^^^^
The same way you can configure the build, you can also configure the setup by editing the file::

    ~/.userConfig.properties

    glu.packaging.dev-setup.agent.base.port=13906
    glu.packaging.dev-setup.dir=... <---- this is most likely the one you will modify to install somewhere else

8. Different setups
^^^^^^^^^^^^^^^^^^^
The command ``../../gradlew dev-setup-2-2`` has several flavors using gradle task rules. It allows to configure and setup your development environment with multiple agents on multiple fabrics quickly and effortlessly: the first number is the number of agents, the second one is the number of fabrics.

.. tip::
   ``dev-setup`` is a shortcut for ``dev-setup-1-1``
