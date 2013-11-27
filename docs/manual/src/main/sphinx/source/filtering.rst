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

.. |goe-filter-logo| image:: /images/goe-filter-logo-66.png
   :alt: filter
   :class: header-logo
 
|goe-filter-logo| Filtering
===========================

.. sidebar:: Filtering

   Filtering is a very powerfull feature that allows you run complex queries on the model (see :ref:`goe-filter-syntax` for some examples of queries)

As explained in the :doc:`static-model` section, the system model is a rather flat structure. You use ``tags`` (and ``metadata``) to add (your own) structure to it. *Filtering* is then what allows you to bring it to life in the glu orchestration engine.


Filtering in action
-------------------

As you have seen in the :doc:`orchestration-engine` section, the live model and the static model are both fed to the delta service to compute the delta. When you use filtering, what happens is that you *inject* a :term:`filter` (the same one!) between the full live model and the full static model to compute a filtered live model and a filtered static model which then get fed to the delta service as shown in the following diagram:

.. image:: /images/goe-filtering-413.png
   :align: center
   :alt: Filtering

A model contains an array of entries. After the filter, the model contains a subset of the original array. Another way to think about it would be a selection in SQL::

 SELECT entries FROM model WHERE xxxx

 => xxx is the filter

Filtering in the console
------------------------

From the ``Dashboard``, the links represent individual filters: clicking on a link `adds` a filter. There is also a way to remove them.

.. tip:: The ``Dashboard`` is a table view representation of the model. Adding a filter essentially `hides` some rows from the table.

.. image:: /images/filtering-console-600.png
   :align: center
   :alt: Filtering in the console

.. _goe-filter-cli:

Filtering from the command line
-------------------------------

.. sidebar:: dotted notation

   For the sake of the :term:`dotted notation`, every entry is treated as a map. You can access *any* field by using the dotted notation::

     agent
     initParameters.<xxx>
     metadata.<yyy>
     mountPoint
     script
     tags (+ special syntax, see below)

At this point in time, the console allows you to filter but does not allow you to create any kind of filter (this will be part of a future release).

The :ref:`command line <goe-cli>` (as well as the :ref:`REST api <goe-rest-api>`), on the other hand, allows you to express any kind of filtering you want.

From the command line you use the ``-s`` or ``-S`` option to specify a filter.

.. _goe-filter-syntax:

Filter syntax
^^^^^^^^^^^^^

The filter syntax is a dsl and allows you to filter by any field of the entry using a :term:`dotted notation`. An entry looks like this (you get this when doing ``/system/live`` or ``/system/model``)::

    {
      "agent": "ei2-app3-zone5.qa",
      "initParameters": {
        "skeleton": "ivy:/com.linkedin.network.container/container-jetty/0.0.007-RC1.1",
        "webapps": [
          {
            "war": "http://localhost:8080/glu/repository/wars/webapp1.war",
            "contextPath": "/cp1",
            "monitor": "/monitor"
          },
          {
            "war": "http://localhost:8080/glu/repository/wars/webapp2.war",
            "contextPath": "/cp2",
            "monitor": "/monitor"
          }
          ]
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
      "script": "ivy:/com.linkedin.glu.glu-scripts/glu-scripts-jetty/3.0.0/script",
      "tags": ["frontend", "webapp"]
    }

The dsl has the following syntax::

    and / or / not => to do logic
    <dotted notation>='<value>' => to express the matching criteria
    tags.hasAny('tag1[;tagN]*') => entry with any of the provided tag
    tags.hasAll('tag1[;tagN]*') => entry with all of the provided tag
    tags='tag1[;tagN]*' => shortcut for tags.hasAll('tag1;tag2')


Examples:

1. Only container 'jobs-server'::

        metadata.container.name='jobs-server'

2. Container is 'jobs-server' or 'activemq'::

        or { 
          metadata.container.name='jobs-server'
          metadata.container.name='activemq'
        }

        // can be compacted on 1 line as:
        or{metadata.container.name='jobs-server';metadata.container.name='activemq'}

3. All containers that are not running (on live system only of course)::

        not {
          metadata.currentState='running'
        }

        // can be compacted on 1 line as:
        not{metadata.currentState='running'}

4. All containers not running on agent ei2-app3-zone5.qa (on live system only of course)::

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

5. All webapps (tag filtering)::

        tags='webapp'

        // equivalent to
        tags.hasAll('webapp')

        // equivalent to (because only 1 tag provided)
        tags.hasAny('webapp')

6. All frontent or backend (tag filtering)::

        tags.hasAny('frontend;backend')

        // equivalent to but discouraged as the previous notation will be much faster!
        or {
          tags='frontend'
          tags='backend'
        }

7. All entries where there is at least one webapp with contextPath '/cp1'::

        // "webapps" is a collection... which can be accessed using [] notation
        // [0..-1] means "all items in the collection"
        // so the filter means "any item where contextPath is '/cp1'"
        initParameters.webapps[0..-1].contextPath = '/cp1'

8. All entries where the 2nd webapp has a contextPath '/cp2'::

        // "webapps" is a collection... which can be accessed using [] notation
        // [1] means "item 2 in the collection" (0 is the first entry, so 1 is the second one)
        // so the filter means "second item where contextPath is '/cp2'"
        initParameters.webapps[1].contextPath = '/cp2'

.. note:: The REST api is expecting the filter as a query parameter (``systemFilter``) and it needs to be properly url encoded.
   For example it should be:: 

      systemFilter=not%7bmetadata.currentState%3d'running'%7d

   The command line will do the encoding for you so you would just use::

      ... -s "not{metadata.currentState='running'}"

