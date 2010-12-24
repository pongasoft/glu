Introduction
============
The GLU console is the orchestrator of the whole system. It offers both a user friendly web interface and a REST api (for the command line).

Configuration and bootstrap
===========================
Tutorial
--------
Check the tutorial [3-TASTE-OF-GLU-TUTORIAL.md](https://github.com/linkedin/glu/blob/master/3-TASTE-OF-GLU-TUTORIAL.md) for detailed instructions on how to run the tutorial.

For dev
-------
Check the document called [1-QUICK-DEV-SETUP.md](https://github.com/linkedin/glu/blob/master/1-QUICK-DEV-SETUP.md) for detailed instructions on how to set it up

For production
--------------
The console is packaged as a regular webapp (war file) and can simply be dropped in any servlet container (tested with tomcat). In order to run the console requires a configuration file. You can find a sample in the docs folder. There are 3 ways to provide this configuration file to the console:

1. store it under `$HOME/.org.linkedin.glu/glu-console-webapp.groovy`
2. store it under `conf/glu-console-webapp.groovy` (relative to wherever the VM user.dir is set to)
3. pass a system property `-Dorg.linkedin.glu.console.config.location=/fullpath/to/file`

First bootstrap
---------------
The very first time, the console is started, it will create an admin user. Log in as this user:

    username: admin
    password: admin

Then click on the `'admin'` tab (not the one called `'Admin'`) and click `'Manage your credentials'`.

**It is strongly recommended you immediately change the admin password!**

User management
===============
There are 2 ways to manage user.

LDAP
----
If you define an ldap section in the configuration file (see [console.groovy](https://github.com/linkedin/glu/raw/master/docs/console.groovy) example), then the console will automatically allow any user who has the correct ldap credentials to login. If the user has never logged in before, a new account will be automatically created and the user will have the role `USER` which gives him read access mostly (and limited access to log files). This allows any user to login to the console without any administrator intervention.

Manual user management
----------------------
Whether you use LDAP or not you can always use this method. If you don't use LDAP then it is your only choice. On the `Admin` tab, you can create a new user by giving it a user name and a password. You can also assign roles to a user:

* `USER`: mostly read access (limited access to some log files (cannot go anywhere on the filesystem))
* `RELEASE`: most of the traditional release actions (like starting and stopping entries…)
* `ADMIN`: administrative role (create user, assign roles…)
* `RESTRICTED`: if you want to ban a user from the console

*Note that no matter what the role the user in, actions taken are logged in the audit log that can be viewed from the `Admin` tab.*

Password management
-------------------
Even if you use LDAP, a user can assign himself a console password (useful if the password need to be stored in scripts for example).

Console tabs
------------
Documentation coming soon. In the meantime, take a look at [3-TASTE-OF-GLU-TUTORIAL.md](https://github.com/linkedin/glu/blob/master/3-TASTE-OF-GLU-TUTORIAL.md)

REST API
========
Security / Authorization
------------------------
The security model is simply implementing http basic authorization:

every request needs to come with a Authorization header with the following format:

    Authorization: Basic <base64(username:password)>

*The username and password are only slightly obfuscated (base 64) but it is not an issue because the cli talks to the console over https and as a result the headers are never traveling over an insecure channel.*

API
---
Main URI: `/console/rest/v1/<fabric>` (all the URIs in the following tables starts with the main URI)

<table>
  <tr>
    <th>Method</th>
    <th>URI</th>
    <th>Description</th>
    <th>Request</th>
    <th>Response</th>
    <th>Status</th>
  </tr> 
  <tr> 
    <td>GET</td>
    <td>/plans</td>
    <td>List all the plans</td>
    <td>N/A</td>
    <td><li>TBD</td>
  </tr>
  <tr> 
    <td>POST</td>
    <td>/plans</td>
    <td>Create a plan</td>
    <td>view details below for the content of body of the POST</td>
    <td>
    <ul>
    <li>201 (CREATED) with Location header to access the plan (/plan/&lt;planId&gt;)
    <li>204 (NO CONTENT) when no plan created because there is nothing to do
    </ul>
    </td>
  </tr>
  <tr> 
    <td>GET</td>
    <td>/plan/&lt;planId&gt;</td>
    <td>View the plan (as an xml document)</td>
    <td>N/A</td>
    <td>
      <ul>
      <li>200 (OK) with an xml representation of the plan
      <li>204 (NOT_FOUND) if no such plan
      </ul>
    </td>
  </tr>
  <tr> 
    <td>POST</td>
    <td>/plan/&lt;planId&gt;/execution</td>
    <td>Executes the plan</td>
    <td>N/A</td>
    <td>
      <ul>
      <li>201 (CREATED) with Location header to access the plan execution (/plan/&lt;planId&gt;/execution/&lt;executionId&gt;). Note that it is a non blocking call and it returns right away and you can check the progress thus allowing to have a progress bar!
      <li>204 (NOT_FOUND) if no such plan
      </ul>
    </td>
  </tr>
  <tr> 
    <td>HEAD</td>
    <td>/plan/&lt;planId&gt;/execution/&lt;executionId&gt;</td>
    <td>Returns the status of the execution</td>
    <td>N/A</td>
    <td>
      <ul>
      <li>200 (OK) with X-LinkedIn-GLU-Completion header with value:
      <li>if plan non completed, percentage completion (ex: 87)
      <li>if completed: 100:<completion status> (ex:100:FAILED or 100:COMPLETED)
      <li>204 (NOT_FOUND) if no such execution
      </ul>
    </td>
  </tr>
  <tr> 
    <td>GET</td>
    <td>/plan/&lt;planId&gt;/execution/&lt;executionId&gt;</td>
    <td>Returns the execution as an xml document</td>
    <td>N/A</td>
    <td>
      <ul>
      <li>200 (OK) with an xml representation of the execution (equivalent to the view in the console)
      <li>204 (NOT_FOUND) if no such execution
      </ul>
    </td>
  </tr>
  <tr> 
    <td>DELETE</td>
    <td>/plan/&lt;planId&gt;/execution/&lt;executionId&gt;</td>
    <td>Aborts the execution</td>
    <td>N/A</td>
    <td>TBD</td>
  </tr>
  <tr> 
    <td>POST</td>
    <td>/system/model</td>
    <td>Loads the manifest in the console</td>
    <td>Body contains a query string rootUrl=file:/xxx&amp;release=yyy</td>
    <td>
      <ul>
      <li>201 (CREATED) when loaded successfully
      <li>204 (NO_CONTENT) if model was loaded successfully and is equal to the previous one
      <li>400 (BAD_REQUEST) if rootUrl is not a file (note that it is temporary as well)
      <li>404 (NOT_FOUND) when error (note error handling needs to be revisited)
      </ul>
    </td>
  </tr>
  <tr> 
    <td>GET</td>
    <td>/system/model</td>
    <td>Retrieves the current loaded model (aka 'desired' state)</td>
    <td>
      optional request parameters:
      <ul>
      <li>prettyPrint=true for human readable output
      <li>systemFilter=... for filtering (see below for the syntax)
      <li>legacy=true for keeping the legacy section in the output
      </ul>
    </td>
    <td>
      <ul>
      <li>200 (OK) with a json representation of the model
      </ul>
    </td>
  </tr>
  <tr> 
    <td>GET</td>
    <td>/system/live</td>
    <td>Retrieves the current live model coming from ZooKeeper (aka current state)</td>
    <td>
      optional request parameters:
      <ul>
      <li>prettyPrint=true for human readable output
      <li>systemFilter=... for filtering (see below for the syntax)
      <li>legacy=true for keeping the legacy section in the output
      </ul>
    </td>
    <td>
      <ul>
      <li>200 (OK) with a json representation of the live model (note that the metadata contains information like currentState
      </ul>
    </td>
  </tr>
</table>


Filter syntax
-------------
The filter syntax is a dsl and allows you to filter by any field of the entry using a dotted notation. An entry looks like this (you get this when doing `/system/live` or `/system/model`):

    {
      "agent": "ei2-app3-zone5.qa",
      "initParameters": {
        "skeleton": "ivy:/com.linkedin.network.container/container-jetty/0.0.007-RC1.1",
        "wars": "ivy:/com.linkedin.jobs/jobs-war/0.0.504-RC3.2612|jobs"
      },
      "metadata": {
        "container": {
          "kind": "servlet",
          "name": "jobs-server"
        },
        "currentState": "running",
        "modifiedTime": 1284583501275,
        "product": "network",
        "version": "R950"
      },
      "mountPoint": "/jobs-server/i001",
      "script": "ivy:/com.linkedin.glu.glu-scripts/glu-scripts-jetty/3.0.0/script"
    }

The dsl has the following syntax:

    and / or / not => to do logic
    <dotted notation>='<value>' => to express the matching criteria

Examples:

1. Only container 'jobs-server'

        metadata.container.name='jobs-server'

2. Container is 'jobs-server' or 'activemq'

        or { 
          metadata.container.name='jobs-server'
          metadata.container.name='activemq'
        }

        // can be compacted on 1 line as:
        or{metadata.container.name='jobs-server';metadata.container.name='activemq'}

3. All containers that are not running (on live system only of course)

        not {
          metadata.currentState='running'
        }

        // can be compacted on 1 line as:
        not{metadata.currentState='running'}

4. All containers not running on agent ei2-app3-zone5.qa (on live system only of course)

        not {
          metadata.currentState='running'
        }
        agent='ei2-app3-zone5.qa'

        // is 100% equivalent to:
        and {
          not {
            metadata.currentState='running'
          }
          agent='ei2-app3-zone5.qa'
        }

        // can be compacted on 1 line as:
        not{metadata.currentState='running'};agent='ei2-app3-zone5.qa'
	
The REST api is expecting the filter as a query parameter (`systemFilter`) and it needs to be properly url encoded. For example it should be `systemFilter=not%7bmetadata.currentState%3d'running'%7d`

Representing a plan
===================
Query String based
------------------
The POST you issue must be of Content-Type `application/x-www-form-urlencoded`

The body then contains a query string with the following parameters:

<table>
  <tr>
    <th>name</th>
    <th>value</th>
    <th>required</th>
    <th>example</th>
  </tr>
  <tr>
    <td>planAction</td>
    <td>start, stop, bounce, deploy, undeploy, redeploy</td>
    <td>Yes</td>
    <td>planAction=start</td>
  </tr> 
  <tr>
    <td>systemFilter</td>
    <td>a system filter as described in the previous section (remember that it <strong>must</strong> be properly url encoded.</td>
    <td>No. It simply means don't filter at all.</td>
    <td>systemFilter=and%7bagent%3d'ei2-app3-zone5.qa'%7d</td>
  </tr> 
  <tr>
    <td>order</td>
    <td>parallel or sequential</td>
    <td>No. Default to sequential</td>
    <td>order=parallel</td>
  </tr> 
</table>

Json/DSL Based
==============
Coming soon