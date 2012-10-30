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

package org.linkedin.glu.agent.rest.resources

import org.restlet.Context
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.InputRepresentation
import org.restlet.representation.Representation
import org.linkedin.glu.agent.api.Shell
import org.linkedin.groovy.util.lang.GroovyLangUtils
import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils

/**
 * @author yan@pongasoft.com */
public class CommandsResource extends BaseResource
{
  private final Shell _shell

  CommandsResource(Context context, Request request, Response response)
  {
    super(context, request, response);
    _shell = context.attributes['shellForCommands'] as Shell
  }

  @Override
  boolean allowPost()
  {
    return true
  }

  /**
   * Handle POST
   */
  @Override
  public void acceptRepresentation(Representation representation)
  {
    noException {
      def args = toArgs(request.originalRef.queryAsForm)

      def stdin = getCopyOfStdin(representation)

      if(stdin)
        args.stdin = stdin

      def res

      if(args.type == "shell")
      {
        res = agent.executeShellCommand(args)
        response.setEntity(toRepresentation(res: res))
      }
      else
        throw new UnsupportedOperationException("unknown command type [${args.toString()}]")
    }
  }

  /**
   * Due to the nature of HTTP, we need to first copy stdin locally because we need to return right
   * away and use the local copy instead
   *
   * @return
   */
  private InputStream getCopyOfStdin(Representation representation)
  {
    if(representation instanceof InputRepresentation)
    {
      def copyOfStdin = _shell.tempFile()

      String password = UUID.randomUUID().toString()

      try
      {
        // we store an encrypted copy of stdin since it is on the file system
        _shell.withOutputStream(copyOfStdin) { OutputStream os ->
          GluGroovyIOUtils.withStreamToEncrypt(password, os) { OutputStream eos ->
            eos << representation.stream
          }
        }

        return GluGroovyIOUtils.decryptStream(password, copyOfStdin.inputStream)
      }
      finally
      {
        // implementation note: the temp file is deleted after getting an input stream out of it
        // under Unix this is fine... the file will no longer be available on the filesystem
        // but will still "exist" until the stream is read/closed
        GroovyLangUtils.noException {
          _shell.rm(copyOfStdin)
        }
      }
    }
    else
    {
      return null
    }
  }
}