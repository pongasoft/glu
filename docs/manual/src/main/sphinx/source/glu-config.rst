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

 
.. _glu-config:

Configuring glu
===============

glu is very configurable and offers many ways of configuring:

 * simple tweaks like port numbers in the meta model
 * more advanced tweaks, like jvm parameters, in the meta model (``configTokens`` section)
 * configs roots which lets you add/delete/modify any file in the distributions that will be generated during setup
 * console plugins to extend/modify the behavior of the console

.. tip::
   Although glu offers many configuration point, the defaults are usually sensible and you should be good without tweaking anything until there is a specific need.

Configuration concepts
----------------------
.. note::
   These concepts are new since glu 5.1.0. If you are using a prior version of glu, the configuration is mostly manual. The documentation that comes bundled with glu has more details.


.. image:: /images/glu-configuration-800.png
   :align: center
   :alt: glu configuration

* glu comes bundled with several components (:term:`clis <cli>` and :term:`servers <server>`) distributed as *raw* packages under the ``packages/`` top folder.
* glu also comes bundled with a tool (``setup.sh``), that is used as part of the :ref:`setup process <easy-propduction-setup-gen-dist>` to generate the distributions (which are ready to install/run packages).
* In order for the setup tool to generate the right set of packages tailored for **your** environment, you need to define, at the very least, a glu :term:`meta model` which essentially describes where each component needs to go (on which host they are installed).
  * In addition, the meta model lets you tweak several configuration parameters if the default ones are not satisfactory.
* The setup tool also lets you provide additional ``configs`` directory structure to further tweak what goes inside the distributions (for example, you can replace entirely the glu logo with your own, substitute a library with another one, change any startup script, etc...).

Understanding the setup workflow
--------------------------------
When you generate the distributions (:ref:`easy-propduction-setup-gen-dist`), this is what happens:

  * the glu meta model is parsed to build an in-memory representation of the model
  * based on information in the glu meta model, for each *raw* package

    * the package is copied under the *outputFolder* (with the proper naming)
    * each template in the ``configs`` folder(s) for this package is processed (this step uses information coming from the meta model including config tokens)
    * if requested, the package is compressed (``.tgz``)



``configTokens``
----------------



``configs``
-----------
