/*
 * Copyright (c) 2012 Yan Pujante
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



package org.linkedin.glu.agent.impl.script

import org.linkedin.util.annotations.Initializable

/**
 * @author yan@pongasoft.com */
public abstract class AbstractScriptFactoryFactory implements ScriptFactoryFactory
{
  @Initializable
  AbstractScriptFactoryFactory scriptFactoryFactory

  void chain(AbstractScriptFactoryFactory scriptFactoryFactory)
  {
    if(this.scriptFactoryFactory == null)
      this.scriptFactoryFactory = scriptFactoryFactory
    else
      this.scriptFactoryFactory.chain(scriptFactoryFactory)
  }

  @Override
  final ScriptFactory createScriptFactory(def args)
  {
    if(args instanceof ScriptDefinition)
      args = args.scriptFactoryArgs

    def scriptFactory = doCreateScriptFactory(args)

    if(scriptFactory == null)
      scriptFactory = scriptFactoryFactory?.createScriptFactory(args)

    if(scriptFactory)
      return scriptFactory
    else
      throw new IllegalArgumentException("cannot determine script factory: ${args}")
  }

  protected abstract ScriptFactory doCreateScriptFactory(def args)
}