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

.. |console-logo| image:: /images/console-logo-86.png
   :alt: console logo
   :class: header-logo

|console-logo| Console
======================
The glu console is a web application that offers a graphical presentation on top of the :doc:`orchestration engine <orchestration-engine>`.

Console in action
-----------------
The best way to get a feel of what the console looks like and what can be achieved, take a look at the :doc:`tutorial <tutorial>`.

Installation
------------
Check the section :ref:`production-setup-console` for details about how to install the console.

.. _console-configuration:

Configuration file
------------------

When the console boots it reads a configuration file. There are 3 ways to provide this configuration file to the console:

1. store it under ``$HOME/.org.linkedin.glu/glu-console-webapp.groovy``
2. store it under ``conf/glu-console-webapp.groovy`` (relative to wherever the VM ``user.dir`` is set to)
3. pass a system property ``-Dorg.linkedin.glu.console.config.location=/fullpath/to/file``

Through this file, the console allows you to configure several areas including some aspects of the UI.

.. note:: if you modify the configuration file, you need to stop and restart the console for the changes to take effect

Console <-> Agent Connection
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The console (orchestration engine) talks to all the agents using a REST api over https (or http if you do disable security).

The following section allows you to configure how the console talks to the agent::

  console.sslEnabled=true

  def keysDir = System.properties['org.linkedin.glu.console.keys.dir'] ?: 
                "${System.properties['user.dir']}/keys"

  console.keystorePath="${keysDir}/console.keystore"
  console.keystorePassword = "nacEn92x8-1"
  console.keyPassword = "nWVxpMg6Tkv"

  console.secretkeystorePath="${keysDir}/console.secretkeystore"

  console.truststorePath="${keysDir}/agent.truststore"
  console.truststorePassword = "nacEn92x8-1"

TODO: add link to key management.

If you do not want a secure connection between the console and the agent, this is how you would configure it::

  console.sslEnabled=false

  // the following entries *must* be present (but can be left empty)
  console.keystorePath=""
  console.keystorePassword = ""
  console.keyPassword = ""
  console.secretkeystorePath=""
  console.truststorePath=""
  console.truststorePassword = ""


.. warning:: Disabling the secure connection means that the agents will happily serve any request irrelevant of where it is coming from, which could be a serious security hole.

Plugins
^^^^^^^

The console (orchestration engine) behavior can be tweaked and or enhanced with the use of plugins. Check the :ref:`goe-plugins` section for details on how to create and install a plugin. You add your plugin(s) to the console by modifying the following property::

   orchestration.engine.plugins = [
     'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin',
     'org.acme.MyPlugin',
     // etc...
   ]

.. warning:: Since 4.1.0, *restricting file access on an agent* has been migrated to the ``StreamFileContentPlugin`` plugin and is now being configured by defining the 
             properties::

               // path to the root of the unrestricted location (empty means no restriction)
               // this property is used by StreamFileContentPlugin
               plugins.StreamFileContentPlugin.unrestrictedLocation = "/export/content/glu"

               // role for unrestricted
               plugins.StreamFileContentPlugin.unrestrictedRole = 'ADMIN'


Database
^^^^^^^^

The console uses a database to store some of its data (like the audit log, user information, etc...). At this moment in time, the console uses `HSQLDB <http://hsqldb.org/>`_ for its database. You may want to change the location of the database folder by providing a ``org.linkedin.glu.console.dataSource.url`` system property (``-Dorg.linkedin.glu.console.dataSource.url=/path/to/database/root`` when starting the webapp container) or directly modifying the following section in the configuration file::

   def dataSourceUrl =
     System.properties['org.linkedin.glu.console.dataSource.url'] ?:
     "jdbc:hsqldb:file:${System.properties['user.dir']}/database/prod;shutdown=true"

   // specify the database connection string
   dataSource.dbCreate = "update"
   dataSource.url = dataSourceUrl

.. _console-configuration-database-mysql:

Example for a different database: MySql
"""""""""""""""""""""""""""""""""""""""

Here are the steps to follow in order to use MySql instead of HSQLDB (the steps would be very similar for a different database!)

* Download and install MySql on your system (don't forget to start it!)

* Connect to MySql and create the ``glu`` user::

    sudo /usr/local/mysql/bin/mysql
    mysql> create database glu;
    mysql> show databases;
    mysql> create user 'glua'@'localhost' identified by 'password';
    mysql> grant all on glu.* to 'glua'@'localhost';

  .. warning:: This values are obviously just example values (they simply need to be plugged in the right place in the configuration file (see below)).

* Download the MySql `driver <http://www.mysql.com/downloads/connector/j/>`_

* Copy the driver (in this example it is called ``mysql-connector-java-5.1.17-bin.jar``) to ``console-server/jetty-distribution-7.2.2.v20101205/lib/ext``

  .. note:: You may need to run ``./bin/tutorial.sh setup`` in order to create the proper folder structure first!

* Edit the glu configuration file (``console-server/conf/glu-console-webapp.groovy``) to input the proper values::

    // 'glu' is the name of the database created previously
    def dataSourceUrl = "jdbc:mysql://localhost/glu"
    dataSource.dbCreate = "update"
    dataSource.url = dataSourceUrl
    dataSource.logSql=false // set to true for details (+ open trace logging level)
    dataSource.dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
    dataSource.driverClassName = "com.mysql.jdbc.Driver"
    dataSource.username= "glua" // user created previously
    dataSource.password = "password" // password assigned previously

* Start the console

* You can check that MySql is being used::

    mysql> use glu;
    mysql> show tables;
    +---------------------+
    | Tables_in_glu       |
    +---------------------+
    | audit_log           |
    | db_current_system   |
    | db_deployment       |
    | db_system_model     |
    | db_user_credentials |
    | fabric              |
    | role                |
    | user                |
    | user_permissions    |
    | user_role           |
    +---------------------+

Logging
^^^^^^^

The log4j section allows you to configure where and how the console logs its output. It is a DSL and you can view more details on how to configure it directly on the `grails web site <http://grails.org/doc/1.3.x/guide/3.%20Configuration.html#3.1.2%20Logging>`_::

   log4j = {
       appenders {
	 file name:'file',
	 file:'logs/console.log',
	 layout:pattern(conversionPattern: '%d{yyyy/MM/dd HH:mm:ss.SSS} %p [%c{1}] %m%n')
       }

       root {
	 info 'file'
	 additivity = false
       }

       error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
		  'org.codehaus.groovy.grails.web.pages', //  GSP
		  'org.codehaus.groovy.grails.web.sitemesh', //  layouts
		  'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
		  'org.codehaus.groovy.grails.web.mapping', // URL mapping
		  'org.codehaus.groovy.grails.commons', // core / classloading
		  'org.codehaus.groovy.grails.plugins', // plugins
		  'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
		  'org.springframework',
		  'org.hibernate'

       info 'grails',
	    'org.linkedin'

       //debug 'com.linkedin.glu.agent.tracker', 'com.linkedin.glu.zookeeper.client'

       //trace 'org.hibernate.SQL', 'org.hibernate.type'

       warn   'org.mortbay.log', 'org.restlet.Component.LogService', 'org.restlet'
   }

.. note:: This has nothing to do with the audit log!

.. _console-configuration-ldap:

LDAP
^^^^

You can configure LDAP for handling user management in the console. See :ref:`console-user-management` for details. Here is the relevant section in the configuration file::

  // This section is optional if you do not want to use ldap
  ldap.server.url="ldaps://ldap.acme.com:3269"
  ldap.search.base="dc=acme,dc=com"
  ldap.search.user="cn=glu,ou=glu,dc=acme,dc=com"
  ldap.search.pass="helloworld"
  ldap.username.attribute="sAMAccountName"

Tuning
^^^^^^

Deployments auto-archive
""""""""""""""""""""""""

The plans page displays the list of deployments that have happened recently. Since 3.3.0, items in this list are automatically archived after 30 minutes. You can tune this property or simply disable automatic archiving by adding the following property::

  // set it to "0" if you want to disable auto archiving entirely
  console.deploymentService.autoArchiveTimeout = "30m"


UI configuration
^^^^^^^^^^^^^^^^

The UI is configured in the ``console.defaults`` section of the configuration file. It is a simple map::

  console.defaults = 
  [
    ... configuration goes here ...
  ]

.. _console-configuration-custom-css:

Custom Css
""""""""""

If you want to customize the look and feel of the console and override or tweak any css value, you can provide your own custom css. Possible values are ``null`` (or simply non existent), ``String`` or ``URI``::

      // no custom css
      customCss: null

      // as a String (using groovy """ notation makes it convenient)
      customCss: """
        .OK {
          background: green;
        }
      """

      // as a URI (for security reasons should be local!)
      customCss: new URI("/glu/repository/css/custom.css")

Default Dashboard
"""""""""""""""""
See the :ref:`section <console-default-dashboard>` on how to configure the ``dashboard`` section of the configuration file.

Tags
""""

You can specify the colors of each tag (foreground and background)::

    tags:
    [
      '__default__': [background: '#005a87', color: '#ffffff'],
      'webapp': [background: '#ec7000', color: '#fff0e1'],
      'frontend': [background: '#006633', color: '#f1f5ec'],
      'backend': [background: '#5229a3', color: '#e0d5f9'],
    ],

.. note:: the ``__default__`` entry is optional and specify the color of the tags that are not specifically defined

.. note:: the color value (ex: ``#005a87``) ends up being used in css so you can use whatever is legal in css (ex: ``red``)

Model
"""""

The model page displays a summary and the columns are configurable in the following section::

      model:
      [
         agent: [name: 'agent'],
        'tags.webapp': [name: 'webapp'],
         'metadata.container.name': [name: 'container'],
         'metadata.product': [name: 'product'],
         'metadata.version': [name: 'version'],
         'metadata.cluster': [name: 'cluster']
      ]

This configuration results in the following output:

.. image:: /images/configuration-system-600.png
   :align: center
   :alt: Model display configuration

``model`` is a map with the following convention:

* the key represent the :term:`dotted notation` of an entry (see: :ref:`goe-filter-syntax` for more details and examples on the dotted notation)
* the value is a map with (currently) one entry only: ``name`` which represents the (display) name of the column

.. note:: Since 3.3.0, it takes effect only when showing a single model: for performance reasons the page which shows the list of models no longer fetches the model and as such cannot display this information

.. _console-configuration-non-editable-model:

Non Editable Model
""""""""""""""""""

You can enable or disable the fact that a system model is editable or not by changing the following property::

     disableModelUpdate: false,

By default, the model is editable which means that there is a ``Save Changes`` button and the text area containing the body is editable. Changing this value to ``true`` removes the button and makes the text area non editable anymore.

.. note:: Even if the model is not editable, it is always possible to *load* a new one by going to the ``Model`` tab. The idea behind this feature is to enforce the fact that the model should be properly versioned outside of glu and changing it should go through a proper flow that is beyond the scope of glu.

Shortcut Filters
""""""""""""""""

This entry allows you to define a dropdown that will be populated based on the model metadata in order to quickly set a filter. In the screenshot below, clicking on ``'product2:2.0.0'`` in the dropdown is equivalent to removing the current filter on ``product1`` and adding a new one for ``product2`` simplifying going back and forth between globally used concepts (like teams or product lines or zones, etc...)::

      shortcutFilters:
      [
          [
              name: 'product',
              source: 'metadata.product',
              header: ['version']
          ]
      ]

This configuration results in the following output:

.. image:: /images/shortcut-filters.png
   :align: center
   :alt: Shortcut filters

The format of this entry is an array of maps (so you can have more than one!) where each map consists of:

* a ``name`` entry which will be displayed in the dropdown when no filter entry is selected ``All [<name>]``
* a ``source`` which represents a :term:`dotted notation` of which data to use in the model. The data in the model should have the following structure:

  * the key is the value of the filter
  * the value is another map with the following convention:

    * ``name`` is the display name of the value (in this example they are the same!)
    * other entries that are used in the ``header`` section (see below)

* a ``header`` entry which is an array for extra information referring to entries in the metadata (see above)

Example of model which produces the previous screen::

  "metadata": {
    "product": { <================ value as defined in the model section (name: 'product')
      "product1": { <============= first entry (value for the filter!)
        "name": "product1", <===== display name of the value
        "version": "1.0.0"  <===== extra info as defined in the model section (header: ['version'])
      },
      "product2": { <============= second entry (value for the filter!)
        "name": "product2", <===== display name of the value
        "version": "2.0.0"  <===== extra info as defined in the model section (header: ['version'])
      }
    }
  }

Header
""""""

This entry allows you to add more information to the header in the console::

      header:
      [
          metadata: ['drMode']
      ]

This entry simply contains a ``metadata`` section which is an array of keys in the :term:`metadata` of the model (**not** of an entry).

Example of model::

  "metadata": {
    "product": {...},
    "drMode": "primary"
 }

The previous model and ``header`` configuration produce the following output:

.. image:: /images/configuration-header.png
   :align: center
   :alt: Header configuration

.. tip:: Since every fabric has its own model, hence its own ``metadata`` section, this feature is a convenient way to display fabric specific information. In this example we can display which fabric represents the primary data center versus which one represents the secondary data center.

Dashboard Agent Links
"""""""""""""""""""""

By default (new since 4.0.0), a link to an agent name in the dashboard has the same effect as all other links on the dashboard: it adds a filter. In this case it filters the model to see only the entries on this specific agent. The top level ``'Agents'`` tab allows you to navigate to the agent view. If you want to turn this off (thus reverting to the behavior prior to 4.0.0) use this entry (``dashboard`` section of the configuration file)::

      dashboardAgentLinksToAgent: true

.. _console-dashboard:

Dashboard
---------

The dashboard is the main entry point of the console and is a visual representation of the :ref:`delta <goe-delta>`.

.. image:: /images/console-dashboard-600.png
   :align: center
   :alt: Console dashboard

.. _console-dashboard-customization:
 
Customization
^^^^^^^^^^^^^

Since 4.0.0, the dashboard can be customized by every user the way they see fit. Every user can also define as many `dashboards` as they want and the dropdown allows to switch between dashboard quickly.

.. image:: /images/console-dashboard-switch.png
   :align: center
   :alt: Console dashboard

The easier way to create a new dashboard is to add filters and group by columns (by clicking on a column name) and then select ``'Save as new'``. The new dashboard can then be further customized by clicking on the ``'Customize'`` subtab which shows the definition of the dashboard as a json document::

  {
    "columnsDefinition": [
      {
        "groupBy": "uniqueCountOrUniqueVal",
        "linkable": true,
        "name": "cluster",
        "orderBy": "asc",
        "source": "metadata.cluster",
        "visible": true
      },
     ... etc ...
   ],
    "customFilter": "metadata.cluster='c1'",
    "errorsOnly": false,
    "name": "c1 cluster",
    "summary": false
  }

* ``name``: the name of the dashboard (will be displayed in the dropdown)
* ``errorsOnly``: to display only rows that are in error
* ``summary``: to display a summary view when there are more than one row with the same value (based on the first column). When this value is set to ``true`` then the ``groupBy`` entry of a column definition kicks in
* ``columnsDefinition`` is an array which defines each column. The order in the array defines in which order columns will be displayed so it is easy to move columns around by simply moving them in the array. Each column is defined this way:

  * ``name``: the name of the column as it appears in the dashboard
  * ``source``: which data in the model needs to be displayed using the :term:`dotted notation` (see: :ref:`goe-filter-syntax` for more details and examples on the dotted notation)
  * ``orderBy``: how to sort the column. Possible values are: ``asc / desc / null`` (``null`` means ignore for sorting purposes)
  * ``groupBy``: how to group rows when using a ``summary`` view. Possible values are: ``uniqueVals / uniqueCount / uniqueCountOrUniqueVal / vals / count / min / max``
  * ``linkable``: whether a link to add a filter should be displayed
  * ``visible``: whether the column should be visible at all (useful mostly internally for derived values)

.. _console-default-dashboard:

Default Dashboard
^^^^^^^^^^^^^^^^^

When a user logs in the first time it is assigned a `default` dashboard. This dashboard is defined in the configuration file under the ``defaults.dashboard`` section and can be a json string or a simple groovy array of ``columnsDefinition`` as defined previously. This is the one that comes by default::

      dashboard:
      [
        [ name: "mountPoint", source: "mountPoint" ],
        [ name: "agent",      source: "agent"],
        [ name: "tags",       source: "tags",       groupBy: "uniqueVals"],
        [ name: "container",  source: "metadata.container.name"],
        [ name: "version",    source: "metadata.version"],
        [ name: "product",    source: "metadata.product"],
        [ name: "cluster",    source: "metadata.cluster"],
        [ name: "status",     source: "status" ],
        [ name: "statusInfo", source: "statusInfo", groupBy: "vals", visible: false],
        [ name: "state",      source: "state",                       visible: false]
      ]


.. _console-script-log-files:

Log Files Display
-----------------

When looking at an agent (agents view page), for each entry, there may be a log section determined by the fields declared in the script:

.. image:: /images/console-script-log-files.png
   :align: center
   :alt: Script log files

In order to see an entry like this you can do the following in your script:

* declare any field which ends in ``Log`` (ex: ``serverLog``)
* declare a field called ``logsDir`` (pointing to a folder) which will display the ``more...`` link
* declare a field called ``logs`` and of type ``Map`` where each entry will point to a log file

Example of glu script::

    class GluScriptWithLogs
    {
      def logsDir
      def serverLog
      def logs = [:]

      def install = {
        logsDir = shell.toResource("${mountPoint}/logs")
        serverLog = logsDir."server.log" // using field with name ending in Log
        logs.gc = logsDir."gc.log" // using logs map
      }
    }

First bootstrap
---------------
The very first time the console is started, it will create an admin user. Log in as this user::

    username: admin
    password: admin

Then click on the ``'admin'`` tab (not the one called ``'Admin'``) and click ``'Manage your credentials'``.

.. warning:: It is strongly recommended you immediately change the admin password!

.. _console-user-management:

User management
---------------
There are 2 ways to manage users.

LDAP
^^^^
If you define an ldap section in the configuration file (see :ref:`configuration section <console-configuration-ldap>` for details), then the console will automatically allow any user who has the correct ldap credentials to login. If the user has never logged in before, a new account will be automatically created and the user will have the role ``USER`` which gives him read access mostly (and limited access to log files). This allows any user to login to the console without any administrator intervention.

Manual user management
^^^^^^^^^^^^^^^^^^^^^^
Whether you use LDAP or not you can always use this method. If you don't use LDAP then it is your only choice. On the ``Admin`` tab, you can create a new user by giving it a user name and a password. You can also assign roles to a user:

+--------------------+------------------------------------------------------------------------+
|Role                |Description                                                             |
+====================+========================================================================+
|``USER``            |mostly read access (limited access to some log files (cannot go anywhere|
|                    |on the filesystem))                                                     |
+--------------------+------------------------------------------------------------------------+
|``RELEASE``         |most of the traditional release actions (like starting and stopping     |
|                    |entries…)                                                               |
+--------------------+------------------------------------------------------------------------+
|``ADMIN``           |administrative role (create user, assign roles…)                        |
+--------------------+------------------------------------------------------------------------+
|``RESTRICTED``      |if you want to ban a user from the console                              |
+--------------------+------------------------------------------------------------------------+

.. note:: No matter what the role the user in, actions taken are logged in the audit log that can be viewed from the ``Admin`` tab.

Password management
^^^^^^^^^^^^^^^^^^^
Even if you use LDAP, a user can assign himself a console password (useful if the password needs to be stored in scripts for example).

Console tabs
^^^^^^^^^^^^
Documentation coming soon. In the meantime, take a look at the :doc:`tutorial <tutorial>`.

