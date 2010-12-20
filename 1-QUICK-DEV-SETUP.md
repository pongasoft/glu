Quick Setup Guide
=================
This is a quick setup guide that shows you how to bring all the stack up (step 3 and 4 are optional and are just meant to verify that the agents are up and familiarizes you with the tools).

1. Install ZooKeeper
--------------------
First you need ZooKeeper installed. If you do not have a ZooKeeper running on your box then you can either:

* download it and install it from [the main website](http://hadoop.apache.org/zookeeper/)

* download and install the server and cli from the sibling project on github called [linkedin-zookeeper](https://github.com/linkedin/linkedin-zookeeper/downloads) (if you want to build it yourself, follow the [instructions](https://github.com/linkedin/linkedin-zookeeper/blob/master/README.md))

In any case, make sure that ZooKeeper is up and running. If you installed the cli simply run:

    <path_to_cli>/bin/zk.sh ls /

which will display

    zookeeper

2. Bring the glu agent(s) up
----------------------------
    cd agent/org.linkedin.glu.agent-server

    gradle setup-2-2

This will automatically create a setup by loading all the necessary information in ZooKeeper and creating a startup script: it creates 2 fabrics and 2 agents.

Go back to checkout root

    cd ../..

Go to the dist devsetup folder

    cd out/build/agent/org.linkedin.glu.agent-server/install/devsetup

and start the 2 agents:

    ./agentdevctl.sh start

You can now issue

    ./agentdevctl.sh tail

which will automatically tail the log files of both agents

3. Try the agent cli (optional)
-------------------------------
Go to checkout root (you may want to do this in a different window as the tail command is blocking)

    cd agent/org.linkedin.glu.agent-cli

    gradle package-install

Go to the installation folder (the previous command will tell you where) and issue:

    ./bin/agent-cli.sh -s https://localhost:13906
    
which returns (list all mountpoints on agent-1)

    [/]

then

    ./bin/agent-cli.sh -s https://localhost:13907

which returns (list all mountpoints on agent-2)

    [/]

then

    ./bin/agent-cli.sh -s https://localhost:13906 -m /

which returns (details about the mountPoint '/' on agent-1)

    [scriptDefinition:[initParameters:[:], mountPoint:/, scriptFactory:[class:org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory, className:org.linkedin.glu.agent.impl.script.RootScript]], scriptState:[stateMachine:[currentState:installed], script:[rootPath:/]]]

Note that when issuing this command you should see an entry in the log file of the agent (if you
continued the tail started in step 2).

4. Try the REST api directly (optional)
---------------------------------------
Go to checkout root

and issue the command which is doing a `GET /agent` on agent-2 using the right keys

    curl -k https://localhost:13907/agent -E agent/org.linkedin.glu.agent-server/src/zk-config/keys/console.dev.pem

    {"fullState":{"scriptDefinition":{"initParameters":{},"mountPoint":"/","scriptFactory":    {"class":"org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory","className":    "org.linkedin.glu.agent.impl.script.RootScript"}},"scriptState":{"stateMachine":{"currentState":"installed"},"script":{"rootPath":"/"}}}}

The passphrase you are promted for is: `password`

Note how what you get back is a json string

5. Start the console
--------------------
Go to checkout root

    cd console/org.linkedin.glu.console-webapp

    gradle -i run-app

Note that in order to work you must have grails installed. The -i option is a bit verbose but if you don't gradle is very silent and you don't see the output coming from grails.

    [ant:exec] Server running. Browse to http://localhost:8080/console

Note that if you prefer you can run:

    gradle lib
    grails run-app

This way you run grails command directly. gradle lib is used to populate the lib folder with the
right set of dependencies and bootstrap information for the app.

At this stage you are all setup!!!!

Check the document [2-QUICK-DEV-TUTORIAL.md](https://github.com/linkedin/glu/blob/master/2-QUICK-DEV-TUTORIAL.md) for a quick walkthrough the console.

6. Setup configuration
----------------------
The same way you can configure the build, you can also configure the setup by editing the file

    ~/.userConfig.properties

    # control the agent setup when running gradle setup from org.linkedin.glu.agent-server
    glu.agent.devsetup.fabric=...
    glu.agent.devsetup.name=...

    # control the agent setup when running gradle setup-x-y from org.linkedin.glu.agent-server
    glu.agent.devsetup.basePort=13906
    glu.agent.devsetup.zkRoot=/org/glu
    glu.agent.devsetup.dir=... <---- this is most likely the one you will modify to install somewhere else
    glu.agent.setup.zkConfigDir=...

Check the file [`build.gradle`](https://github.com/linkedin/glu/blob/master/agent/org.linkedin.glu.agent-server/build.gradle) in `org.linkedin.glu.agent-server` for details on how those properties
are used.

7. Different setups
-------------------
The command `gradle setup-2-2` has several flavors using gradle task rules. It allows to configure and setup your development environment with multiple agents on multiple fabrics quickly and effortlessly: the first number is the number of agents, the second one is the number of fabrics.

8. Cleaning up
--------------
In order to clean up you can do the following:

Stop all the agents that were started in 2. by issuing

    ./agentdevctl.sh stop

(you may need to `CTRL-C` the tail command if it is still running)

Under `agent/org.linkedin.glu.agent-server` you can use 

    gradle clean-setup

which cleans up all the data in ZooKeeper and deletes the devsetup folder created in step 2.

You can then shutdown ZooKeeper 
