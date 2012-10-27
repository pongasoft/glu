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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.orchestration.engine.commands.CommandsService
import org.linkedin.glu.orchestration.engine.commands.CommandExecution
import org.linkedin.glu.orchestration.engine.commands.NoSuchCommandExecutionException
import javax.servlet.http.HttpServletResponse
import org.linkedin.glu.orchestration.engine.commands.StreamType
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * @author yan@pongasoft.com */
public class CommandsController extends ControllerBase
{
  CommandsService commandsService

  /**
   * Commands view page
   */
  def list = {
    // nothing special to do... simply renders the gsp
  }

  /**
   * Render the stream for the given command
   */
  def stream = {
    StreamType streamType = StreamType.valueOf(params.streamType?.toString()?.toUpperCase())

    try
    {
      commandsService.writeStream(request.fabric,
                                  params.id,
                                  streamType) { CommandExecution ce, long contentLength ->
        response.addHeader('X-glu-command-id', ce.commandId)
        if(contentLength != 0)
        {
          response.contentType = "text/plain"
          if(contentLength != -1)
            response.contentLength = contentLength
          return response.outputStream
        }
        else
          return null
      }
      null
    }
    catch (NoSuchCommandExecutionException e)
    {
      render '<not found>'
    }
  }

  /**
   * Render history according to criteria
   */
  def renderHistory = {
    println params
    def res = commandsService.findCommandExecutions(request.fabric,
                                                    params.agentId,
                                                    params)

    render(template: 'history', model: res)
  }

  /**
   * Renders a single command
   */
  def renderCommand = {
    def command = commandsService.findCommandExecution(request.fabric, params.id)

    if(command)
      render(template: 'command', model: [command: command])
    else
      render "not found"
  }

  /**
   * Writes the stream requested
   *
   * curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/command/2d044e0b-a1f5-4cbd-9210-cf42c77f6e94/stdout"
   */
  def rest_show_command_execution_stream = {
    StreamType streamType = StreamType.valueOf(params.streamType?.toString()?.toUpperCase())

    try
    {
      commandsService.writeStream(request.fabric,
                                  params.id,
                                  streamType) { CommandExecution ce, long contentLength ->
        response.addHeader('X-glu-command-id', ce.commandId)
        if(contentLength != 0)
        {
          response.contentType = "application/octet-stream"
          if(contentLength != -1)
            response.contentLength = contentLength
          return response.outputStream
        }
        else
        {
          response.sendError(HttpServletResponse.SC_NO_CONTENT, "no content for ${streamType}")
          render ''
          return null
        }
      }
    }
    catch (NoSuchCommandExecutionException e)
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND)
      render ''
    }
  }
}