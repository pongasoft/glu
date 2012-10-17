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

package org.linkedin.glu.orchestration.engine.commands

import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.io.resource.Resource
import java.text.SimpleDateFormat
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com */
public class FileSystemCommandExecutionIOStorage implements CommandExecutionIOStorage
{
  public static final String MODULE = FileSystemCommandExecutionIOStorage.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = true)
  FileSystem commandExecutionFileSystem

  @Initializable
  String stdinStreamFileName = 'stdin.stream'

  @Initializable
  String resultStreamFileName = 'result.stream'

  @Initializable
  SimpleDateFormat dateFormat = new SimpleDateFormat('yyyy/MM/dd/HH/z')

  private class FileSystemStreamStorage implements StreamStorage
  {
    CommandExecution commandExecution
    Resource dir

    FileSystemStreamStorage()
    {
      dir = commandExecutionFileSystem.createTempDir()
    }

    @Override
    OutputStream findStdinStorage()
    {
      new FileOutputStream(dir.createRelative(stdinStreamFileName).file)
    }

    @Override
    OutputStream findResultStreamStorage()
    {
      new FileOutputStream(dir.createRelative(resultStreamFileName).file)
    }
  }

  @Override
  def withStdinInputStream(CommandExecution commandExecution, Closure closure)
  {
    withInputStream(commandExecution, stdinStreamFileName, closure)
  }


  @Override
  def withResultInputStream(CommandExecution commandExecution, Closure closure)
  {
    withInputStream(commandExecution, resultStreamFileName, closure)
  }

  @Override
  def captureIO(Closure closure)
  {
    def storage = new FileSystemStreamStorage()

    def res = closure(storage)

    // we rename the temporary directory into a name specific to the command
    def p = commandExecutionFileSystem.mv(storage.dir, computePath(storage.commandExecution))

    log.info ("moved ${storage.dir.file.canonicalPath} to ${p.file.canonicalPath}")

    return res
  }

  private String computePath(CommandExecution commandExecution)
  {
    // YP note: SimpleDateFormat is NOT thread safe!
    synchronized(dateFormat)
    {
      "${dateFormat.format(new Date(commandExecution.startTime))}/${commandExecution.commandId}"
    }
  }

  private def withInputStream(CommandExecution commandExecution,
                              String streamName,
                              Closure closure)
  {
    def baseResource = commandExecutionFileSystem.toResource(computePath(commandExecution))
    def streamResource = baseResource.createRelative(streamName)
    if(streamResource.exists())
    {
      def file = streamResource.file
      new BufferedInputStream(new FileInputStream(file)).withStream { stream ->
        closure(stream, file.size())
      }
    }
    else
      closure(null, null)
  }
}
