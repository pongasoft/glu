Release Notes
=============

.. _glu-5.4.1:

5.4.1 (2013/12/06)
------------------

This release is a small bug fix release.

* Fixed `glu-254 <https://github.com/pongasoft/glu/issues/254>`_: `After stop, the start action is not displayed`
* Fixed `glu-255 <https://github.com/pongasoft/glu/issues/255>`_: `Setup generates myid file in wrong location for ZooKeeper cluster`
* Fixed `glu-256 <https://github.com/pongasoft/glu/issues/256>`_: `Wrong connection string when multiple ZooKeepers`


.. _glu-5.4.0:

5.4.0 (2013/11/27)
------------------

This release contains a few bug fixes and small features. Only the console is affected, so no need to upgrade the agents.

New and noteworthy
^^^^^^^^^^^^^^^^^^
* an admin user can now reset passwords
* passwords are now salted and using bcrypt for hashing which makes it way more secure (note that current passwords are *not* changed and you will need to change your password to have the new feature kick in).
* system filters can now contain ``[x]`` in their syntax like ``initParameters.webapps[1].contextPath`` (check the :ref:`filtering section <goe-filter-syntax>`)

Tickets
^^^^^^^
* Fixed `glu-247 <https://github.com/pongasoft/glu/issues/247>`_: `Glu applies variable expansion to local filenames`
* Implemented `glu-248 <https://github.com/pongasoft/glu/issues/248>`_: `Add support for array items in filters`
* Fixed `glu-249 <https://github.com/pongasoft/glu/issues/249>`_: `Wrong fabric selected when multiple windows are opened`
* Implemented `glu-250 <https://github.com/pongasoft/glu/issues/250>`_: `Allow admin user to reset other users passwords`
* Implemented `glu-251 <https://github.com/pongasoft/glu/issues/251>`_: `Seed the passwords with the user name`
* Fixed (+ debug) `glu-252 <https://github.com/pongasoft/glu/issues/252>`_: `Problem starting Jetty`
* Implemented `glu-253 <https://github.com/pongasoft/glu/issues/253>`_: `Add "Reconfigure" button to agent view`

.. _glu-5.3.1:

5.3.1 (2013/10/03)
------------------

This release contains a minor bug fix. Only the console is affected, so no need to upgrade the agents.

* Fixed `glu-242 <https://github.com/pongasoft/glu/issues/242>`_: `NPE when selecting "bounce" or "stop" plans with a tags filter`
* Fixed `glu-246 <https://github.com/pongasoft/glu/issues/246>`_: `Name of a plan generated from agent view contains __role in it`

.. _glu-5.3.0:

5.3.0 (2013/09/27)
------------------

This release contains a major overhaul of the directory/file listing feature for a given agent.

.. tip::
   In order to benefit fully from the new feature, the agent needs to be upgraded (check the :ref:`migration steps <migration-guide-5.2.0-5.3.0>` for details).

New and noteworthy
^^^^^^^^^^^^^^^^^^
* It is now possible to continuously tail any file located on any agent (initial tail size and refresh rate are both :ref:`configurable <console-configuration-tail>`) as well as view it in the browser or download the content. The directory listing view has also been enhanced to add the same functionality.
* glu scripts now have access to the ZooKeeper instance used by the agent (using the ``agentZooKeeper`` property).
* All URLs in the console are now `enhanced` to include the fabric which makes them copy/paste friendly.
* The agent cli now supports a different state machine (``--start`` (``-S``) and ``--install`` (``-I``) behave according to the state machine definition).
* The max form post size is now configurable (in the console meta model)::

    configTokens: [
      maxFormConfigSize: '500k'
    ]

* The full package size has been reduced.

Tickets
^^^^^^^
* Implemented `glu-153 <https://github.com/pongasoft/glu/issues/153>`_: `Make URLs copy/paste friendly`
* Implemented `glu-183 <https://github.com/pongasoft/glu/issues/183>`_: `Add support for different state machine in agent-cli`
* Implemented `glu-187 <https://github.com/pongasoft/glu/issues/187>`_: `Add "tail -f" for log files`
* Implemented `glu-240 <https://github.com/pongasoft/glu/issues/240>`_: `Add ZooKeeper access from glu script`
* Fixed `glu-241 <https://github.com/pongasoft/glu/issues/241>`_: `inconsistent use of java vs $JAVA_HOME/bin/java`
* Fixed `glu-242 <https://github.com/pongasoft/glu/issues/242>`_: `NPE when selecting "bounce" or "stop" plans with a tags filter`
* Implemented `glu-243 <https://github.com/pongasoft/glu/issues/243>`_: `Remove redundant/irrelevant data in package (all)`
* Fixed `glu-245 <https://github.com/pongasoft/glu/issues/245>`_: `Exception: Form Too large`

.. _glu-5.2.0:

5.2.0 (2013/08/14)
------------------

This release contains a few bug fixes and enhancements.

New and noteworthy
^^^^^^^^^^^^^^^^^^
* you can configure the agent outside the (upgrade) tarball (although since 5.1.0 this is less useful): ``$AGENT_ROOT/conf/pre_master_conf.sh`` and ``$AGENT_ROOT/conf/post_master_conf.sh``
* you can change the :ref:`session timeout <console-configuration-session-timeout>` in the console
* you can use a :ref:`json groovy dsl <static-model-json-groovy-dsl>` for the system model (check the `repository <https://github.com/pongasoft/glu/tree/master/console/org.linkedin.glu.console-server/src/cmdline/resources/glu/repository/systems>`_ for examples on how to use the dsl).
* you can configure the agent with a shared class loader to minimize memory footprint
* the agent is now properly registered in ZooKeeper **after** opening the rest api

Tickets
^^^^^^^
* Implemented `glu-215 <https://github.com/pongasoft/glu/issues/215>`_: `Add ability to configure agent outside the "tarball"`
* Fixed `glu-220 <https://github.com/pongasoft/glu/issues/220>`_: `java.lang.IllegalArgumentException: not a boolean : [:]`
* Fixed `glu-222 <https://github.com/pongasoft/glu/issues/222>`_: `Only Admin users can tail Commands output`
* Fixed `glu-224 <https://github.com/pongasoft/glu/issues/224>`_: `StringIndexOutOfBoundsException when listing models`
* Implemented `glu-225 <https://github.com/pongasoft/glu/issues/225>`_: `Allow to configure session timeout in console`
* Fixed `glu-227 <https://github.com/pongasoft/glu/issues/227>`_: `Glu Console Fabric menu is too large for users' screen resolution`
* Implemented `glu-228 <https://github.com/pongasoft/glu/issues/228>`_: `Reconfigure Plan`
* Merged `glu-230 <https://github.com/pongasoft/glu/issues/230>`_: `Add 'agents' command to the console-cli tool` (Thank you sodul)
* Fixed `glu-232 <https://github.com/pongasoft/glu/issues/232>`_: `High overhead for each mountpoint on agent`
* Fixed `glu-235 <https://github.com/pongasoft/glu/issues/235>`_: `in agent => java.lang.IllegalStateException: Can't overwrite cause`
* Fixed `glu-236 <https://github.com/pongasoft/glu/issues/236>`_: `gradle setup no longer working in agent-server`
* Fixed `glu-237 <https://github.com/pongasoft/glu/issues/237>`_: `Agent is "up" before being accessible via rest`
* Implemented `glu-238 <https://github.com/pongasoft/glu/issues/238>`_: `Add json groovy dsl for static model`

.. _glu-5.1.0:

5.1.0 (2013/07/20)
------------------

This release contains a brand new way of configuring and installing glu which should make it much easier to deploy glu in production. The documentation has been enhanced throughout to reflect the changes, including several new pages (:doc:`easy-production-setup`, :doc:`meta-model`, :doc:`glu-config`, :doc:`setup-tool`, :doc:`migration-guide`).

.. note::
   Although this release contains a huge number of changes (from github stats: *66 commits, 197 files changed, 13,791 additions, 2,887 deletions*), glu per se has not really changed: only the glu setup is different.

.. tip::
   If you are already familiar with glu, check the (new) :ref:`migration steps <migration-guide-5.0.0-5.1.0>` section. In particular the :ref:`migration-guide-5.0.0-5.1.0-quick-and-easy` section can allow you to quickly recreate a more familiar structure.

* Implemented `glu-58 <https://github.com/pongasoft/glu/issues/58>`_: `Easy production setup`
* Fixed `glu-142 <https://github.com/pongasoft/glu/issues/142>`_: `Reliance on -z flag whilst using the tar command`
* Fixed `glu-231 <https://github.com/pongasoft/glu/issues/231>`_: `Cannot start console in development mode (grailsw) with java 1.7 v 25`

.. _glu-5.0.0:

5.0.0 (2013/04/23)
------------------

This release is the very first release that requires java 1.7. As noted in the previous release notes, in order to upgrade glu from an earlier release, you should first upgrade to the `4.7.x` line (which works both with java 1.6 and java 1.7), then upgrade to the 5.x.y line.

There is no new features or bug fixes since `4.7.1`.

* Implemented `glu-218 <https://github.com/pongasoft/glu/issues/218>`_: `Migrate to jdk1.7`

.. note:: This version comes with some structural changes that you should be aware of:

          * the glu binaries (tar files) are now hosted on bintray under the `glu <https://bintray.com/pkg/show/general/pongasoft/glu/releases>`_ repository
          * the source code has been moved under a new home on github: `pongasoft/glu <http://www.github.com/pongasoft/glu>`_
          * the documentation also has been moved under a new home on github `pongasoft.github.io/glu <http://pongasoft.github.io/glu/docs/latest/html/index.html>`_
          * the glu jar files (which you should normally not care about unless you are extending glu in some shape or form) are also hosted on bintray/jcenter::

               mavenRepo url: 'http://jcenter.bintray.com'

.. _glu-4.7.2:

4.7.2 (2013/05/09)
------------------

This release is a bug fix release.

* Fixed `glu-223 <https://github.com/pongasoft/glu/issues/223>`_: `jdk1.7 workaround does not "persist" in 4.7.1`

.. _glu-4.7.1:

4.7.1 (2013/04/16)
------------------

This release is a bug fix release.

.. warning:: this version should be used instead of 4.7.0 due to the `critical bug <https://github.com/pongasoft/glu/issues/214>`_

* Implemented `glu-205 <https://github.com/pongasoft/glu/issues/205>`_: `Make zookeeperRoot configurable in console`
* Implemented `glu-206 <https://github.com/pongasoft/glu/issues/206>`_: `Allow agent to bind to a specified network interface`
* Fixed `glu-207 <https://github.com/pongasoft/glu/issues/207>`_: `Agent fails to restart if cannot reload scripts`
* Implemented `glu-210 <https://github.com/pongasoft/glu/issues/210>`_: `Agent not recreating ephemeral node after ZK outage`
* Fixed `glu-211 <https://github.com/pongasoft/glu/issues/211>`_: `Add Support to Users for Default Fabric`
* Fixed `glu-212 <https://github.com/pongasoft/glu/issues/212>`_: `agent returns a 405 when Accept header with a value of 'application/json' is specified`
* Fixed `glu-213 <https://github.com/pongasoft/glu/issues/213>`_: `Glu console does more absolute url redirects with 4.7.0`
* Fixed `glu-214 <https://github.com/pongasoft/glu/issues/214>`_: `Upgrading from 4.6.2 to 4.7.0 breaks all user roles`

.. note:: Due to the fact that `glu-210 <https://github.com/pongasoft/glu/issues/210>`_ is (most likely) a race condition, a monitor has been added to the agent in order to detect (and correct) the situation. This will hopefully provide more insight into the problem. In order to disable the monitor, set the property ``glu.agent.zkMonitor.enabled`` to ``false``.

.. _glu-4.7.0:

4.7.0 (2013/04/02)
------------------

.. warning:: * 2013/04/15 update: a `critical bug <https://github.com/pongasoft/glu/issues/214>`_ has been found in this release if you are using the ``console.datasource.table.user.mapping`` configuration option (which allows you to rename the ``user`` table). If you are, do not upgrade to this version.
             * 2013/04/16 update: the bug has been resolved in version 4.7.1

This release contains a major upgrade of all the libraries used by glu. The purpose of this release is to allow glu to finally be able to run under any java VM including java 1.7 (as java 1.6 is now no longer supported by Oracle). Here are the requirements in terms of VM version(s):

+----------------+-----------------------------------+
|glu version     |java version(s)                    |
+================+===================================+
| 5.0.0+         |java 1.7                           |
+----------------+-----------------------------------+
| 4.7.x          |java 1.6 (any VM) or java 1.7      |
+----------------+-----------------------------------+
| 4.6.x and below|java 1.6 (with Sun/Oracle VM only!)|
+----------------+-----------------------------------+

The next major release of glu (5.0.0) will support java 1.7 only. As a result, the 4.7.x line is also the recommended upgrade path from any prior version of glu since it will be the only line that will support both 1.6 and 1.7 java VMs!

The code in glu has not changed much in this release, but it contains a whole set of new libraries. Although glu has been tested thoroughly (including longevity tests which uncovered some memory leak in the latest version of groovy!), you should use caution when upgrading to this version and make sure you test it on a small set of nodes prior to making a big push.

.. note:: One notable change is the use of the latest version of ZooKeeper (3.4.5). Although the ZooKeeper servers do not need to be upgraded (backward compatible), it is advised to upgrade them and you should follow the procedure described on the ZooKeeper web site.

* Fixed `glu-74 <https://github.com/pongasoft/glu/issues/74>`_: `NPE when opening the Dashboard in the tutorial`
* Implemented `glu-109 <https://github.com/pongasoft/glu/issues/109>`_: `Make build compatible with Gradle version 1.0-milestone-3`
* Fixed `glu-123 <https://github.com/pongasoft/glu/issues/123>`_: `Hammering console with several simple REST requests in parallel leads to strange groovy error`
* Implemented `glu-135 <https://github.com/pongasoft/glu/issues/135>`_: `Migrate to latest grails/groovy`
* Fixed `glu-143 <https://github.com/pongasoft/glu/issues/143>`_: `Removal of hardcoded 'java' command from zk.sh`
* Implemented `glu-148 <https://github.com/pongasoft/glu/issues/148>`_: `Upgrade to gradle 1.0`
* Fixed `glu-193 <https://github.com/pongasoft/glu/issues/193>`_: `Add support for nesting /console/ in a sub url in console-cli.py and PEP-8 Python style compliance`
* Fixed `glu-199 <https://github.com/pongasoft/glu/issues/199>`_: `Cannot resolve reference to bean LightUserCustomDeltaDefinitionDomainClass`
* Fixed `glu-201 <https://github.com/pongasoft/glu/issues/201>`_: `Incorrect plan generated from console-cli`
* Fixed `glu-208 <https://github.com/pongasoft/glu/issues/208>`_: `When no (console) plugins available, getting a warning message from jetty (8)`

Thanks to `JProfiler <http://www.ej-technologies.com/products/jprofiler/overview.html>`_ for providing a free license key in order to troubleshoot (and very quickly identify!) the memory leak in groovy.

.. _glu-4.6.2:

4.6.2 (2013/01/21)
------------------

This release is a bug fix release only.

* Fixed `glu-195 <https://github.com/pongasoft/glu/issues/195>`_: `Very long lines in an application log are loaded surprisingly slow when browsing from Console`
* Fixed `glu-196 <https://github.com/pongasoft/glu/issues/196>`_: `DisabledFeatureProxy should proceed hashCode and equals methods to avoid breaking Spring ApplicationContext`
* Fixed `glu-197 <https://github.com/pongasoft/glu/issues/197>`_: `Once a fabric is deleted cannot create a new fabric with the same name`
* Fixed `glu-198 <https://github.com/pongasoft/glu/issues/198>`_: `Child not able to generate Parent Plan`

.. _glu-4.6.1:

4.6.1 (2012/12/21)
------------------

This release is essentially a bug fix release with a couple of minor enhancements to the agent api.

* Fixed `glu-134 <https://github.com/pongasoft/glu/issues/134>`_: `Race condition between agent.waitForState and ZooKeeper state`
* Fixed `glu-177 <https://github.com/pongasoft/glu/issues/177>`_: `No plan generated when mountpoint not deployed with REST api`
* Fixed `glu-178 <https://github.com/pongasoft/glu/issues/178>`_: `Install script agent REST api not handling path correctly`
* Fixed `glu-181 <https://github.com/pongasoft/glu/issues/181>`_: `Using an unknown fabric in console REST call works`
* Fixed `glu-182 <https://github.com/pongasoft/glu/issues/182>`_: `Delta engine reports success when agents are missing`
* Implemented `glu-185 <https://github.com/pongasoft/glu/issues/185>`_: `Add "pwd" to generic shell.exec command`
* Implemented `glu-191 <https://github.com/pongasoft/glu/issues/191>`_: `Add rootShell to GluScript`
* Implemented `glu-192 <https://github.com/pongasoft/glu/issues/192>`_: `Add "env" to generic shell command`
* Implemented `glu-193 <https://github.com/pongasoft/glu/issues/193>`_: `Add support for nesting /console/ in a sub url in console-cli.py and PEP-8 Python style compliance` (Thanks to Stéphane)
* Implemented `glu-194 <https://github.com/pongasoft/glu/issues/194>`_: `Find a new "Downloads" space`

.. note:: Due to `github deprecating the Download/Upload feature <https://github.com/blog/1302-goodbye-uploads>`_, the binary release has been moved to a new `location <http://www.pongasoft.com/glu/downloads/>`_.

.. _glu-4.6.0:

4.6.0 (2012/11/18)
------------------

This release contains the new feature ``commands`` which extends glu capabilities in order to execute an arbitrary (unix/shell) command on any node. One way to think about it is executing a remote command using a REST api rather than ssh. It provides the added benefit that all commands executed this way are following the `standard` authentication and auditing path followed by deployments. The ``All commands`` view shows you instantly what is (or has been) executing on various agents, thus allowing you to immediately get a sense of what other `actions` (besides deployments) have been performed on an agent for tracking and/or diagnosing purposes.

.. note:: This feature may be disabled entirely. If you are using your own configuration file it will have to be enabled explicitely. If you use the configuration files coming with the distribution, it is enabled by default.

* Implemented `glu-166 <https://github.com/pongasoft/glu/issues/166>`_: `Allow agent to run any kind of command`
* Implemented `glu-169 <https://github.com/pongasoft/glu/issues/169>`_: `Add a shell.exec api to also expose stderr`
* Implemented `glu-170 <https://github.com/pongasoft/glu/issues/170>`_: `Add "start" in the plans subtab`


4.5.2 (2012/10/31)
------------------

.. warning:: This release contains a critical bug fix and is highly recommended. 

Only the agent needs to be upgraded. The issue fixed is the ability to talk to the agent over ssl without any certificate (the agent is not honoring the ``needClientAuth`` flag).

.. note:: In order to know if you are affected by this issue and you should upgrade, follow the 
          simple steps:

          * if you are running your agent with ``sslEnabled`` set to ``false`` then you are not affected
          * otherwise run the following command::

             curl -v -k https://<agentIP>:<agentPort>/agent

            * if you receive an error message then you are not affected by the issue
            * if you do not receive an error message and simply an OK (200) response from the agent (which should be 
              a json document with the list of all mount points), then you are affected and it is highly 
              recommended to upgrade

* Fixed `glu-175 <https://github.com/pongasoft/glu/issues/175>`_: `client auth not working for agent with ssl enabled`


4.5.1 (2012/09/23)
------------------

This release essentially contains some minor fixes. The deployment view has a subtle change: all (leaf) steps are now links: when you hover your mouse over one of them you can click on it and it is a shortcut to the agent view page (fix for glu-163).

* Fixed `glu-155 <https://github.com/pongasoft/glu/issues/155>`_: `shell.exec leaks file descriptors`
* Fixed `glu-163 <https://github.com/pongasoft/glu/issues/163>`_: `Deployment view does not have agent links when model has parents`
* Fixed `glu-165 <https://github.com/pongasoft/glu/issues/165>`_: `symlinks are not being shown in the console`


4.5.0 (2012/08/15)
------------------

This release contains a refactoring of the authorization framework in order to be able to change the authorization levels via :ref:`configuration <console-configuration-security-levels>` as well as being entirely customizable via :ref:`plugins <goe-plugins>`.

.. warning:: The property ``console.authFilters.rest.write.roleName`` has been removed from the configuration file. Instead you can define your own level per REST call.

.. note:: The prefixes ``/release`` and ``/admin`` which used to determine the level of authorization in the various URLs, have been removed since they do not serve this purpose anymore and as a result could be very confusing.

* Implemented `glu-140 <https://github.com/pongasoft/glu/issues/140>`_: `Revisit permission/authorization system`
* Fixed `glu-152 <https://github.com/pongasoft/glu/issues/152>`_: `NPE when no Step in execution plan`
* Fixed `glu-154 <https://github.com/pongasoft/glu/issues/154>`_: `make console-cli return 1 on failure` (Thanks to Stéphane)


4.4.2 (2012/07/26)
------------------

This release contains mostly bug fixes and minor improvements

* Fixed `glu-111 <https://github.com/pongasoft/glu/issues/111>`_: `Console server initialization fails with Oracle 11g` (Thanks to Chris for the tip)
* Implemented `glu-141 <https://github.com/pongasoft/glu/issues/141>`_: `Add documentation about mysql configuration`
* Fixed `glu-144 <https://github.com/pongasoft/glu/issues/144>`_: `Addition of pre-setup Java version check` (Thanks to Stuart)
* Implemented `glu-147 <https://github.com/pongasoft/glu/issues/147>`_: `Allow to limit (optionally) massive parallel deployment`
* Fixed `glu-151 <https://github.com/pongasoft/glu/issues/151>`_: `Allow '_' in mountPoint`

4.4.1 (2012/07/04)
------------------

This release contains a critical bug fix

* Fixed `glu-150 <https://github.com/pongasoft/glu/issues/150>`_: `Cannot change password`

4.4.0 (2012/04/28)
------------------

This release further improves the performance of the previous one.

.. warning:: Unlike the previous release, for performance reasons, the default is now to compute the checksum system model using jackson output. 
             As a result, the **same** model loaded prior to 4.4.0 will have a different checksum. 
             If this turns out to be an issue in your case (which should be extremely unlikely if you usually "move forward"), then you can disable this behavior and revert back to the previous computation using the following configuration property in your (console) configuration file::

                console.systemModelRenderer.maintainBackwardCompatibilityInSystemId=true

List of tickets:

* Fixed `glu-139 <https://github.com/pongasoft/glu/issues/139>`_: `Fix documentation for ZooKeeper URL`
* Implemented `glu-138 <https://github.com/pongasoft/glu/issues/138>`_: `Make pretty printing configurable`
* Merged `glu-137 <https://github.com/pongasoft/glu/issues/137>`_: `Place focus in username input text field on page load` (thanks to Tom)

4.3.1 (2012/03/31)
------------------

Mostly a performance improvement release: use of the jackson library to enhance memory consumption and speed particularly visible on large system models.

.. note:: Some (json) pretty printed output may look slightly different due to the change in serialization library.

.. note:: For backward compatibility reasons, the computation of the checksum for the system model has not been modified and still uses the ``org.json`` library.

.. tip:: As an added benefit for using a more powerful json parsing library, you can 
   now:

   * use comments (java style ``//`` or ``/* */``) in your json model (note that the comments are **not** preserved, but it won't generate an error when parsing!)
   * use single quotes
   * don't quote keys

List of tickets:

* Implemented `glu-132 <https://github.com/pongasoft/glu/issues/132>`_: `Enhance glu's performance by integrating jackson`
* Fixed `glu-133 <https://github.com/pongasoft/glu/issues/133>`_: `Be able to run GLU on IBM's JDK` (thanks to Lucas)


4.3.0 (2012/03/18)
------------------

4.3.0 introduces:

* the ability to define your own system wide state machine (check the glu script chapter in the documentation for 
  details)::

	defaultTransitions =
	[
	  NONE: [[to: 's1', action: 'noneTOs1']],
	  s1: [[to: 'NONE', action: 's1TOnone'], [to: 's2', action: 's1TOs2']],
	  s2: [[to: 's1', action: 's2TOs1']]
	]
        defaultEntryState = 's2'


* customize the actions for a given mountPoint on the agents page

  .. image:: /images/release/v4.3.0/mountPointActions.png
     :align: center
     :alt: mountPoint actions

* customize the plans available on the ``Plans`` subtab

  .. image:: /images/release/v4.3.0/plans.png
     :align: center
     :alt: Plans

* define your own set of custom plan type (or redefine one, like the meaning of "Bounce") (check the plugin hook 
  documentation)::

	def PlannerService_pre_computePlans = { args ->
	  switch(args.params.planType)
	  {
	    case "customPlan":
	      args.params.state = "installed"
	      return plannerService.computeTransitionPlans(args.params, args.metadata)
	      break

	    default:
	      return null
	  }
	}


List of tickets:

* Fixed `glu-127 <https://github.com/pongasoft/glu/issues/127>`_: `cannot issue stop from cli`
* Implemented `glu-128 <https://github.com/pongasoft/glu/issues/128>`_: `Allow customization of the default state machine`
* Fixed `glu-129 <https://github.com/pongasoft/glu/issues/129>`_: `Exception when calling stop with nothing to do`


4.2.0 (2012/02/16)
------------------

4.2.0 introduces the ability to package a glu script as a precompiled class (or set of classes) inside one (or more) jar file(s). As a result, a glu script can inherit from another class as well as have external (to glu) dependencies! Check :ref:`glu-script-packaging` for more info.

* Implemented `glu-118 <https://github.com/pongasoft/glu/issues/118>`_: `Add classpath / compiled glu script capability`
* Fixed `glu-120 <https://github.com/pongasoft/glu/issues/120>`_: `Release user can't load model via the cli`
* Fixed `glu-121 <https://github.com/pongasoft/glu/issues/121>`_: `Admin user can't load model via the cli`
* Fixed `glu-124 <https://github.com/pongasoft/glu/issues/124>`_: `REST api should not use current logged in user session`
* Implemented `glu-125 <https://github.com/pongasoft/glu/issues/125>`_: `add extra link shortcut in the dashboard`
* Implemented `glu-126 <https://github.com/pongasoft/glu/issues/126>`_: `Add REST api for manipulating fabrics`


4.1.1 (2012/01/27)
------------------

.. note:: Issue 116 introduces a change in the default handling of delta vs error (requested by both LinkedIn and Orbitz): when an application is not running and there is a delta, it is better to treat it as an error instead of a simple delta because it represents the fact that something is wrong. 
          You can revert to the previous behavior (delta is never treated as an error) by adding the configuration parameter to your (console) configuration file::

            console.deltaService.stateDeltaOverridesDelta = false

* Fixed `glu-115 <https://github.com/pongasoft/glu/issues/115>`_: `NPE when creating undeploy/redeploy plan for a model with child/parent relationship`
* Fixed `glu-116 <https://github.com/pongasoft/glu/issues/116>`_: `DELTA takes priority over ERROR in the UI`
* Fixed `glu-117 <https://github.com/pongasoft/glu/issues/117>`_: `shell.fetch generates Authorization header when not required`


4.1.0 (2011/12/29)
------------------

.. warning:: The following configuration parameters have changed in the console configuration file. If you are using the feature *restricting file access on an agent* then you need to rename them prior to starting the 
             new console when upgrading::

               console.authorizationService.unrestrictedLocation  -> plugins.StreamFileContentPlugin.unrestrictedLocation
               plugins.StreamFileContentPlugin.unrestrictedRole (new and optional value)

This version of glu adds the concept of plugins to the orchestration engine/console which allows you to enhance and/or tweak the behavior of glu. Typical uses cases are the ability to entirely change the authentication mechanism used by glu, send a notification when a deployment ends, prevent a deployment by the wrong user or at the wrong time, etc... Check the orchestration engine documentation for more information about plugins. This new version sets up the infrastructure for plugins and adds a handful of hooks. Future versions will contain more hooks (depending on user needs).

List of tickets
^^^^^^^^^^^^^^^

* Fixed `glu-113 <https://github.com/pongasoft/glu/issues/113>`_: `Exception with customized dashboard`
* Implemented `glu-114 <https://github.com/pongasoft/glu/issues/114>`_: `Adding concept of plugin to glu`

4.0.0 (2011/11/17)
------------------

What is new in 4.0.0 ?
^^^^^^^^^^^^^^^^^^^^^^

.. warning:: 2 configuration parameters have changed in the console configuration file and you need to rename them prior to starting the 
             new console when upgrading (see the :ref:`configuration section <console-configuration>` for more details on the values)::

               model  -> shortcutFilters
               system -> model
  

4.0.0 contains a major redesign of the console with an easier to use interface and ability to create custom dashboards.

* Top navigation changes:

  * added ``Agents`` tab which lists all the agents (nodes) with direct access to individual agents
  * renamed ``Plans`` into ``Deployments``
  * ``System`` tab is gone and has been replaced with a combination of the ``Model`` tab and the ``Plans`` subtab in the dashboard
  * ``Model`` tab is now used to view the models previously loaded as well as load a new one
  * Fabric selection is now a drop down (same for filter shortcuts (``All [product]``))

* Dashboard is now customizable and a user can create different dashboards (see the :ref:`dashboard section<console-dashboard>` for details). The dashboard represents a table view of the `delta`. Both columns and rows can be customized:

  * columns can be customized: ability to add/remove/move any column. Clicking on a column name does a `'group by'` on the column and make it the first column (same functionality as the `'group by checkbox'` from the previous version). What is rendered in the column is customizable, from the sort order to the grouping functionality (when using `summary` view)
  * rows can be customized: you can add a filter to the model which essentially filters which row is displayed. Clicking on a value in a cell now adds a filter (this functionality existed with the difference that it was `replacing` instead of `adding`). You can of course remove a filter.
  * to customize the dashboard, there is a new subtab for it: ``Customize`` (this gives you access to the raw json representation of the dashboard which you can then tweak, like moving columns around or adding/removing new ones)
  * the first subtab on the dashboard allows you to quickly switch between your saved dashboards and also contains a very useful ``Save as New`` entry which allows you to save what you see as a new dashboard (so instead of tweaking the json, you can add filters and move columns around and then save it as a new dashboard which you can then tweak)

* Dashboard selection is now sticky which means if you move around and come back to the dashboard it will be in the same state. This is used for the ``Plans`` subtab of the dashboard which allows you to `act` on the delta: actions will be based on the filter currently set. If you want to act on the full system (old ``System`` tab), simply clear all filters.

* You can now give a name to your model and it will be displayed in addition to the SHA-1 (``metadata.name``)

* Downgraded security level for model manipulation (load/save) from ``ADMIN`` to ``RELEASE``

* Clicking on the name of an agent in the dashboard table used to link to the agent. By default it now behaves like any other value: adding a filter. You can now access an agent using the ``Agents`` tab. If you want to revert to the previous behavior, use this configuration property: ``dashboardAgentLinksToAgent: true`` in ``console.defaults``.

* Renamed ``console.defaults.model`` into ``console.defaults.shortcutFilters``: this functionality is now a simple shortcut that allows to switch between various predefined filters (example of usage: changing zones, changing products, changing teams, etc...)

* Renamed ``console.defaults.system`` into ``console.defaults.model``: to be consistent with the UI where you are looking at models

List of tickets
^^^^^^^^^^^^^^^

* Implemented `glu-17 <https://github.com/pongasoft/glu/issues/17>`_: `Feature Request: make console views navigation friendly (bookmarkable)`
* Implemented `glu-28 <https://github.com/pongasoft/glu/issues/28>`_: `Feature Request: Add dates to the table at /console`
* Implemented `glu-44 <https://github.com/pongasoft/glu/issues/44>`_: `handle dashboard.model properly`
* Implemented `glu-104 <https://github.com/pongasoft/glu/issues/104>`_: `Make dashboard customizable by user`
* Fixed `glu-105 <https://github.com/pongasoft/glu/issues/105>`_: `Error count incorrect in glu dashboard`
* Fixed `glu-107 <https://github.com/pongasoft/glu/issues/107>`_: `CSS and some js become inaccessible after a while`
* Fixed `glu-108 <https://github.com/pongasoft/glu/issues/108>`_: `Key mistake in the summary section in the documentation`

3.4.0 (2011/10/10)
------------------

A few changes to the agent (requires upgrade):

* Now the agent saves its fabric in ZooKeeper on boot (since it can be overriden on the command line, it ensures that the console sees the same value!)
* The agent offers a ``/config`` REST api after full boot (which allows to change the fabric after the agent has booted (but it still requires a manual agent reboot... will be implemented later))
* Fixed timing issue on auto upgrade
* Fixed the order in which properties are read to make sure that properties assigned in a previous run are used as default values and never override new values!

Several new REST apis:

* ``GET /-/``: list all fabrics
* ``GET /-/agents``: list agent -> fabric association
* ``PUT /<fabric>/agent/<agent>/fabric``: assign a fabric to an agent
* ``DELETE /<fabric>/agent/<agent>/fabric``: clear the fabric for an agent (also added to the UI ``Admin/View agents fabric``)
* ``DELETE /<fabric>/agent/<agent>``: `decommission` and agent (clear ZooKeeper of all agent information)  (also added to the UI ``Admin/View agents fabric``)

Upgraded to ``linkedin-utils-1.7.1`` and ``linkedin-zookeeper-1.4.0`` to fix #95

List of tickets:

* Implemented `glu-35 <https://github.com/pongasoft/glu/issues/35>`_: `Add 'decommission' a node/agent to the console`
* Fixed `glu-69 <https://github.com/pongasoft/glu/issues/69>`_: `Agent auto upgrade process relies on timing`
* Fixed `glu-95 <https://github.com/pongasoft/glu/issues/95>`_: `shell.fetch delivers files to an incorrect location`
* Fixed `glu-99 <https://github.com/pongasoft/glu/issues/99>`_: `add assign to fabric to REST API`
* Fixed `glu-100 <https://github.com/pongasoft/glu/issues/100>`_: `agent persistent property issues: override new values`
* Fixed `glu-101 <https://github.com/pongasoft/glu/issues/101>`_: `console fails to start when changing keys`
* Fixed `glu-103 <https://github.com/pongasoft/glu/issues/103>`_: `3.4.0dev Agent REST Call doesn't return unassociated agents.`


3.3.0 (2011/09/16)
------------------

This release features the following:

* Performance tuning (minimizing GC) based on LinkedIn feedback
* UI change: text area for modifying the model can be (optionally) made non editable (see :ref:`documentation <console-configuration-non-editable-model>`)
* UI change: selecting the current system/model is done through a radio group selection under the ``System`` tab
* UI change: selecting a plan is no longer a drop down selection (this was discussed in the `forum <http://glu.977617.n3.nabble.com/RFC-Selecting-a-plan-proposal-td3333742.html>`_)
* UI change: on the dashboard, there is now a different color for ``DELTA`` vs ``ERROR``
* UI customization: added powerful ability to provide your own custom stylesheet (see :ref:`documentation <console-configuration-custom-css>`) allowing you to easily tweak the rendering (colors, layout, etc...)
* Added documentation example on how to use a :ref:`different database <console-configuration-database-mysql>` with glu (MySql in this example)

List of tickets:

* Implemented `glu-76 <https://github.com/pongasoft/glu/issues/76>`_: `Allow database configuration for the console`
* Implemented `glu-77 <https://github.com/pongasoft/glu/issues/77>`_: `Do not fetch full json model on System page`
* Implemented `glu-78 <https://github.com/pongasoft/glu/issues/78>`_: `Make System Text Area optionally read only`
* Implemented `glu-79 <https://github.com/pongasoft/glu/issues/79>`_: `keeping completed plans in unarchived state causes memory pressure`
* Implemented `glu-89 <https://github.com/pongasoft/glu/issues/89>`_: `make delta distinct from error in console`
* Implemented `glu-93 <https://github.com/pongasoft/glu/issues/93>`_: `Issue #89: make delta distinct from error in console` (thanks Richard)
* Implemented `glu-94 <https://github.com/pongasoft/glu/issues/94>`_: `fix typo in hello-world sample` (thanks Vincent)
* Implemented `glu-96 <https://github.com/pongasoft/glu/issues/96>`_: `Make plan selection easier`

Thanks to Richard and Vincent for the contributions to this release.

3.2.0 (2011/07/31)
------------------

Enhanced REST API by exposing more functionalities (agent upgrade, deployments, plans). Note that the REST call ``HEAD /plan/<planId>/execution/<executionId>`` now returns a header called ``X-glu-completion`` (the old one ``X-LinkedIn-GLU-completion`` is still returned for backward compatibility).

* Implemented `glu-66 <https://github.com/pongasoft/glu/issues/66>`_: `implement rest call GET /plans`
* Fixed `glu-81 <https://github.com/pongasoft/glu/issues/81>`_: `Sometimes ste.message is null. It is null when the exception is java.util`
* Fixed `glu-82 <https://github.com/pongasoft/glu/issues/82>`_: `Add some spacing around the pagination items.`
* Fixed `glu-83 <https://github.com/pongasoft/glu/issues/83>`_: `NPE at http://glu/console/plan/deployments/XXX`

3.1.0 (2011/07/26)
------------------

Added unit test framework for glu script and created sibling project `glu-script-contribs <https://github.com/pongasoft/glu-scripts-contrib>`_

* Implemented `glu-80 <https://github.com/pongasoft/glu/issues/80>`_: `Add ability to write unit tests for glu script`
* Added ``Shell.httpPost`` method

3.0.0 (2011/06/25)
------------------

What is new in 3.0.0 ?
^^^^^^^^^^^^^^^^^^^^^^

3.0.0 adds the following features:

* :ref:`parent/child relationship <static-model-entries-parent>` which adds the capability of decoupling the lifecycle of a parent and a child 
  (typical examples being deploying a webapp inside a webapp container or deploying a bundle in an OSGi container)
* define the desired state of an entry in the model (:ref:`entryState <static-model-entries-entryState>`) which, for example, allows you to deploy an 
  application without starting it
* The console is no longer precomputing the various plans (deploy, bounce, undeploy and redeploy) and they are now computed on demand only
* The delta is now a first class citizen and a new rest API allows to :ref:`access it <goe-rest-api-get-model-delta>`
* The core of the orchestration engine (delta, planner and deployer) has been fully rewritten to offer those new capabilities (now in java
  which should provide some performance improvements over groovy).

List of tickets
^^^^^^^^^^^^^^^

* Fixed `glu-18 <https://github.com/pongasoft/glu/issues/18>`_: `Grails Runtime Exception (500) when viewing a deployment status` (thanks to Ran!)
* Fixed `glu-21 <https://github.com/pongasoft/glu/issues/21>`_: `The model should allow for expressing which state is desired`
* Fixed `glu-33 <https://github.com/pongasoft/glu/issues/33>`_: `Mountpoint disappears from agent view when not in model`
* Implemented `glu-63 <https://github.com/pongasoft/glu/issues/63>`_: `Handle parent/child relationship in the orchestration engine/console`
* Fixed `glu-71 <https://github.com/pongasoft/glu/issues/71>`_: `Fix plan when bouncing parent/child`
* Fixed `glu-72 <https://github.com/pongasoft/glu/issues/72>`_: `Console times out while talking to agent`
* Fixed `glu-73 <https://github.com/pongasoft/glu/issues/73>`_: `Agent upgrade broken due to pid file invalid`

2.4.2 (2011/05/27)
------------------
* Fixed `glu-64 <https://github.com/pongasoft/glu/issues/64>`_: `Concurrent deployment of ivy artifacts causes wrong artifact to be downloaded`

2.4.1 (2011/05/24)
------------------
* Fixed `glu-61 <https://github.com/pongasoft/glu/issues/61>`_: `ClassCastException when error is a String`
* Fixed `glu-62 <https://github.com/pongasoft/glu/issues/62>`_: `"View Full Stack Trace" fails if agent disappears`

2.4.0 (2011/05/20)
------------------
* Added instrumentation for `glu-18 <https://github.com/pongasoft/glu/issues/18>`_: `Grails Runtime Exception (500) when viewing a deployment status`
* Implemented `glu-42 <https://github.com/pongasoft/glu/issues/42>`_: `Support 'transient' declaration in glu script` (thanks to Andras!)
* Implemented `glu-37 <https://github.com/pongasoft/glu/issues/37>`_: `Console should support ETags`
* Fixed `glu-43 <https://github.com/pongasoft/glu/issues/43>`_: `IllegalMonitorException thrown by glu script`
* Fixed `glu-45 <https://github.com/pongasoft/glu/issues/45>`_: `password.sh requires absolute path`
* Misc.: better handling of logs in the console, improved documentation

2.3.0 (2011/05/13)
------------------
* Implemented `glu-56 <https://github.com/pongasoft/glu/issues/56>`_: `Finalize refactoring (#34)`

  * fixed some issues with tagging
  * fixed GString as a key in map issue
  * made some classes more configurable
  * when an entry had only 1 tag, it was being excluded
  * console no longer generates a delta when tags are different!
  * Refactor AgentCli to allow custom configuration

2.2.3 (2011/05/05)
------------------
* Fixed `glu-52 <https://github.com/pongasoft/glu/issues/52>`_: `deadlock on agent shutdown`

2.2.2 (2011/05/04)
------------------
* Fixed `glu-51 <https://github.com/pongasoft/glu/issues/51>`_: `agent does not recover properly when safeOverwrite fails`

2.2.1 (2011/04/30)
------------------
* Fixed `glu-49 <https://github.com/pongasoft/glu/issues/49>`_: `shell.cat is leaking memory`
* Fixed `glu-48 <https://github.com/pongasoft/glu/issues/48>`_: `use -XX:+PrintGCDateStamps for gc log`

Also tweaked a couple of parameters for the agent (starting VM now 128M).

2.2.0 (2011/04/22)
------------------
* Implemented `glu-34 <https://github.com/pongasoft/glu/issues/34>`_: `Refactor code out of the console`

  The business logic layer of the console has been moved to the orchestration engine area so it is now more easily shareable.

* Massive documentation rewrite which covers the tickets `glu-5 <https://github.com/pongasoft/glu/issues/5>`_, `glu-36 <https://github.com/pongasoft/glu/issues/36>`_ and `glu-14 <https://github.com/pongasoft/glu/issues/14>`_

  Check out the `new documentation <http://pongasoft.github.io/glu/docs/latest/html/index.html>`_


2.1.1 (2011/03/04)
------------------
* fixed `glu-31 <https://github.com/pongasoft/glu/issues/31>`_: Agent exception when no persistent properties files

2.1.0 (2011/03/01)
------------------
This version is highly recommended for glu-27 specifically which may prevent the agent to recover properly. It affects all previous versions of the agent.

* fixed `glu-26 <https://github.com/pongasoft/glu/issues/26>`_: agent cli fails when using spaces
* fixed `glu-27 <https://github.com/pongasoft/glu/issues/27>`_: Unexpected exception can disable the agent

2.0.0 (2011/02/14)
------------------
* fixed `glu-22 <https://github.com/pongasoft/glu/issues/22>`_: jetty glu script (1.6.0) does not handle restart properly
* Implemented `glu-25 <https://github.com/pongasoft/glu/issues/25>`_: add tagging capability

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
* workaround for `glu-19 <https://github.com/pongasoft/glu/issues/19>`_: New users aren't displayed at ``/console/admin/user/list``
* fixed `glu-20 <https://github.com/pongasoft/glu/issues/20>`_: Race condition while upgrading the agent

1.7.0 (2011/01/17)
------------------
* Implemented `glu-12 <https://github.com/pongasoft/glu/issues/12>`_: better packaging
* fixed `glu-1 <https://github.com/pongasoft/glu/issues/1>`_: Agent name and fabric are not preserved upon restart
* fixed `glu-9 <https://github.com/pongasoft/glu/issues/9>`_: Using ``http://name:pass@host:port`` is broken when uploading a model to ``/system/model``
* Implemented `glu-16 <https://github.com/pongasoft/glu/issues/16>`_: Use ip address instead of canonical name for Console->Agent communication
* Updated Copyright

1.6.0 (2011/01/11)
------------------
* changed the tutorial to deploy jetty and the sample webapps to better demonstrate the capabilities of glu
* added jetty glu script which demonstrates a 'real' glu script and allows to deploy a webapp container with webapps and monitor them
* added sample webapp with built in monitoring capabilities
* added ``replaceTokens`` and ``httpHead`` to ``shell`` (for use in glu script)
* added ``Help`` tab in the console with embedded forum
* Implemented `glu-12 <https://github.com/pongasoft/glu/issues/12>`_ (partially): better packaging
* fixed `glu-13 <https://github.com/pongasoft/glu/issues/13>`_: missing connection string in setup-zookeeper.sh

1.5.1 (2010/12/28)
------------------
* fixed `glu-10 <https://github.com/pongasoft/glu/issues/10>`_: missing -s $GLU_ZK_CONNECT_STRING in setup-agent.sh (thanks to Ran)
* fixed `glu-11 <https://github.com/pongasoft/glu/issues/11>`_: missing glu.agent.port when not using default value

1.5.0 (2010/12/24)
------------------
* fixed `glu-8 <https://github.com/pongasoft/glu/issues/8>`_: added support for urls with basic authentication (thanks to Ran)
* added console cli (``org.linkedin.glu.console-cli``) which talks to the REST api of the console
* changed tutorial to add a section which demonstrates the use of the new cli
* added the glu logo (thanks to Markus for the logos)

1.4.0 (2010/12/20)
------------------
* use of `gradle-plugins 1.5.0 <https://github.com/pongasoft/gradle-plugins/tree/REL_1.5.0>`_ which now uses gradle 0.9
* added packaging for all clis
* added ``org.linkedin.glu.packaging-all`` which contains all binaries + quick tutorial
* added ``org.linkedin.glu.console-server`` for a standalone console (using jetty under the cover)
* moved keys to a top-level folder (``dev-keys``)
* minor change in the console to handle the case where there is no fabric better
* new tutorial based on pre-built binaries (``org.linkedin.glu.packaging-all``)

1.3.2 (2010/12/07)
------------------
* use of `linkedin-utils 1.2.1 <https://github.com/pongasoft/linkedin-utils/tree/REL_1.2.1>`_ which fixes the issue of password not being masked properly
* use of `linkedin-zookeeper 1.2.1 <https://github.com/pongasoft/linkedin-zookeeper/tree/REL_1.2.1>`_

1.3.1 (2010/12/02)
------------------
* use of `gradle-plugins 1.3.1 <https://github.com/pongasoft/gradle-plugins/tree/REL_1.3.1>`_
* fixes issue in agent cli (exception when parsing configuration)

1.0.0 (2010/11/07)
------------------
* First release
