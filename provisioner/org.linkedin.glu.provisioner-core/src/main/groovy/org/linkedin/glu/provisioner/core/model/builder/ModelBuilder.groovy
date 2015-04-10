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

package org.linkedin.glu.provisioner.core.model.builder

import com.fasterxml.jackson.core.JsonParseException
import org.codehaus.groovy.control.CompilationFailedException
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.io.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com  */
public abstract class ModelBuilder<M>
{
  public static final String MODULE = ModelBuilder.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  ModelBuilder<M> deserializeFromJsonInputStream(InputStream inputStream,
                                                 String filename)
  {
    deserializeFromJsonString(inputStream?.text, filename)
  }

  ModelBuilder<M> deserializeFromJsonResource(Resource resource)
  {
    resource?.inputStream?.withStream { InputStream inputStream ->
      deserializeFromJsonInputStream(inputStream, resource.filename)
    }

    return this
  }

  ModelBuilder<M> deserializeFromJsonString(String jsonModel,
                                            String filename)
  {
    if(jsonModel != null)
    {
      if(filename?.endsWith('.json.groovy'))
        deserializeFromJsonGroovyDsl(jsonModel)
      else
        deserializeFromJsonString(jsonModel)
    }

    return this
  }


  ModelBuilder<M> deserializeFromJsonString(String jsonModel)
  {
    Map jsonMapModel

    try
    {
      jsonMapModel = (Map) JsonUtils.fromJSON(jsonModel?.trim())
    }
    catch(JsonParseException jpe)
    {
      def lines = []
      int i = 1
      jsonModel.eachLine { line ->
        lines << "[${i++}] ${line}"
      }

      int errorLine = jpe.location.lineNr - 1 // 1 based

      def minLine = Math.max(0, errorLine - 5)
      def maxLine = Math.min(lines.size() - 1, errorLine + 5)

      def excerpt = """${'/' * 20}
${lines[minLine..maxLine].join('\n')}
${'/' * 20}"""

      log.error(excerpt)

      throw new ModelBuilderParseException(jpe, excerpt)
    }

    deserializeFromJsonMap(jsonMapModel)
  }

  ModelBuilder<M> deserializeFromJsonGroovyDsl(String jsonModel)
  {
    Map jsonMapModel
    try
    {
      jsonMapModel = doParseJsonGroovy(jsonModel)
    }
    catch(CompilationFailedException cfe)
    {
      def lines = []
      int i = 1
      jsonModel.eachLine { line ->
        lines << "[${i++}] ${line}"
      }

      def excerpt = """${'/' * 20}
${lines.join('\n')}
${'/' * 20}"""

      log.error(excerpt)

      throw new ModelBuilderParseException(cfe, excerpt)
    }

    deserializeFromJsonMap(jsonMapModel)
  }

  abstract ModelBuilder<M> deserializeFromJsonMap(Map jsonModel)

  abstract M toModel()

  protected abstract Map doParseJsonGroovy(String jsonModel)

}