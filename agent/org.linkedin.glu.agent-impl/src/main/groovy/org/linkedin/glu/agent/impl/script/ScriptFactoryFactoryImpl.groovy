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

import org.linkedin.util.url.QueryBuilder

/**
 * @author yan@pongasoft.com */
public class ScriptFactoryFactoryImpl extends AbstractScriptFactoryFactory
{
  @Override
  protected ScriptFactory doCreateScriptFactory(def args)
  {
    if(args.scriptFactory)
    {
      return args.scriptFactory
    }

    if(args.scriptClassName)
    {
      return new FromClassNameScriptFactory(args.scriptClassName, args.scriptClassPath)
    }

    if(args.scriptLocation)
    {
      // add support for class:/<fqcn>?cp=<cp1>&cp=<cp2>...
      if(args.scriptLocation.toString().startsWith("class:/"))
      {
        URI uri = new URI(args.scriptLocation.toString())
        QueryBuilder query = new QueryBuilder()
        query.addQuery(uri)
        return new FromClassNameScriptFactory(uri.path - '/',
                                              query.getParameterValues("cp")?.collect { it })
      }
      else
        return new FromLocationScriptFactory(args.scriptLocation)
    }

    return null
  }
}