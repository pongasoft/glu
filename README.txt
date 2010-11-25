1. Introduction
---------------
GLU is a deployment automation platform. It has been built and deployed at LinkedIn in early 2010
and then released as open source in November 2010. The goal is to be able to automate the
deployment of any kind of applications accross many nodes. Although written in groovy/java, the
type of applications that can be deployed through GLU is not limited to java applications. GLU is
a platform and the way it was architected and designed allows you to pick and choose which part you 
want to use... Check the docs folder (and soon the wiki) for more documentation on GLU.

2. Compilation
--------------
In order to compile the code you need
* gradle 0.9-rc2 (http://www.gradle.org/)
* groovy 1.7.5 (http://groovy.codehaus.org/) (only for the console)
* grails 1.3.5 (http://www.grails.org/) (only for the console)

Before doing anything go to
console/org.linkedin.glu.console-webapp

and issue:

grails upgrade

At the top simply run

gradle test

which should compile and run all the tests.

Note: if you do not run the 'grails upgrade' command, you may see this messages:
Plugin [shiro-1.1-SNAPSHOT] not installed. ...
Plugin [yui-2.7.0.1] not installed. ...
Plugin [hibernate-1.3.5] not installed. ...
Plugin [tomcat-1.3.5] not installed. ...

3. IDE Support
--------------
You can issue the command (at the top)

gradle idea

which will use the gradle IDEA plugin to create the right set of modules in order to open the
project in IntelliJ IDEA.

4. Directory structure
----------------------
* agent/org.linkedin.glu.agent-api
Agent api (like Agent class)

* agent/org.linkedin.glu.agent-impl
Implementation of the agent

* agent/org.linkedin.glu.agent-rest-resources
REST resources (endpoint) (used in the agent)

* agent/org.linkedin.glu.agent-rest-client
REST client (which talks to the resources) (used in both agent-cli and console-webapp)

* agent/org.linkedin.glu.agent-cli
The command line which can talk to the agent directly
gradle package // to create the package
gradle package-install // to install locally (for dev)

* agent/org.linkedin.glu.agent-tracker
Listens to ZooKeeper events to track the agent writes (used in console-webapp).

* agent/org.linkedin.glu.agent-server-impl
Contains the implementation of the agent server cli.

* agent/org.linkedin.glu.agent-server-upgrade
Create the upgrade package (to be used when uprading an already installed agent).
gradle package // create the upgrade package

* agent/org.linkedin.glu.agent-server
The actual server
gradle package // create the package

gradle package-install // to install locally (for dev)
gradle setup // setup dev fabric and keys in zookeeper (used in conjunction with gradle install)

gradle setup-x-y // setup x agents in y fabrics: automatically setup zookeeper with the right set of
                    data and create x agents package with a wrapper shell script to start them all

gradle clean-setup // to delete the setup

* console/org.linkedin.glu.console-webapp
The console webapp (grails application).

gradle lib // compile all the dependencies and put them in the lib folder
grails run-app // after running gradle lib, you can simply run grails directly as it will use the
               // libraries in lib to boot the app

5. Installing/Running locally
-----------------------------
Check the 1-QUICK-DEV-SETUP.txt document followed by the 2-QUICK-TUTORIAL.txt for a quick
walkthrough the console.

6. Build configuration
----------------------
The project uses the 'org.linkedin.userConfig' plugin and as such can be configured
See https://github.com/linkedin/gradle-plugins/blob/master/README.txt for details

Example:
~/.userConfig.properties
top.build.dir="/Volumes/Disk2/deployment/${userConfig.project.name}"
top.install.dir="/export/content/${userConfig.project.name}"
top.release.dir="/export/content/repositories/release"
top.publish.dir="/export/content/repositories/publish"

7. Support and discussion
-------------------------
The forum for this project can be found at http://glu.977617.n3.nabble.com/