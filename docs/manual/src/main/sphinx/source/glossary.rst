.. Copyright (c) 2011-2013 Yan Pujante

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License. You may obtain a copy of
   the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
   License for the specific language governing permissions and limitations under
   the License.

Glossary
========

.. glossary::

  Agent
     The glu agent is an active process that needs to run on every :term:`host` where applications need to be deployed

  bounce
     In the context of the orchestration engine, bounce means *stop* followed by *start*.

  Closure
     In a glu script, a closure is a `groovy closure <http://groovy.codehaus.org/Closures>`_ which is essentially a piece of groovy/java code (between curly braces) assigned to an attribute.

  command
     Any kind of (unix) shell command (ex: ``uptime``, ``echo foo > /tmp/file.txt``, etc...)

  cli
     An application/tool that runs and then exists when the task is complete. In glu, clis are self contained and packaged under a directory structure with a ``bin/xxxx.sh`` to start the application/tool.

  Console
    The webapp/REST api built on top of ZooKeeper which is the orchestrator of the system.

  delta
    The differences between the :term:`desired state` and the :term:`live state`.

  deployment plan
    A deployment plan is a set of instructions that the :term:`orchestration engine` executes to deploy and start (resp. stop and undeploy) applications on various hosts.

  desired state
    The state you want the entire system to be in. It is represented by the :term:`model`.

  dotted notation
    This notation is how groovy allows you to access any entry in a map::

      // with the following map
      Map m = [ 
        p1: 'v1',
        p2: [ p3: 'v3'] // nested map!
        c1: [ [p4: 'v4'], [p4: 'v5'] ] // a collection of maps
      ]

      // the dotted notation allows you to write
      m.p1 // which is 'v1'
      m.p2.p3 // which is 'v3'
      m.c1[0].p4 // which is 'v4'
      m.c1[1].p4 // which is 'v5'

  Fabric
    A fabric defines a group of agents. When an agent starts it is assigned a fabric (a string). A fabric is then defined as the group of agents that were started with the same value for the fabric.

  filter
    A filter is used in the orchestration engine to select a set of entries based on some criteria.

  glu script
    A glu script is a set of instructions backed by a state machine that the agent knows how to run. :doc:`View more information <glu-script>`.

  host
    A host represents a physical or virtual instance of a machine running 1 os. Examples 

     * a laptop with Mac OS X is 1 host
     * a unix desktop running 4 Xen virtual machine is 5 hosts (unix destop + 4 VMM)

    .. note:: in a production setup, there is usually one (and only one) glu :term:`agent` running on each host

  live model
    Representation of the :term:`live state` as json document. More information about :ref:`the live model <goe-live-model>`.

  live state
    The state of the *live* system as reported by the agents in ZooKeeper. This state is dynamically collected by the :term:`orchestration engine`.

  meta model
    The model which describes the glu setup itself (agents, consoles, fabrics, etc...). Do not confuse with the :term:`model` described below.

  Metadata
    Metadata in the context of glu represents a map that can be represented as a json object::

      def goodMetadata = 
      [
        p1: 'v1',
        p2: [1, 2, 3], // array
        p3: [p31: 'v31'] // another nested map
      ]

      // in json format, it would look like this
      // note that you cannot have comments in json!
      {
        "p1": "v1",
        "p2": [1, 2, 3],
        "p3": { "p31": "v31" }
      }

      // bad because the value is a java object
      def badMetadata =
      [
        color: java.awt.Color.BLACK
      ]

  model
    The model is a json document which describes what applications need to run, on which host and what it means to deploy and run an application.

  mount point
    The unique key on which a glu script get 'mounted' on a given agent. It is a ``String`` which has a (unix) path like syntax (must start with a ``/``)::

      Example: /a/b/c

  node
    Synonym for :term:`host`

  orchestration engine
    The orchestration engine is the process that listens to ZooKeeper updates coming from the agents, compute differences (:term:`delta`) with the :term:`model` in order to visualize them and/or execute a deployment plan. The orchestration engine is in charge of orchestrating deployments making sure they happen either sequentially or in parallel (or a combination of both).

  server
     An application that is long lived and usually terminates when asked to do so. In glu, servers are self contained and packaged under a directory structure with a ``bin/xxxx.sh`` shell script. The command ``bin/xxxx.sh start`` is used to start the server. The command ``bin/xxxx.sh stop`` is used to stop the server.

  static model
    Synonym for :term:`model`. More information about :ref:`the static model <static-model>`.

  system
    The system represents the set of hosts and applications running in a fabric. Its static representation is the :term:`model`.

  Timer
    A piece of logic that gets executed at a given frequency by the agent. Scheduled/Cancelled by a glu script.

  ZooKeeper
    View more information about `ZooKeeper <http://hadoop.apache.org/zookeeper/>`_
