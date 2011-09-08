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


Restricting file access on an agent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The console offers the ability to show any file on a remote agent which may not always be desirable. There is a way to limit what can be viewed for users which are not ``ADMIN`` (see :ref:`console-user-management` for an explanation of roles)::

  // path to the root of the unrestricted location (empty means no restriction)
  console.authorizationService.unrestrictedLocation = "/export/content/glu"

Database
^^^^^^^^

The console uses a database to store some of its data (like the audit log, user information, etc...). At this moment in time, the console uses `HSQLDB <http://hsqldb.org/>`_ for its database. You may want to change the location of the database folder by providing a ``org.linkedin.glu.console.dataSource.url`` system property (``-Dorg.linkedin.glu.console.dataSource.url=/path/to/database/root`` when starting the webapp container) or directly modifying the following section in the configuration file::

   def dataSourceUrl =
     System.properties['org.linkedin.glu.console.dataSource.url'] ?:
     "jdbc:hsqldb:file:${System.properties['user.dir']}/database/prod;shutdown=true"

   // specify the database connection string
   dataSource.dbCreate = "update"
   dataSource.url = dataSourceUrl

.. note:: Coming Soon: ability to change the database provider entirely.

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

UI configuration
^^^^^^^^^^^^^^^^

The UI is configured in the ``console.defaults`` section of the configuration file. It is a simple map::

  console.defaults = 
  [
    ... configuration goes here ...
  ]

Dashboard
"""""""""
The dashboard can be configured with the following section::

      dashboard:
      [
          mountPoint: [checked: true, name: 'mountPoint', groupBy: true, linkFilter: true],
          agent: [checked: true, name: 'agent', groupBy: true],
          'tag': [checked: false, name: 'tag', groupBy: true, linkFilter: true],
          'tags': [checked: true, name: 'tags', linkFilter: true],
          'metadata.container.name': [checked: true, name: 'container', groupBy: true, linkFilter: true],
          'metadata.version': [checked: true, name: 'version', groupBy: true],
          'metadata.product': [checked: true, name: 'product', groupBy: true, linkFilter: true],
          'metadata.cluster': [checked: true, name: 'cluster', groupBy: true, linkFilter: true],
          'initParameters.skeleton': [checked: false, name: 'skeleton', groupBy: true],
          script: [checked: false, name: 'script', groupBy: true],
          'metadata.modifiedTime': [checked: false, name: 'Last Modified', groupBy: false],
          status: [checked: true, name: 'status', groupBy: true]
      ]

This configuration results in the following output (the window has been made smaller to fit all the options on a readable screenshot):

.. image:: /images/configuration-dashboard.png
   :align: center
   :alt: Dashboard configuration

``dashboard`` is a map with the following convention:

* the key represent the :term:`dotted notation` of an entry (see: :ref:`goe-filter-syntax` for more details and examples on the dotted notation)
* the value is another map with the following conventions:

  ============== ==============
  key            value
  ============== ==============
  ``name``       the display name of the column as well as *Group By* or *Show/Hide* section
  ``checked``    ``true`` or ``false``: whether the checkbox next to the name should be checked by default or in other words if the column should be shown or hidden by default
  ``groupBy``    ``true`` or ``false``: in which section does this option appears (*Group By* section or *Show/Hide* section). Some values do not make too much sense to allow them to do a *group by* on them (like the ``Last Modified`` which is (under the cover) a timestamp
  ``linkFilter`` ``true`` or ``false``: if you want to allow the values in the table to be links to a filter (see :doc:`filtering`)
  ============== ==============

.. note:: This section will be enhanced in the future to allow even more configuration as well as how to *group* values.

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

System
""""""

The system page displays a summary and the columns are configurable in the following section::

      system:
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
   :alt: System configuration

``system`` is a map with the following convention:

* the key represent the :term:`dotted notation` of an entry (see: :ref:`goe-filter-syntax` for more details and examples on the dotted notation)
* the value is a map with (currently) one entry only: ``name`` which represents the (display) name of the column

.. note:: Since 3.3.0, it takes effect only when showing a single system: for performance reasons the page which shows the list of systems no longer fetches the system and as such cannot display this information

Non Editable Model
""""""""""""""""""

You can enable or disable the fact that a system model is editable or not by changing the following property::

     disableModelUpdate: false,

By default, the model is editable which means that there is a ``Save Changes`` button and the text area containing the body is editable. Changing this value to ``true`` removes the button and makes the text area non editable anymore.

.. note:: Even if the model is not editable, it is always possible to *load* a new one by going to the ``Model`` tab. The idea behind this feature is to enforce the fact that the model should be properly versioned outside of glu and changing it should go through a proper flow that is beyond the scope of glu.

Model
"""""

This entry allows you to configure what appears on the top right of the console (right after the fabric name) thus allowing you to tailor the display even further: it allows you to set a temporary :term:`filter` on all views. In the following example we *model* the concept of products (also known as lines), this is why this entry is called ``model``::

      model:
      [
          [
              name: 'product',
              header: ['version']
          ]
      ]

This configuration results in the following output:

.. image:: /images/configuration-model.png
   :align: center
   :alt: Model configuration

The format of this entry is an array of maps (so you can have more than one and they will be delimited by a ``|``) where each map consists of:

* a ``name`` entry referring to the metadata section of the model (**not** of an entry). This should be a map with the given name and the following convention:

  * the key is the value of the filter
  * the value is another map with the following convention:

    * ``name`` is the display name of the value (in this example they are the same!)
    * other entries that are used in the ``header`` section (see below)


  .. note:: the ``name`` value also maps to the metadata dotted notation for the filter (in this example it will be ``metadata.product``)

* a ``header`` entry which is an array for extra information referring to entries in the metadata (see above)

Example of system which produces the previous screen::

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

This entry is very similar to the previous one (``model``) and allows you to add more information to the header in the console::

      header:
      [
          metadata: ['drMode']
      ]

This entry simply contains a ``metadata`` section which is an array of keys in the :term:`metadata` of the system (**not** of an entry).

Example of system::

  "metadata": {
    "product": {...},
    "drMode": "primary"
 }

The previous system and ``header`` configuration produce the following output:

.. image:: /images/configuration-header.png
   :align: center
   :alt: Header configuration

.. tip:: Since every fabric has its own model, hence its own ``metadata`` section, this feature is a convenient way to display fabric specific information. In this example we can display which fabric represents the primary data center versus which one represents the secondary data center.


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

