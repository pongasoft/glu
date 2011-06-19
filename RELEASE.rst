Latest changes
==============

3.0.0.RC3 (2011/06/19)
----------------------
* Fixed `glu-73 <https://github.com/linkedin/glu/issues/73>`_: `Agent upgrade broken due to pid file invalid`

3.0.0.RC2 (2011/06/18)
----------------------
* Fixed `glu-71 <https://github.com/linkedin/glu/issues/71>`_: `Fix plan when bouncing parent/child`
* Fixed `glu-72 <https://github.com/linkedin/glu/issues/72>`_: `Console times out while talking to agent`
* Fixed `glu-73 <https://github.com/linkedin/glu/issues/73>`_: `Agent upgrade broken due to pid file invalid`

3.0.0.RC1 (2011/06/12)
----------------------
* Implemented `glu-63 <https://github.com/linkedin/glu/issues/63>`_: `Handle parent/child relationship in the orchestration engine/console`
* Fixed `glu-21 <https://github.com/linkedin/glu/issues/21>`_: `The model should allow for expressing which state is desired`
* Fixed `glu-33 <https://github.com/linkedin/glu/issues/33>`_: `Mountpoint disappears from agent view when not in model`
* Fixed `glu-18 <https://github.com/linkedin/glu/issues/18>`_: `Grails Runtime Exception (500) when viewing a deployment status` (thanks to Ran!)

This is a pretty big release (hence the bump in the numbering): it contains a full rewrite of the delta, planner and deployer to:

* match the documentation in terms of concepts
* implement the new features: parent/child relationship and any entry state

The delta is now a first class citizen and a new rest API allows to access it: ``/model/delta``

2.4.2 (2011/05/27)
------------------
* Fixed `glu-64 <https://github.com/linkedin/glu/issues/64>`_: `Concurrent deployment of ivy artifacts causes wrong artifact to be downloaded`

2.4.1 (2011/05/24)
------------------
* Fixed `glu-61 <https://github.com/linkedin/glu/issues/61>`_: `ClassCastException when error is a String`
* Fixed `glu-62 <https://github.com/linkedin/glu/issues/62>`_: `"View Full Stack Trace" fails if agent disappears`

2.4.0 (2011/05/20)
------------------
* Added instrumentation for `glu-18 <https://github.com/linkedin/glu/issues/18>`_: `Grails Runtime Exception (500) when viewing a deployment status`
* Implemented `glu-42 <https://github.com/linkedin/glu/issues/42>`_: `Support 'transient' declaration in glu script` (thanks to Andras!)
* Implemented `glu-37 <https://github.com/linkedin/glu/issues/37>`_: `Console should support ETags`
* Fixed `glu-43 <https://github.com/linkedin/glu/issues/43>`_: `IllegalMonitorException thrown by glu script`
* Fixed `glu-45 <https://github.com/linkedin/glu/issues/45>`_: `password.sh requires absolute path`
* Misc.: better handling of logs in the console, improved documentation

2.3.0 (2011/05/13)
------------------
* Implemented `glu-56 <https://github.com/linkedin/glu/issues/56>`_: `Finalize refactoring (#34)`

  * fixed some issues with tagging
  * fixed GString as a key in map issue
  * made some classes more configurable
  * when an entry had only 1 tag, it was being excluded
  * console no longer generates a delta when tags are different!
  * Refactor AgentCli to allow custom configuration

2.2.3 (2011/05/05)
------------------
* Fixed `glu-52 <https://github.com/linkedin/glu/issues/52>`_: `deadlock on agent shutdown`

2.2.2 (2011/05/04)
------------------
* Fixed `glu-51 <https://github.com/linkedin/glu/issues/51>`_: `agent does not recover properly when safeOverwrite fails`

2.2.1 (2011/04/30)
------------------
* Fixed `glu-49 <https://github.com/linkedin/glu/issues/49>`_: `shell.cat is leaking memory`
* Fixed `glu-48 <https://github.com/linkedin/glu/issues/48>`_: `use -XX:+PrintGCDateStamps for gc log`

Also tweaked a couple of parameters for the agent (starting VM now 128M).

2.2.0 (2011/04/22)
------------------
* Implemented `glu-34 <https://github.com/linkedin/glu/issues/34>`_: `Refactor code out of the console`

  The business logic layer of the console has been moved to the orchestration engine area so it is now more easily shareable.

* Massive documentation rewrite which covers the tickets `glu-5 <https://github.com/linkedin/glu/issues/5>`_, `glu-36 <https://github.com/linkedin/glu/issues/36>`_ and `glu-14 <https://github.com/linkedin/glu/issues/14>`_

  Check out the `new documentation <http://linkedin.github.com/glu/docs/latest/html/index.html>`_


2.1.1 (2011/03/04)
------------------
* fixed `glu-31 <https://github.com/linkedin/glu/issues/31>`_: Agent exception when no persistent properties files

2.1.0 (2011/03/01)
------------------
This version is highly recommended for glu-27 specifically which may prevent the agent to recover properly. It affects all previous versions of the agent.

* fixed `glu-26 <https://github.com/linkedin/glu/issues/26>`_: agent cli fails when using spaces
* fixed `glu-27 <https://github.com/linkedin/glu/issues/27>`_: Unexpected exception can disable the agent

2.0.0 (2011/02/14)
------------------
* fixed `glu-22 <https://github.com/linkedin/glu/issues/22>`_: jetty glu script (1.6.0) does not handle restart properly
* Implemented `glu-25 <https://github.com/linkedin/glu/issues/25>`_: add tagging capability

  Dashboard View:

  .. image:: /images/release/v2.0.0/dashboard_tags.png
     :align: center
     :alt: Dashboard View

  Agent View:

  .. image:: /images/release/v2.0.0/agent_view_tags.png
     :align: center
     :alt: Agent View

  Configurable:  

  .. image:: /images/release/v2.0.0/configurable_tags.png
     :align: center
     :alt: Configurable tags

1.7.1 (2011/01/20)
------------------
* workaround for `glu-19 <https://github.com/linkedin/glu/issues/19>`_: New users aren't displayed at ``/console/admin/user/list``
* fixed `glu-20 <https://github.com/linkedin/glu/issues/20>`_: Race condition while upgrading the agent

1.7.0 (2011/01/17)
------------------
* Implemented `glu-12 <https://github.com/linkedin/glu/issues/12>`_: better packaging
* fixed `glu-1 <https://github.com/linkedin/glu/issues/1>`_: Agent name and fabric are not preserved upon restart
* fixed `glu-9 <https://github.com/linkedin/glu/issues/9>`_: Using ``http://name:pass@host:port`` is broken when uploading a model to ``/system/model``
* Implemented `glu-16 <https://github.com/linkedin/glu/issues/16>`_: Use ip address instead of canonical name for Console->Agent communication
* Updated Copyright

1.6.0 (2011/01/11)
------------------
* changed the tutorial to deploy jetty and the sample webapps to better demonstrate the capabilities of glu
* added jetty glu script which demonstrates a 'real' glu script and allows to deploy a webapp container with webapps and monitor them
* added sample webapp with built in monitoring capabilities
* added ``replaceTokens`` and ``httpHead`` to ``shell`` (for use in glu script)
* added ``Help`` tab in the console with embedded forum
* Implemented `glu-12 <https://github.com/linkedin/glu/issues/12>`_ (partially): better packaging
* fixed `glu-13 <https://github.com/linkedin/glu/issues/13>`_: missing connection string in setup-zookeeper.sh

1.5.1 (2010/12/28)
------------------
* fixed `glu-10 <https://github.com/linkedin/glu/issues/10>`_: missing -s $GLU_ZK_CONNECT_STRING in setup-agent.sh (thanks to Ran)
* fixed `glu-11 <https://github.com/linkedin/glu/issues/11>`_: missing glu.agent.port when not using default value

1.5.0 (2010/12/24)
------------------
* fixed `glu-8 <https://github.com/linkedin/glu/issues/8>`_: added support for urls with basic authentication (thanks to Ran)
* added console cli (``org.linkedin.glu.console-cli``) which talks to the REST api of the console
* changed tutorial to add a section which demonstrates the use of the new cli
* added the glu logo (thanks to Markus for the logos)

1.4.0 (2010/12/20)
------------------
* use of `gradle-plugins 1.5.0 <https://github.com/linkedin/gradle-plugins/tree/REL_1.5.0>`_ which now uses gradle 0.9
* added packaging for all clis
* added ``org.linkedin.glu.packaging-all`` which contains all binaries + quick tutorial
* added ``org.linkedin.glu.console-server`` for a standalone console (using jetty under the cover)
* moved keys to a top-level folder (``dev-keys``)
* minor change in the console to handle the case where there is no fabric better
* new tutorial based on pre-built binaries (``org.linkedin.glu.packaging-all``)

1.3.2 (2010/12/07)
------------------
* use of `linkedin-utils 1.2.1 <https://github.com/linkedin/linkedin-utils/tree/REL_1.2.1>`_ which fixes the issue of password not being masked properly
* use of `linkedin-zookeeper 1.2.1 <https://github.com/linkedin/linkedin-zookeeper/tree/REL_1.2.1>`_

1.3.1 (2010/12/02)
------------------
* use of `gradle-plugins 1.3.1 <https://github.com/linkedin/gradle-plugins/tree/REL_1.3.1>`_
* fixes issue in agent cli (exception when parsing configuration)

1.0.0 (2010/11/07)
------------------
* First release
