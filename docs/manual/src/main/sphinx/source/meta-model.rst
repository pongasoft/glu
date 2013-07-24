.. Copyright (c) 2013 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

.. _meta-model:

Meta Model
==========
The glu :term:`meta model` is a model which describes where the various components of glu will be installed and how they will be configured.

.. note::
   The glu meta model should not be mistaken with the :doc:`static model <static-model>` which represents where the various components that glu manages will be installed and how they will be configured. This is why it is called the meta model: it is about glu itself, not what glu handles.

The model
---------
The glu meta model is represented in memory by a set of classes. This is this representation that the :doc:`glu setup tool <setup-tool>` uses to generate the glu distribution.

.. tip::
   In order to know what you can (or cannot express) in your meta model, it is strongly advised to look at the `classes <https://github.com/pongasoft/glu/tree/master/provisioner/org.linkedin.glu.provisioner-core/src/main/java/org/pongasoft/glu/provisioner/core/metamodel>`_.

Ultimately the model represents where the various components of glu will be installed and how they will be configured. glu is comprised of:

* set of fabrics (which have unique names)
* set of agents, each agent belonging to one and only one fabric (the name of an agent is unique within a fabric)
* set of ZooKeeper clusters (each cluster being comprised by 3 or 5 ZooKeeper instances)
* set of glu onsoles
* an agent cli
* a console cli

The meta model is expressed on the file system as a set of files which can have 2 different formats: json or light json groovy dsl. Those files are read and parsed to build the in memory model.

Json format
^^^^^^^^^^^
Ultimately the model is expressed in json format. Json is a very simple format but the syntax can be a bit cumbersome at times (especially if you have sections that essentially needs to be repeated over and over). This is why there is another syntax that you can use.

The outline of the format is the following::

  {
    "metaModelVersion": "1.0.0",
    "gluVersion": "...",
    "zooKeeperRoot": "/org/glu",
    "stateMachine": {
      ... definition of the state machine
    },

    "fabrics": {
      "<fabricName": {
        ... fabric definition
      },
      ... more fabrics
    },

    "agents": [
      {
        ... agent definition
      },
      ... more agents
    ],

    "zooKeeperClusters": [
      {
        "name": "<cluster name>",
        "zooKeepers": [
          {
            ... zooKeeper definition
          },
          ... more zooKeepers
        ]
      },
      ... more clusters
    ],

    "consoles": [
      {
        ... console definition
      },
      ... more consoles
    ],

    "agent-cli": {
      ... agent cli definition
    },

    "console-cli": {
      ... console cli definition
    }


  }

.. note::
   The rest of this document will use the json groovy dsl but feel free to express your model in json directly. 

Json Groovy DSL
^^^^^^^^^^^^^^^
The json groovy dsl uses the convenience of groovy to express the model: instead of defining the model in one chunk, you build it pieces at a time which makes it a lot easier to build and read. You can also use the power of groovy, like variable replacements (``${xxx}``) syntax, loops, iterations, if conditions, etc...

.. tip::
    This syntax is for convenience only. If you want to see the metal model you create in its final json format (fully expanded with all defaults values filled in), simply use the ``-J`` option of the :ref:`setup tool <setup-tool_J>`)

Here are the top entries of the dsl::

  metaModelVersion
  gluVersion
  stateMachine
  zooKeeperRoot

  fabrics           // map where the key is fabric name
  agents            // collection
  consoles          // collection
  zooKeeperClusters // collection

  agentCli
  consoleCli

Components of the model
-----------------------

This section will describe each entry in the model. Please refer to the `source of truth <https://github.com/pongasoft/glu/tree/master/provisioner/org.linkedin.glu.provisioner-core/src/main/java/org/pongasoft/glu/provisioner/core/metamodel>`_ for an exhaustive list of all the properties.

.. tip::
   glu's distribution comes with a set of models which are a good starting point to see how they are built and understand the syntax.

.. _meta-model-configTokens:

``configTokens``
^^^^^^^^^^^^^^^^
There is a concept that will pop up in various components so let's explain it first. Every *configurable* entry has a ``configTokens`` section which is a simple json map (keys are ``String`` and values are any json valid type). The config tokens are simply passed down to any :ref:`config template <glu-config-templates>` during the setup process and as a result are available for token replacement. For example::

  // in your meta model
  agents << [
    ...
    configTokens: [
      myKey: "myValue"
    ]
  ]

  // in a config template agent-server/readme.txt.gtmpl
  This is the value of my token: ${configTokens.myKey}

.. note::
   If you use the *simpler* template type (``.xtmpl``), the config tokens will be flattened since those are just key/value pairs.

``metaModelVersion``
^^^^^^^^^^^^^^^^^^^^
The version of the meta model itself. This is in case the model changes in the future, to be able to distinguish the various formats. Current value is ``"1.0.0"`` and is optional. Example::

  metaModelVersion = "1.0.0"

``gluVersion``
^^^^^^^^^^^^^^
The version of glu itself. This mostly serves as a shortcut since it allows you to not have to define a version per component. This is an optional entry as well::

  gluVersion = "5.1.0"

.. _meta-model-zooKeeperRoot:

``zooKeeperRoot``
^^^^^^^^^^^^^^^^^
The location where to store glu's information in ZooKeeper. This is a system wide setting and is optional. It defaults to ``/org/glu``. Example::

  zooKeeperRoot = "/my/other/glu/location"

.. _meta-model-stateMachine:

``stateMachine``
^^^^^^^^^^^^^^^^
The system wide state machine. See :ref:`glu-script-state-machine` for more details. This is an optional entry and it defaults to the default state machine that comes with glu. Example::

    stateMachine = [
      defaultTransitions: [
        NONE: [[to: 's1', action: 'noneTOs1']],
        s1: [[to: 'NONE', action: 's1TOnone'], [to: 's2', action: 's1TOs2']],
        s2: [[to: 's1', action: 's2TOs1']]
      ],

      defaultEntryState: 's2'
   ]

.. _meta-model-fabric:

``fabrics``
^^^^^^^^^^^
A :term:`fabric` is only defined by the set of agents that belong to it: every agent will define the link to it fabric (rather than the other way around). The configuration points for a fabric are:

* ``keys``: the various keys to establish security while talking to the agents
* ``console``: which console (name) this fabric is hosted by
* ``zooKeeperCluster``: which ZooKeeper cluster (name) this fabric is stored

Example::

  fabrics['my-fabric-1'] = [
    keys: ...,
    console: 'tutorialConsole',
    zooKeeperCluster: 'tutorialZooKeeperCluster'
  ]

.. note::
   The format of the ``keys`` entry is not shown as this is simply generated for you during the :ref:`setup process <easy-production-setup-gen-keys>`.

.. note::
   If you do not care about securing the channel while talking to the agents then simply set keys to ``null`` (``keys: null``).

.. _meta-model-agent:

``agents``
^^^^^^^^^^
You install one agent on every host where you want glu to deploy your own application. As a result, in general there are many agents, but besides the host on which they go, the rest of the configuration is identical. Example::

  def installPath = '/opt/glu/'

  [
    'agent-host-1': 'my-fabric-1',
    'agent-host-2': 'my-fabric-2',
    'agent-host-3': 'my-fabric-1'
  ].each { agentHost, agentFabric ->

    agents << [
      host: agentHost,
      install: [
        path: installPath,
      ],
      fabric: agentFabric,
      configTokens: [:] // map of config tokens if necessary
    ]
  }

.. tip::
   If you want to change the default agent port, then simply add ``port: xxxx`` with the new port.

.. note::
   Note how the agent defines which fabric it belongs to.

.. note::
   Check the default templates for ``agent-server`` in order to know which ``configTokens`` are used by the default templates. In addition to those, the code also uses the following values to further customize the jvm::

       GLU_CONFIG_PREFIX
       GLU_ZOOKEEPER
       GLU_AGENT_NAME
       GLU_AGENT_TAGS
       GLU_AGENT_HOSTNAME_FACTORY
       GLU_AGENT_PORT
       GLU_AGENT_ADDRESS
       GLU_AGENT_FABRIC
       GLU_AGENT_APPS
       GLU_AGENT_ZOOKEEPER_ROOT
       APP_NAME
       APP_VERSION
       JAVA_HOME
       JAVA_CMD
       JAVA_CMD_TYPE
       JVM_CLASSPATH
       JVM_SIZE
       JVM_SIZE_NEW
       JVM_SIZE_PERM
       JVM_GC_TYPE
       JVM_GC_OPTS
       JVM_GC_LOG
       JVM_LOG4J
       JVM_TMP_DIR
       JVM_XTRA_ARGS
       JVM_DEBUG
       JVM_APP_INFO
       MAIN_CLASS
       MAIN_CLASS_ARGS


.. _meta-model-console:

``consoles``
^^^^^^^^^^^^
The console is the glu ui which also contains the orchestration engine. Example::

   def installPath = '/opt/glu'

   consoles << [
     name: 'my-console',
     host: 'console-host1',
     install: [
       path: installPath,
     ],
     plugins: ...,
     dataSourceDriverUri: 'http://jcenter.bintray.com/mysql/mysql-connector-java/5.1.25/mysql-connector-java-5.1.25.jar',
     configTokens: [
       dataSource: """
   def dataSourceUrl = "jdbc:mysql://mysql-host1/glu"
   dataSource.dbCreate = "update"
   dataSource.url = dataSourceUrl
   dataSource.logSql=false // set to true for details (+ open trace logging level)
   dataSource.dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
   dataSource.driverClassName = "com.mysql.jdbc.Driver"
   dataSource.username= "xxx"
   dataSource.password = "yyy"
   """,
     ]
   ]

.. note::
   The ``plugins`` syntax is not shown and is explained in the :ref:`section <goe-plugins>`.

.. note::
   The name of the console is the one used in the ``fabrics`` definition.

.. note::
   Check the default templates for ``console-server`` in order to know which ``configTokens`` are used by the default templates. In addition to those, the code also uses the following values to further customize the jvm::

       APP_NAME
       APP_VERSION
       JAVA_HOME
       JAVA_CMD
       JAVA_CMD_TYPE
       JAVA_OPTIONS
       JVM_SIZE
       JVM_SIZE_NEW
       JVM_SIZE_PERM
       JVM_GC_TYPE
       JVM_GC_OPTS
       JVM_GC_LOG
       JVM_APP_INFO
       JETTY_CMD

.. note::
   The console server is a jetty server (a web application server) and can be configured further if you want to put it behind a web server (like nginx, apache,... ). For example::

        ports: [
          mainPort: 9090,
          externalPort: 80
        ],
        internalPath: '/console',
        externalHost: 'www.glu-console.acme.org',
        externalPath: '/',

.. _meta-model-zooKeeperCluster:

``zooKeeperClusters``
^^^^^^^^^^^^^^^^^^^^^
A ZooKeeper cluster is a set of ZooKeeper that work as a cluster. This is not a glu concept: you may want to check the `Running Replicated ZooKeeper <http://zookeeper.apache.org/doc/trunk/zookeeperStarted.html#sc_RunningReplicatedZooKeeper>`_ section on the Apache ZooKeeper documentation web site. Example::

  zooKeeperClusters << [
    name: 'my-zk-cluster-1',
    configTokens: [:], // map of config tokens if necessary
    zooKeepers: [
      {
        ... // see next section 
      }
    ]
  ]

.. note::
   The name of the ZooKeeper cluster is the one used in the ``fabrics`` definition.

.. note::
   The main configuration for the cluster is the generation of the files that will then be uploaded in ZooKeeper in :ref:`easy-production-setup-zooKeeper`. Check the templates under ``zookeeper-cluster/agents`` and ``zookeeper-cluster/fabrics`` to know which ``configTokens`` are used by the default templates.

.. _meta-model-zooKeeper:

``zooKeepers``
^^^^^^^^^^^^^^
This is a single instance of a ZooKeeper server in the cluster. Example::

   def zooKeeperVersion = 'xxx'
   def installPath = '/opt/glu'

   zooKeeperClusters << [
     name: 'my-zk-cluster-1',
     zooKeepers: ['zk-host1', 'zk-host2', 'zk-host3'].collect { zkHost ->
       [
         version: zooKeeperVersion,
         host: zkHost,
         install: [
           path: installPath,
         ],
         configTokens: [:] // map of config tokens if necessary
       ]
     }
   ]

.. note::
   ZooKeeper uses 3 different ports (when used in a cluster): the client port (aka ``mainPort``, default to 2181), the leader election port (default to 3888) and the quorum port (default to 2888) which are all configurable::

      ports: [
        mainPort: 5555,
        leaderElectionPort: 10000,
        quorumPort: 20000
      ],

.. note::
   Check the default templates for ``zookeeper-server`` in order to know which ``configTokens`` are used by the default templates.
