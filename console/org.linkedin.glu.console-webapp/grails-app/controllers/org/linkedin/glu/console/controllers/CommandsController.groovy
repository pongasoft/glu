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

import org.linkedin.glu.orchestration.engine.commands.NoSuchCommandExecutionException
import javax.servlet.http.HttpServletResponse

import org.linkedin.glu.utils.core.Sizeable

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
   * Render the stream(s) for the given command
   */
  def streams = {
    rest_show_command_execution_streams()
  }

  /**
   * Render history according to criteria
   */
  def renderHistory = {
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
  def rest_show_command_execution_streams = {
    try
    {
      commandsService.withCommandExecutionAndWithOrWithoutStreams(request.fabric,
                                                                  params.id,
                                                                  params) { args ->
        response.addHeader('X-glu-command-id', params.id)
        if(args.stream)
        {
          response.contentType = "application/octet-stream"
          def contentLength = -1
          if(args.stream instanceof Sizeable)
            contentLength = args.stream.size
          if(contentLength != -1)
            response.contentLength = contentLength

          response.outputStream << args.stream
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