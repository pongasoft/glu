/*
 * Copyright (c) 2013 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pongasoft.glu.provisioner.core.metamodel.impl.builder

import org.pongasoft.glu.provisioner.core.metamodel.impl.GluMetaModelImpl

/**
 * @author yan@pongasoft.com  */
public class GluMetaModelJsonGroovyDsl
{
  public static Map parseJsonGroovy(String text)
  {
    def binding = new GluMetaModelBinding(metaModelVersion: GluMetaModelImpl.META_MODEL_VERSION,
                                          gluVersion: null,
                                          stateMachine: null,
                                          zooKeeperRoot: null,
                                          fabrics: [:],
                                          agents: [],
                                          agentCli: null,
                                          consoles: [],
                                          consoleCli: null,
                                          zooKeeperClusters: [],
                                          out: System.out)

    def shell = new GroovyShell(binding)

    shell.evaluate(text)

    return binding.variables
  }

  /**
   * The purpose of this class is to restrict which variables can be used in the groovy json dsl
   */
  public static class GluMetaModelBinding extends Binding
  {
    GluMetaModelBinding(Map variables)
    {
      super(variables)
    }

    @Override
    void setVariable(String name, Object value)
    {
      if(getVariables().containsKey(name))
        super.setVariable(name, value)
      else
        throw new IllegalArgumentException("unsupported ${name} property => use [def ${name} = ${value}] instead")
    }
  }

}