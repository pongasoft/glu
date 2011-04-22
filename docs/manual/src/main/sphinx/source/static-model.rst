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

.. |static-model-logo| image:: /images/static-model-logo-86.png
   :alt: static model
   :class: header-logo

.. _static-model:

|static-model-logo| Static Model
================================

The static model describes what applications need to run, on which host and what it means to deploy and run an application. It is represented by a json document.

Structure of the model
----------------------

.. sidebar:: Empty Model

   This is a totally valid model. It is simply empty: if you load this model in the orchestration engine, it means that you want your system to be empty which will trigger undeploying everything. 

   .. note:: This is equivalent to running the ``undeploy`` command for everything (``-a`` option) with the difference that the orchestration engine will detect a delta after running ``undeploy`` on a non empty system.


The most basic structure of the model is the following::

  {
    "fabric": "glu-dev-1",
    "metadata": {}
    "agentTags": {},
    "entries": [],
  }

.. note:: The model is internally represented by the `SystemModel <https://github.com/linkedin/glu/blob/master/provisioner/org.linkedin.glu.provisioner-core/src/main/groovy/org/linkedin/glu/provisioner/core/model/SystemModel.groovy>`_ (groovy) class.

``fabric``
^^^^^^^^^^

The fabric section is required and specifies which :term:`fabric` this model is for. It is a simple string.

.. note:: if you have more than 1 fabric, you need to have 1 model for each one of them 

``metadata``
^^^^^^^^^^^^

This section is of type :term:`metadata` and can contain any kind of information you want to store alongside your model. One way to think about it is `structured comments`. The console can be configured to display and/or use some of this information (TODO: add link). 

``agentTags``
^^^^^^^^^^^^^

This section contains :ref:`tags <static-model-tagging>` that you want to assign to each entries that are deployed on this agent.

It is represented as map where:

* the key is the name of an agent
* the value is an array of tags (a tag is a string)

Example::

  "agentTags": {
    "agent-1": ["small-instance", "osx"],
    "agent-2": ["large-instance", "linux"]
  },

.. tip:: ``agentTags`` are no more than a shortcut to assign the same set of tags to all entries assigned to the agent (see :ref:`static-model-entry-tags`)

``entries``
^^^^^^^^^^^

This section is an array of entries. An entry describes where a particular instance of an application need to be deployed, and how to deploy it. An entry is represented like this in json::

  {
    "agent": "node01.prod",
    "mountPoint": "/search/i001",

    "script": "http://repository.prod/scripts/webapp-deploy-1.0.0.groovy",
    "initParameters": {},
    "metadata": {},
    "tags": []
  }

.. note:: An entry is internally represented by the `SystemEntry <https://github.com/linkedin/glu/blob/master/provisioner/org.linkedin.glu.provisioner-core/src/main/groovy/org/linkedin/glu/provisioner/core/model/SystemEntry.groovy>`_ (groovy class).

.. tip:: If you check :ref:`agent-glu-script-engine`, you will be able to understand better why an entry is defined this way:

   * ``agent`` represents which agent to talk to
   * ``mountPoint``, ``script`` and ``initParameters`` are the parameters provided to the ``installScript`` api
   
   .. note:: ``tags`` are only used in the console

``agent``
"""""""""

This section describe on which agent the application needs to be installed.

.. note:: This has to be the name of the agent as defined by :ref:`agent-fabric-and-name`. In most cases the name of the agent is the hostname, but since it is configurable, it may be different. This is so that it is possible to start more than one agent on a single node (which is very useful for development purposes).


``mountPoint``
""""""""""""""

The :term:`mount point` represents a unique key on the agent. You can reuse the same value for a different agent. 

.. tip:: This value is predominently displayed in the console so in general it is better to give it a very meaningful value. For example ``/search/i001`` describes the fact that it is the *search* application, instance *001*. You are of course free to use whichever convention you would like.

``script``
""""""""""

This section should be a URI pointing to the :doc:`glu script <glu-script>` that will be used to deploy the application.

``initParameters``
""""""""""""""""""

This section describes the initialization parameters that are going to be provided to the *script*. It is of type :term:`metadata` and can contain whatever values you want to provide to the script. Example::

    "initParameters": {
       "container": {
         "skeleton": "http://repository.prod/tgzs/jetty-7.2.2.v20101205.tgz",
         "config": "http://repository.prod/configs/search-container-config-2.1.0.json",
         "port": 8080,
       },
       "webapp": {
         "war": "http://repository.prod/wars/search-2.1.0.war",
         "contextPath": "/",
         "config": "http://repository.prod/configs/search-config-2.1.0.json"
       }
    }

.. tip:: The values you use in this section are used to compute the :term:`delta`! This is how the orchestration engine determines that an application needs to be upgraded (because the version has changed)!

``metadata``
""""""""""""

This section is of type :term:`metadata` and can contain any kind of information you want to store alongside this entry. The model itself also has a ``metadata`` section but this one is specific to the entry and each entry can have its own. The console can be configured to display and filter on ``metadata`` (TODO add link).

.. note:: unlike the ``initParameters`` section, ``metadata`` is **not** used to compute the delta.

.. _static-model-entry-tags:

``tags``
""""""""

This section is an array of tags. The console can be configured to display and filter on ``tags``. See :ref:`static-model-tagging` for more information.

.. _static-model-tagging:

Tagging
-------

The static model has 2 ways of defining tags:

1. through ``agentTags`` for the entire model
2. through ``tags`` for a particular entry

What is a tag?
^^^^^^^^^^^^^^

A tag is a simple piece of information (a simple string) that can be associated to an entity. You may be familiar with the concept under a different name: *label*. There are lots of system using this concept. For example, *gmail* allows you to associate any number of labels to an email (thus simulating folders but more powerfull because the email can be in more than one folder!).

Example of tags: ``frontend``, ``backend``, ``linux``, ``cluster-search-1``, ``cluster-seach-2``, ...

Why would you use tags?
^^^^^^^^^^^^^^^^^^^^^^^

As you saw in the previous sections, the system model is a rather flat structure: a simple array of entries. It was actually designed this way on purpose because glu does not want to impose how you want to model your system. For example, for some, a *cluster* means something, for others it means something different.

Tags allow you to add meaning to the model that glu does not know about (and does not have to) (for glu *cluster* means nothing :). Tags are then used in very powerful ways:

* display: the console displays tags (you can actually configure the color (TODO add link))
* filtering: there is an entire section about :doc:`filtering <filtering>` and how it works but quickly speaking it allows you to constraint glu on what to do. For example, you can tell glu to :term:`bounce` all applications that are part of the first search cluster running on linux nodes.

What about ``metadata``?
^^^^^^^^^^^^^^^^^^^^^^^^

``metadata`` are very similar to ``tags`` with the only difference that ``metadata`` are structured. The console can also display ``metadata`` (albeit not like tags), and use it for filtering. In general, ``metadata`` is more heavyweight than ``tags`` so if you have a choice, you should use ``tags``.

.. image:: /images/static-model-tags-metadata.png
   :align: center
   :alt: tags and metadata

Example::

  // Expressing the same information
  
  // using metadata
  {
    "metadata": {
      "cluster": "search-1",
      "application": {
        "kind": "webapp"
      }
    }
  }

  // using tags
  {
    "tags": ["cluster-search-1", "webapp"]
  }

.. note:: As you can see in the :doc:`filtering <filtering>` section, expressing filters with tags is simpler and can result in faster results.
