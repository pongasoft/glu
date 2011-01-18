Quick Tutorial
==============
The purpose of this tutorial is to give you a taste of glu: the idea is to be up and running as quickly as possible and try it for yourself so that you get a feel of what glu can do.

Note: for the sake of this tutorial and development ease, the agent(s) and the console are all running on the same host. This is not the production case where there is 1 agent per host with a central console to command them.

During this tutorial you are going to deploy 3 jetty containers running 4 webapps!

Note that this tutorial will launch several applications on the following ports so make sure nothing is running on them or you will encounter some issues:

    agent:    12906
    console:   8080
    webapp1:   9000
    webapp2:   9001
    webapp3:   9003
    zookeeper: 2181


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
Point your browser to [http://localhost:8080/console](http://localhost:8080/console)

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
Go back to the agent view page and click `'All Processes'`. This essentialy runs the `'ps'` command on the agent and returns the result. In the `org.linkedin.app.name` column you should be able to identify the agent that is running (as well as zookeeper and the console itself). By clicking on the pid you can view details about the process as well as sending a signal to the process!

All those operations are executed on the agent(s) and the console merely displays the result (as can be seen in the log file of the agent).

Loading the model
-----------------
Click on the `'Model'` tab and enter

    Json Uri: http://localhost:8080/glu/repository/systems/sample-webapp-system.json

and click `Load`.

Note: the console is a simple web application and is being run in a jetty container which is also used to serve static content. In a production environment it is usually *not* the way it is being done as the agents would not in general talk to the console but instead would fetch their information from a binary repository (like Artifactory) using the ivy protocol for example.

Note: you can view the model you just loaded at [http://localhost:8080/glu/repository/systems/sample-webapp-system.json](http://localhost:8080/glu/repository/systems/sample-webapp-system.json) (you may need to do 'View Source' in your browser if you don't see anything).

'Fixing' the issues
-------------------
After loading the model you should be back on the Dashboard view with 3 red rows in the table. The status of all those rows read: `'NOT deployed'`. 

What has just happened ? 

We have just loaded a model which represents a system where 3 'entries' need to be running on agent-1. Since nothing is running, there is a delta (represented by the red rows) that the console tells you to fix. 'Fixing' it means deploying the 3 'entries'.

From there, there are several ways to do it (partially or all at once). Let's do it all for now.

Click on the `'System'` tab.

Click on the `'Current'` subtab. You should see a drop down below `"Deploy: Fabric [glu-dev-1]"` which says `'Choose Plan'`. Select the one that has `PARALLEL` in the name. It should immediately shows you the list of actions (and their ordering) that are going to be accomplished to 'fix' the delta.

Click `'Select this plan'`.

The next page allows you to _customize_ the plan. Simply click `'Execute'` and confirm the action.

The next page will show you the plan again and will change as the plan gets executed. Since you selected `PARALLEL` all the actions will take place in parallel. The plan should conclude successfully.

At this stage you can check the tail command output and see all the activity.

Go back to the `Dashboard` and everything should be green.

Note: the terminology 'entry' may sound a little vague right now, but it is associated to a unique mountPoint (or unique key) like `/sample/i001` on an agent with a script (called glu script) which represents the set of instructions necessary to start an application. In the course of this tutorial we use the [`JettyGluScript`](https://github.com/linkedin/glu/blob/master/scripts/org.linkedin.glu.script-jetty/src/main/groovy/JettyGluScript.groovy) which starts a jetty webapp container and deploy some webapps in it.

At this stage you have just started 3 jetty container with 4 webapps and you can verify that it worked:

* webapp1: `/sample/i001` (port 9000)
  * `/cp1`: [http://localhost:9000/cp1/monitor](http://localhost:9000/cp1/monitor)
  * `/cp2`: [http://localhost:9000/cp2/monitor](http://localhost:9000/cp2/monitor)
* webapp2: `/sample/i002` (port 9001)
  * `/cp1`: [http://localhost:9001/cp1/monitor](http://localhost:9001/cp1/monitor)
* webapp3: `/sample/i003` (port 9002)
  * `/cp4`: [http://localhost:9002/cp4/monitor](http://localhost:9002/cp4/monitor)



Viewing entry details
---------------------
Click on `'agent-1'` on any of the 3 rows to go back to the agent page (same step as before).

The page shows you now the 3 entries that were installed.

Under `/sample/i001` click the `'View Details'` link to show/hide details about the entry.

You should see a section called `initParameters` which is coming directly from the system model that you loaded.

You should also see a section called `scriptState` which shows various information like the port (9000) or the pid of the process that was started or the location of the log files.

Note also that under every entry, there is a `Logs:` section which allows you to access the log file of the specific container directly, including the gc log file.

Detecting failures
------------------
In another browser window, go to the monitor page for the first entry (`/sample/i001`): [http://localhost:9000/cp2/monitor](http://localhost:9000/cp2/monitor)

Select `BUSY` and click `Change monitor state`. By doing this, we are simulating the fact that the webapp has detected that it is overloaded and not responding. 2 things should happen (it may take up to 15 seconds to detect the failure):

1. in the agent log file (look at the `tail` command you ran previously), you should see something like:

        2011/01/11 14:57:21.140 WARN [/sample/i001] Server is up but some webapps are busy. Check the log file for errors.

2. on the Dashboard, the first row should be red and the status should read: `ERROR`. If you click on `ERROR` you should see the same message you just saw in the agent log file:

        Server is up but some webapps are busy. Check the log file for errors.

Now go back to the monitor page, select `GOOD` and click `Change monitor state`. Again 2 things should happen (within 15 seconds at most):

1. in the agent log file, you should see something like:

        2011/01/11 15:03:57.082 INFO [/sample/i001] All webapps are up, clearing error status.

2. on the Dashboard, everything should be back to green.

Changing the system
-------------------
Now click the `'System'` tab again.

You should see a table with 1 entry which shows you the systems that you loaded.

Click on the first id. You should now see the json document that you loaded previously. We are going to edit it in place.

The format is an array of entries representing each entry in the system (as explained previously).

In the second entry (look for `"port": 9001`, around the bottom of the text area), change the `contextPath` value to `/cp3`. and click `"Save Changes"`.

Go back to the `Dashboard`.

Note that the second row is now red and the status says `'version MISMATCH'`. If you click on the status it will say:

    initParameters.webapps[0].contextPath:/cp3 != initParameters.webapps[0].contextPath:/cp1

There is a delta: the system in the console is not matching with what is currently deployed. Hence it is red.

Click on `'/sample/i002'` and you land on a filtered view containing only the mountPoint you clicked on.

Choose a plan under `'Deploy: mountPoint [/sample/i002]'`. Note that since there is only 1 entry, choosing `SEQUENTIAL` or `PARALLEL` will have the same effect.

Select the plan and execute it: it first stops the jetty server uninstalls it entirely and reinstall and restart the new one.

When the plan finishes executing, click on `/sample/i002` which is a shortcut to the agent view page.

If you click on `'View Details'` (for `/sample/i002`), you should see the new context path and you can check that it did work by going to: [http://localhost:9001/cp3/monitor](http://localhost:9001/cp3/monitor)  

Now the system (also known as desired state) and the current state match. There is no delta anymore so the console is happy: everything is green.

Reloading the model
-------------------
Manually edit the file: `$GLU_TUTORIAL_ROOT/console-server/glu/repository/systems/sample-webapp-system.json`

Change the contextPath in the very last entry from `/cp4` to `/fail` and save your changes

Go back to the console and reload the model:

Click on the `'Model'` tab and enter

    Json Uri: http://localhost:8080/glu/repository/systems/sample-webapp-system.json

and click `Load`.

Note that you should now have 2 rows that are red: you reloaded the model thus discarding the changes you had made to entry 2 and you changed entry 3.

Click on the `System` tab and then on `Current` subtab and follow the same steps we did before to 'fix' the delta (select deploy in parallel and then execute the plan).

This time around you should see a failure: the last entry failed during boot time (this is artificially triggered by deploying it under `/fail`). Click on the shortcut `/sample/i003` and on the agent view page select the `Logs: more...` entry for `/sample/i003` then click on the first log file called `<yyyy_mm_dd>.stderrout.log`. You should be able to see the log file of the jetty container with the exception of why it failed (something similar to):

    java.lang.RuntimeException: does not boot
      at org.linkedin.glu.samples.webapp.SampleListener.contextInitialized(SampleListener.java:45)
    ...

Using the console cli
---------------------
Click on the `System` tab and on the currently selected system and make sure you change the `/fail` back to `/cp4`.

In the console, click on the `'Plans'` tab and make sure you leave this window visible. Note that at this point you should see the list of all the plans you have already executed including the last one which failed.

Now open a new shell terminal (if you have followed all the instructions so far, you should have a shell terminal window with the tail command in it, this is why we need to open a new one).

Go to the root directory:

    cd $GLU_TUTORIAL_ROOT

Now issue the following command (`-b` is to make it more readable):

    ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -b status

which will display the model that is currently loaded in the console.

    ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -b -l status

which will display the current live model (note that you get a `scriptState` section similar to the one you can see in the console when clicking on the `View Details` link for an entry).

Now we are going to redeploy everything in parallel by issuing:

    ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -a -p redeploy

Please pay attention to the following:

* in the shell window in which you just issued the command there will be a progress bar
* in your web browser you should also see the plan appearing with a progress bar (you can click on the plan to see the details)
* in the shell window with the tail you should see the ouput of the execution

The plan will succeed and you should see:

    100:COMPLETED

unless you did not change the context path to `/cp4` (you may want to try reverting the system to `/fail` as an exercise...).

Try a dry-run mode (`-n`):

    ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -a -n -p redeploy
    
which will display an xml representation of the plan that would be executed if you remove the `-n` option. You should see the 3 entries in the xml output:

    <?xml version="1.0"?>
    <plan name="origin=rest - action=redeploy - filter=all - PARALLEL" savedTime="1294784608849">
      <parallel origin="rest" action="redeploy" filter="all">
        <sequential agent="agent-1" mountPoint="/sample/i001">
          <leaf name="Stop agent-1:/sample/i001 on agent-1" />
          <leaf name="Unconfigure agent-1:/sample/i001 on agent-1" />
          <leaf name="Uninstall agent-1:/sample/i001 from agent-1" />
          <leaf name="Uninstall script for installation agent-1:/sample/i001 on agent-1" />
          <leaf name="Install script for installation agent-1:/sample/i001 on agent-1" />
          <leaf name="Install agent-1:/sample/i001 on agent-1" />
          <leaf name="Configure agent-1:/sample/i001 on agent-1" />
          <leaf name="Start agent-1:/sample/i001 on agent-1" />
        </sequential>
        <sequential agent="agent-1" mountPoint="/sample/i002">
          <leaf name="Stop agent-1:/sample/i002 on agent-1" />
          <leaf name="Unconfigure agent-1:/sample/i002 on agent-1" />
          <leaf name="Uninstall agent-1:/sample/i002 from agent-1" />
          <leaf name="Uninstall script for installation agent-1:/sample/i002 on agent-1" />
          <leaf name="Install script for installation agent-1:/sample/i002 on agent-1" />
          <leaf name="Install agent-1:/sample/i002 on agent-1" />
          <leaf name="Configure agent-1:/sample/i002 on agent-1" />
          <leaf name="Start agent-1:/sample/i002 on agent-1" />
        </sequential>
        <sequential agent="agent-1" mountPoint="/sample/i003">
          <leaf name="Stop agent-1:/sample/i003 on agent-1" />
          <leaf name="Unconfigure agent-1:/sample/i003 on agent-1" />
          <leaf name="Uninstall agent-1:/sample/i003 from agent-1" />
          <leaf name="Uninstall script for installation agent-1:/sample/i003 on agent-1" />
          <leaf name="Install script for installation agent-1:/sample/i003 on agent-1" />
          <leaf name="Install agent-1:/sample/i003 on agent-1" />
          <leaf name="Configure agent-1:/sample/i003 on agent-1" />
          <leaf name="Start agent-1:/sample/i003 on agent-1" />
        </sequential>
      </parallel>
    </plan>

Now try with a filter:

    ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -n -p -s "metadata.cluster='c1'" redeploy

You should now see only 2 entries because the first two have been tagged `c1` for the cluster and the last one is tagged `c2` and we are applying a filter which selects only the entries in cluster `c1`:

    <?xml version="1.0"?>
    <plan name="origin=rest - action=redeploy - filter=metadata.cluster='c1' - PARALLEL" savedTime="1294784656357">
      <parallel origin="rest" action="redeploy" filter="metadata.cluster='c1'">
        <sequential agent="agent-1" mountPoint="/sample/i001">
          <leaf name="Stop agent-1:/sample/i001 on agent-1" />
          <leaf name="Unconfigure agent-1:/sample/i001 on agent-1" />
          <leaf name="Uninstall agent-1:/sample/i001 from agent-1" />
          <leaf name="Uninstall script for installation agent-1:/sample/i001 on agent-1" />
          <leaf name="Install script for installation agent-1:/sample/i001 on agent-1" />
          <leaf name="Install agent-1:/sample/i001 on agent-1" />
          <leaf name="Configure agent-1:/sample/i001 on agent-1" />
          <leaf name="Start agent-1:/sample/i001 on agent-1" />
        </sequential>
        <sequential agent="agent-1" mountPoint="/sample/i002">
          <leaf name="Stop agent-1:/sample/i002 on agent-1" />
          <leaf name="Unconfigure agent-1:/sample/i002 on agent-1" />
          <leaf name="Uninstall agent-1:/sample/i002 from agent-1" />
          <leaf name="Uninstall script for installation agent-1:/sample/i002 on agent-1" />
          <leaf name="Install script for installation agent-1:/sample/i002 on agent-1" />
          <leaf name="Install agent-1:/sample/i002 on agent-1" />
          <leaf name="Configure agent-1:/sample/i002 on agent-1" />
          <leaf name="Start agent-1:/sample/i002 on agent-1" />
        </sequential>
      </parallel>
    </plan>

Finally, issue the command:

    ./bin/console-cli.sh -f glu-dev-1 -u admin -x admin -a -p undeploy

which will undeploy all apps.

Viewing the audit log
---------------------
Go back to the console and click the `'Admin'` tab and then select `'View Audit Logs'`.

You should be able to see all the actions that you have done in the system (usually all actions involving talking to the agent are logged).

The end
-------
You should go back to the original shell terminal (the one where the `tail` command should still be running), issue a CTRL-C to stop the `tail` and issue:

    ./bin/tutorial.sh stop

which will stop the console, the agent and ZooKeeper.

Note that if you did not undeploy the apps, as previously mentionned in the 'console cli' section, they should still be running and this is on purpose: the lifecycle of the apps installed by the glu agent is independent from the agent itself. You can restart the tutorial (`./bin/tutorial.sh start`) and continue where you left off!

That is it for this quick tutorial. You can now check the [documentation](https://github.com/linkedin/glu/wiki).