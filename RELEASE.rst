Release Notes (Latest)
======================

.. sidebar:: Get notified

  .. image:: https://www.bintray.com/docs/images/bintray_badge_color.png
     :alt: Get automatic notifications about new releases
     :class: sidebar-logo
     :target: https://bintray.com/pongasoft/glu/releases/view?source=watch

  Get automatic notifications about new releases.

.. tip:: 

     * Make sure to check the :doc:`Migration Guide <migration-guide>` for tips on how to upgrade glu.

.. _glu-5.6.0:

5.6.0 (2015/05/06)
------------------

.. sidebar:: Download

  .. image:: https://api.bintray.com/packages/pongasoft/binaries/glu/images/download.png?version=5.6.0
     :alt: Download the latest version of glu
     :class: sidebar-logo
     :target: https://bintray.com/pongasoft/glu/releases/5.6.0

  Download glu latest release from Bintray.

This release contains no new features or bug fixes. The only change is that this is the first version of glu that is compatible with java 1.8 (and still works with java 1.7).

.. warning:: In order to make glu compatible with java 1.8, most direct dependencies have been upgraded to their most recent versions (and transitively). Although all tests pass and longevity tests are not showing any difference in memory usage and speed, I would strongly advise to use caution when upgrading to this version. Check the :ref:`migration steps <migration-guide-5.5.6-5.6.0>` for more details.

List of tickets:

* Implemented `glu-295 <https://github.com/pongasoft/glu/issues/295>`_: `Make glu work with java 8`

.. _glu-5.5.6:

5.5.6 (2015/04/30)
------------------

This release includes the following:

* Explicitly track the user who changes the static model (and when it is changed). This information was already available in the Audit Log, but it is now part of the data recorded directly in the domain object ``DbSystemModel`` and ``DbCurrentSystem``. This data is now displayed in the UI as well as returned in REST calls (as HTTP headers). Note that past data is *not* populated and will remain blank. Make sure to check the :ref:`Migration Guide <migration-guide-5.5.5-5.5.6>` for some details on impact.
* Tidy up some UI elements based on SkyHigh Networks feedback (part I).

List of tickets:

* Implemented `glu-287 <https://github.com/pongasoft/glu/issues/287>`_: `Tidy up UI (Part I)`
* Implemented `glu-286 <https://github.com/pongasoft/glu/issues/286>`_: `Record who changed the model`

.. _glu-5.5.5:

5.5.5 (2015/03/20)
------------------

This release includes a couple of bug fixes.

* Issue ``glu-279`` relates to the agent and is required only if you use parent/child relationships in your script (and you have been experiencing this issue).
* Issue ``glu-280`` relates to the console and is only required if you use the console in HTTPS mode behind a proxy (and have been experiencing this issue).

List of tickets:

* Implemented `glu-279 <https://github.com/pongasoft/glu/issues/279>`_: `call delegation not working in glu script`
* Implemented `glu-280 <https://github.com/pongasoft/glu/issues/280>`_: `/console/js/bootstrap.min.js redirects bypasses serverURL`

.. _glu-5.5.4:

5.5.4 (2015/01/20)
------------------

This release disables the sslv3 protocol entirely in the agent to fix the issue related to `POODLE <http://en.wikipedia.org/wiki/POODLE>`_. Note that there is no other change in this release, so if you do not worry about the POODLE issue because for example your agents are not accessible outside your own network, or you are not even running the agents in secure mode (http vs https), then you can skip this release.

* Implemented `glu-277 <https://github.com/pongasoft/glu/issues/277>`_: `Disable sslv3 for glu agent`

.. _glu-5.5.3:

5.5.3 (2014/12/08)
------------------

This release is a minor release which includes a couple of bug fixes. The console has been enhanced to handle pids from glu scripts: :ref:`check the section <console-script-pids>` for details.

* Fixed `glu-274 <https://github.com/pongasoft/glu/issues/274>`_: `Support pid/ps for long clis`
* Fixed `glu-276 <https://github.com/pongasoft/glu/issues/276>`_: `Exception in the console after renaming a fabric`

.. _glu-5.5.2:

5.5.2 (2014/09/08)
------------------

This release is a minor release which includes some improvements to the REST api (abort a plan, list and set static model) as well as a couple of bug fixes.

.. note:: if you are using LDAP, make sure to check the :ref:`migration steps <migration-guide-5.5.1-5.5.2>` for details.

* Fixed `glu-261 <https://github.com/pongasoft/glu/issues/261>`_: `Should be able to disable ldap in console`
* Fixed `glu-264 <https://github.com/pongasoft/glu/issues/264>`_: `wrong install file generated during setup when multiple agents`
* Merged `glu-267 <https://github.com/pongasoft/glu/issues/267>`_: `added more information about failed jobs in deployment logs` (Thanks Subhan)
* Merged `glu-268 <https://github.com/pongasoft/glu/issues/268>`_: `handle unexpected output while setting JAVA_TOOL_OPTIONS enviroment variable` (Thanks Ady)
* Implemented `glu-269 <https://github.com/pongasoft/glu/issues/269>`_: `Add DELETE for /rest/v1/$fabric/plan/$planId/execution/$id`
* Implemented `glu-271 <https://github.com/pongasoft/glu/issues/271>`_: `Add REST api to list and set (static) model`

.. _glu-5.5.1:

5.5.1 & 4.7.3 (2014/04/21)
--------------------------

This release is a bug fix release. Due to the nature of the bug, both the main branch as well as the 4.7.x branch (for java 1.6) have been updated.

The bug is rare but can happen while upgrading glu: it manifests itself by bad data being written to ZooKeeper (only when the agent cannot instantiate a previously deployed glu script) and the console does not handle it properly. The fix is 2 fold:

  * fixed the agent to not write bad data in ZooKeeper
  * fixed the console to handle improper data in ZooKeeper

* Fixed `glu-262 <https://github.com/pongasoft/glu/issues/262>`_: `A bad agent should not bring the console down`

.. _glu-5.5.0:

5.5.0 (2014/03/14)
------------------

New and noteworthy
^^^^^^^^^^^^^^^^^^
* Added ability to retrieve the audit log via a :ref:`REST api <goe-rest-api-list-audit-logs>`
* Added a concept of `max parallel steps count` in order to limit the parallelism of a given
  deployment on a per deployment basis.

  * A new text field (in order to input this value) has been added in the UI

     .. image:: /images/release/v5.5.0/maxParallelStepsCount.png
        :width: 600
        :align: center
        :alt: Max Parallel Steps Count

  * a new parameter has been added to the :ref:`REST api <goe-rest-api-representing-a-plan>`.

  .. note:: The ``...leafExecutorService.fixedThreadPoolSize`` :ref:`console property configuration <console-configuration-limiting-parallel-steps>` allows you to limit the parallelism globally at the thread level (as soon as one step completes, another one will start). The new concept allows you to limit the parallelism for a given deployment by `transforming` a fully parallel plan into a sequential plan containing groups of parallel plans: only when the entire group is completed will the next one start.

Tickets
^^^^^^^
* Implemented `glu-159 <https://github.com/pongasoft/glu/issues/159>`_: `Add audit log access to the REST api`
* Fixed `glu-258 <https://github.com/pongasoft/glu/issues/258>`_: `wait for state does not wake up on forceChangeState`
* Implemented `glu-260 <https://github.com/pongasoft/glu/issues/260>`_: `Implement "hybrid" plan`

.. _glu-5.4.2:

5.4.2 (2014/01/17)
------------------

This release is a small bug fix release.

* Fixed `glu-257 <https://github.com/pongasoft/glu/issues/257>`_: `Allow to change the console server port in the setup phase`

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

4.7.x
-----

Check the :doc:`release-notes-old` section for older release notes.
