Quick Tutorial
==============
The purpose of this tutorial is to give you a taste of glu: the idea is to be up and running as quickly as possible and try it for yourself so that you get a feel of what glu can do.

Note: for the sake of this tutorial and development ease, the agent(s) and the console are all running on the same host. This is not the production case where there is 1 agent per host with a central console to command them.

Install the tutorial
--------------------
Download the binary called `org.linkedin.glu.packaging-all-<version>.tgz` from the [downloads](https://github.com/linkedin/glu/downloads) section on github.
  
Untar/Unzip in a location of your choice. From now on, this location will be referred to as `GLU_TUTORIAL_ROOT`

Initial setup
-------------

    cd $GLU_TUTORIAL_ROOT
    ./bin/tutorial.sh setup
    
This step does the following:

1. it loads the keys in ZooKeeper (`agent.keystore`, `console.truststore`) for the fabric `glu-dev-1`
2. it loads the agent configuration in ZooKeeper (`config.properties`) for the fabric `glu-dev-1`
3. it configures and assigns `agent-1` to fabric `glu-dev-1`

Start all components
--------------------

    ./bin/tutorial.sh start
    ./bin/tutorial.sh tail

This will start 3 components:

* ZooKeeper
* the agent
* the console

The second command tails the log for each component.

Login to the console
-----------------------
Point your browser to `http://localhost:8080/console`

and login

    username: admin
    password: admin

The very first time, you will be prompted to create a fabric:

    Name             : glu-dev-1
    Zk Connect String: localhost:2181
    Zk Timeout       : 30s

When the console is launched for the very first time, the database is empty and you need to add a fabric to it. In this case we create the fabric called `glu-dev-1` (which is the same name used in the setup) and we associate it to the local ZooKeeper.

View the agent
--------------
Click on the `'Dashboard'` tab

Click on `agent:1` (in the `'Group By'` section) (don't click on the checkbox, but on `'agent:1'`)

You should see a row in the table where the status says 

    'nothing deployed'

Click on `'agent-1'` (in the first column of the table) which brings you to the agent view.

Click on `'View Details'` which show/hide the details about the agent: this is information coming straight from agent-1 which was registered in ZooKeeper when the agent started.

You should see the properties `glu.agent.port` (12906) and `glu.agent.pid` representing the pid of the agent.

View log files
--------------
Click on `'main'` (next to Logs:) which shows the last 500 lines of the main log file of the agent (if you scroll to the bottom you should see the same message that the tail command (started previously is showing)).

Note how the agent logs a message that you are looking at its log file!

Go back to the agent view page and click `'more...'` (next to `Logs:`). This will show you the content of the logs folder and you can navigate to look at any file you want!

All those operations are executed on the agent(s) and the console merely displays the result (as can be seen in the log file of the agent).

View processes (ps)
-------------------
Go back to the agent view page and click `'All Processes'`. This essentialy runs the `'ps'` command on the agent and returns the result. In the `org.linkedin.app.name` column you should be able to identify the agent that is running. By clicking on the pid you can view details about the process as well as sending a signal to the process!

All those operations are executed on the agent(s) and the console merely displays the result (as can be seen in the log file of the agent).

Loading the model
-----------------
Click on the `'Model'` tab and enter

    Json Uri: http://localhost:8080/glu/sample/systems/hello-world-system.json

and click `Load`.

Note: the console is a simple web application and is being run in a jetty container which is also used to serve static content. In a production environment it is usually *not* the way it is being done as the agents would not in general talk to the console but instead would fetch their information from a binary repository (like Artifactory) using the ivy protocol for example.

Note: you can view the model you just loaded at [http://localhost:8080/glu/sample/systems/hello-world-system.json](http://localhost:8080/glu/sample/systems/hello-world-system.json)

'Fixing' the issues
-------------------
After loading the model you should be back on the Dashboard view with 4 red rows in the table. The status of all those rows read: `'NOT deployed'`. 

What has just happened ? 

We have just loaded a model which represents a system where 4 'entries' need to be running on agent-1. Since nothing is running, there is a delta (represented by the red rows) that the console tells you to fix. 'Fixing' it means deploying the 4 'entries'.

From there, there are several ways to do it (partially or all at once). Let's do it all for now.

Click on the `'System'` tab.

Click on the `'Current'` subtab. You should see a drop down below `"Deploy: Fabric [glu-dev-1]"` which says `'Choose Plan'`. Select the one that has `SEQUENTIAL` in the name. It should immediately shows you the list of actions (and their ordering) that are going to be accomplished to 'fix' the delta.

Click `'Select this plan'`.

The next page allows you to _customize_ the plan. Simply click `'Execute'` and confirm the action.

The next page will show you the plan again and will change as the plan gets executed. Since you selected `SEQUENTIAL` all the actions will take place one after another. The plan should conclude successfully.

At this stage you can check the tail command output and see all the activity.

Go back to the `Dashboard` and everything should be green.

Note: the terminology 'entry' may sound a little vague right now, but it is associated to a unique mountPoint (or unique key) like `/m1/i001` on an agent with a script (called glu script) which represents the set of instructions necessary to start an application. In the course of this tutorial we use the [`HelloWorldScript`](https://github.com/linkedin/glu/blob/master/scripts/glu.script-hello-world/src/main/groovy/HelloWorldScript.groovy) which displays the name of the 'phase' and the message. So we do not really start anything, but the scripts can do whatever you want them to do.

Viewing entry details
---------------------
Click on `'agent-1'` on any of the 4 rows to go back to the agent page (same step as before).

The page shows you now the 4 entries that were installed.

Under `/m1/i001` click the `'View Details'` link to show/hide details about the entry.

You should see the message (`Hello World`) and the location of the `HelloWorldScript`

Changing the system
-------------------
Now click the `'System'` tab again.

You should see a table with 1 entry which shows you the systems that you loaded.

Click on the first id. You should now see the json document that you loaded previously. We are going to edit it in place.

The format is a json document which contains essentially an array of entries representing each entry in the system (as explained previously).

Change the message in the very first entry to `"Hello World 2"` and click `"Save Changes"`

Go back to the `Dashboard`.

Note that the first row is now red and the status says `'version MISMATCH'`. If you click on the status it will say:

    initParameters.message:Hello World 2 != initParameters.message:Hello World

There is a delta: the system in the console is not matching with what is currently deployed. Hence it is red.

Click on `'/m1/i001'` and you land on a filtered view containing only the mountPoint you clicked on.

Choose a plan under `'Deploy: mountPoint [/m1/i001]'`. Note that since there is only 1 entry, choosing `SEQUENTIAL` or `PARALLEL` will have the same effect.

Select the plan and execute it: it firsts uninstall the first one entirely and reinstall and restart the new one.

When the plan finishes executing, click on `/m1/i001` which is a shortcut to the agent view page.

If you click on `'View Details'` you should see the new message.

Now the system (also known as desired state) and the current state match. There is no delta anymore so the console is happy: everything is green.

Reloading the model
-------------------
Manually edit the file: `$GLU_TUTORIAL_ROOT/org.linkedin.glu.console-server-<version>/glu/sample/systems/hello-world-system.json`

Change the message in the very first entry to `"Hello World 3"` and save your changes

Go back to the console and reload the model:

Click on the `'Model'` tab and enter

    Json Uri: http://localhost:8080/glu/sample/systems/hello-world-system.json

and click `Load`.

You can follow the same steps described previously to 'fix' this new delta.

If you click on the `System` tab, you will be able to see all the various system. Note that the id is a `sha-1` of the content of the file.

9. Viewing the audit log
------------------------
Click the `'Admin'` tab and then select `'View Audit Logs'`.

You should be able to see all the actions that you have done in the system (usually all actions involving talking to the agent are logged).

10. The end
-----------
That is it for this quick tutorial. You can now check the [documentation](https://github.com/linkedin/glu/wiki).