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

Development Setup
=================

Compilation
-----------
In order to compile the code you need

* java 1.6
* `gradle 0.9 <http://www.gradle.org/>`_
* `grails 1.3.5 <http://www.grails.org/>`_ (only for the console)
* Unix (tested on Mac OS X and Solaris)

Before doing anything go to::

    console/org.linkedin.glu.console-webapp

and issue::

    grails install-plugin ../../local-maven-repo/com/pongasoft/grails-external-domain-classes/1.0.0/grails-external-domain-classes-1.0.0.zip
    grails upgrade

At the top simply run::

    gradle test

which should compile and run all the tests.

Note: if you do not run the 'grails upgrade' command, you may see this messages::

    Plugin `shiro-1.1-SNAPSHOT <not installed. ...
    Plugin [hibernate-1.3.5] not installed. ...
    Plugin [tomcat-1.3.5] not installed. ...

IDE Support
-----------
You can issue the command (at the top)::

    gradle idea

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
  The command line which can talk to the agent directly::

        gradle package // to create the package
        gradle package-install // to install locally (for dev)

* ``agent/org.linkedin.glu.agent-cli-impl``:
  Contains the implementation of the agent cli

* ``agent/org.linkedin.glu.agent-tracker``
  Listens to ZooKeeper events to track the agent writes (used in console-webapp).

* ``agent/org.linkedin.glu.agent-server-impl``:
  Contains the implementation of the agent server cli.

* ``agent/org.linkedin.glu.agent-server-upgrade``:
  Create the upgrade package (to be used when uprading an already installed agent)::

        gradle package // create the upgrade package

* ``agent/org.linkedin.glu.agent-server``:
  The actual server::

        gradle package // create the package

        gradle package-install // to install locally (for dev)
        gradle setup // setup dev fabric and keys in zookeeper (used in conjunction with gradle install)

        gradle setup-x-y // setup x agents in y fabrics: automatically setup zookeeper with the right set of
                            data and create x agents package with a wrapper shell script to start them all

        gradle clean-setup // to delete the setup

* ``console/org.linkedin.glu.console-webapp``:
  The console webapp (grails application)::

        gradle lib // compile all the dependencies and put them in the lib folder
        grails run-app // after running gradle lib, you can simply run grails directly as it will use the
                       // libraries in lib to boot the app

* ``console/org.linkedin.glu.console-cli``:
  The cli for the console (written in python) to use the REST api of the console::

        gradle package // create the package
        gradle package-install // to install locally (for dev)

* ``console/org.linkedin.glu.console-server``:
  The ``console/org.linkedin.glu.console-webapp`` project generates the war (or is used in dev through grails). This project creates a ready to run console embedding jetty::

        gradle package // create the package
        gradle package-install // to install locally (for dev)

* ``docs/manual``:
  The manual/documentation. To build simply issue::

        gradle doc

  .. note:: You need to have sphinx installed in order to build the documentation.

  .. note:: The various screenshots have been taken from the live running webapp and using ImageMagick to 
            rescale them::

              convert xxx.png -resize 600 xxx.png
      

* ``packaging/org.linkedin.glu.packaging-all``:
  Creates a package which contains all prebuilt packages and is also used for the tutorial::

        gradle package // create the package
        gradle package-install // to install locally (for dev)

* ``packaging/org.linkedin.glu.packaging-setup``:
  Creates a package with convenient shell scripts to setup the keys and agent in ZooKeeper::

        gradle package // create the package
        gradle package-install // to install locally (for dev)

* ``dev-keys``:
  Contains the keys used in dev (check `key_generation.txt <https://github.com/linkedin/glu/blob/master/dev-keys/key_generation.txt>`_) for instructions on how to generate a different set of keys)

Build configuration
-------------------
The project uses the `org.linkedin.userConfig <https://github.com/linkedin/gradle-plugins/blob/master/README.md>`_ plugin and as such can be configured

Example::

    ~/.userConfig.properties
    top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
    top.install.dir="/export/content/${userConfig.project.name}"
    top.release.dir="/export/content/repositories/release"
    top.publish.dir="/export/content/repositories/publish"

Quick Setup Guide
-----------------
This is a quick setup guide that shows you how to bring all the stack up (step 3 and 4 are optional and are just meant to verify that the agents are up and familiarizes you with the tools).

1. Install ZooKeeper
^^^^^^^^^^^^^^^^^^^^
First you need ZooKeeper installed. If you do not have a ZooKeeper running on your box then you can either:

* download it and install it from `the main website <http://hadoop.apache.org/zookeeper/>`_
* download and install the server and cli from the sibling project on github called `linkedin-zookeeper <https://github.com/linkedin/linkedin-zookeeper/downloads>`_ (if you want to build it yourself, follow the `instructions <https://github.com/linkedin/linkedin-zookeeper/blob/master/README.md>`_)

In any case, make sure that ZooKeeper is up and running. If you installed the cli simply run::

    <path_to_cli>/bin/zk.sh ls /

which will display::

    zookeeper

2. Bring the glu agent(s) up
^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Issue::

    cd agent/org.linkedin.glu.agent-server

    gradle setup-2-2

This will automatically create a setup by loading all the necessary information in ZooKeeper and creating a startup script: it creates 2 fabrics and 2 agents.

Go back to checkout root::

    cd ../..

Go to the dist devsetup folder::

    cd out/build/agent/org.linkedin.glu.agent-server/install/devsetup

and start the 2 agents::

    ./agentdevctl.sh start

You can now issue::

    ./agentdevctl.sh tail

which will automatically tail the log files of both agents

3. Try the agent cli (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Go to checkout root (you may want to do this in a different window as the tail command is blocking)::

    cd agent/org.linkedin.glu.agent-cli

    gradle package-install

Go to the installation folder (the previous command will tell you where) and issue::

    ./bin/agent-cli.sh -s https://localhost:13906
    
which returns (list all mountpoints on agent-1)::

    [/]

then::

    ./bin/agent-cli.sh -s https://localhost:13907

which returns (list all mountpoints on agent-2)::

    [/]

then::

    ./bin/agent-cli.sh -s https://localhost:13906 -m /

which returns (details about the mountPoint '/' on agent-1)::

    [scriptDefinition:[initParameters:[:], mountPoint:/, scriptFactory:[class:org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory, className:org.linkedin.glu.agent.impl.script.RootScript]], scriptState:[stateMachine:[currentState:installed], script:[rootPath:/]]]

Note that when issuing this command you should see an entry in the log file of the agent (if you continued the tail started in step 2).

4. Try the REST api directly (optional)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Go to checkout root

and issue the command which is doing a ``GET /agent`` on agent-2 using the right keys::

    curl -k https://localhost:13907/agent -E agent/org.linkedin.glu.agent-server/src/zk-config/keys/console.dev.pem

    {"fullState":{"scriptDefinition":{"initParameters":{},"mountPoint":"/","scriptFactory":    {"class":"org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory","className":    "org.linkedin.glu.agent.impl.script.RootScript"}},"scriptState":{"stateMachine":{"currentState":"installed"},"script":{"rootPath":"/"}}}}

The passphrase you are promted for is: ``password``

Note how what you get back is a json string

5. Start the console
^^^^^^^^^^^^^^^^^^^^
Go to checkout root::

    cd console/org.linkedin.glu.console-webapp

    gradle -i run-app

Note that in order to work you must have grails installed. The -i option is a bit verbose but if you don't gradle is very silent and you don't see the output coming from grails::
    [ant:exec] Server running. Browse to http://localhost:8080/console

Note that if you prefer you can run::

    gradle lib
    grails run-app

This way you run grails command directly. gradle lib is used to populate the lib folder with the
right set of dependencies and bootstrap information for the app.

At this stage you are all setup!!!!

Check the section :doc:`tutorial` for a quick walkthrough the console.

6. Setup configuration
^^^^^^^^^^^^^^^^^^^^^^
The same way you can configure the build, you can also configure the setup by editing the file::

    ~/.userConfig.properties

    # control the agent setup when running gradle setup from org.linkedin.glu.agent-server
    glu.agent.devsetup.fabric=...
    glu.agent.devsetup.name=...

    # control the agent setup when running gradle setup-x-y from org.linkedin.glu.agent-server
    glu.agent.devsetup.basePort=13906
    glu.agent.devsetup.zkRoot=/org/glu
    glu.agent.devsetup.dir=... <---- this is most likely the one you will modify to install somewhere else
    glu.agent.setup.zkConfigDir=...

Check the file `build.gradle <https://github.com/linkedin/glu/blob/master/agent/org.linkedin.glu.agent-server/build.gradle>`_ in ``org.linkedin.glu.agent-server`` for details on how those properties
are used.

7. Different setups
^^^^^^^^^^^^^^^^^^^
The command ``gradle setup-2-2`` has several flavors using gradle task rules. It allows to configure and setup your development environment with multiple agents on multiple fabrics quickly and effortlessly: the first number is the number of agents, the second one is the number of fabrics.

8. Cleaning up
^^^^^^^^^^^^^^
In order to clean up you can do the following:

Stop all the agents that were started in Step 2. by issuing::

    ./agentdevctl.sh stop

(you may need to ``CTRL-C`` the tail command if it is still running)

Under ``agent/org.linkedin.glu.agent-server`` you can use::

    gradle clean-setup

which cleans up all the data in ZooKeeper and deletes the devsetup folder created in step 2.

You can then shutdown ZooKeeper 
