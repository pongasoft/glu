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

package org.linkedin.glu.commands.impl

import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.io.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.text.SimpleDateFormat
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils

/**
 * @author yan@pongasoft.com */
public class FileSystemCommandExecutionIOStorage extends AbstractCommandExecutionIOStorage
{
  public static final String MODULE = FileSystemCommandExecutionIOStorage.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = true)
  FileSystem commandExecutionFileSystem

  @Initializable
  String stdinStreamFileName = 'stdin.stream'

  @Initializable
  String stdoutStreamFileName = 'stdout.stream'

  @Initializable
  String stderrStreamFileName = 'stderr.stream'

  @Initializable
  String commandFileName = 'command.json'

  @Initializable
  SimpleDateFormat dateFormat = new SimpleDateFormat('yyyy/MM/dd/HH/z')

  /**
   * Contains the storage of the commands while capture IO is executing only!
   */
  final Map<String, CommandExecution> commands = [:]

  @Override
  CommandExecution findCommandExecution(String commandId)
  {
    CommandExecution commandExecution

    synchronized(commands)
    {
      commandExecution = commands[commandId]
    }

    // already in the map, meaning IO is being captured... simply return it
    if(commandExecution != null)
      return commandExecution

    // not in the map, needs to rebuild it from file system
    def baseDir = computeDir(commandId)
    def commandFile = baseDir.createRelative(commandFileName)
    if(commandFile.exists())
    {
      def args = commandFile.withInputStream { InputStream is ->
        JsonUtils.fromJSON(new BufferedInputStream(is).text)
      }

      // if we read from the file system there should be an exit value already
      def exitValue = args.remove('exitValue')
      def exitValueProvider = { exitValue }

      // or if something bad really happened
      def exception = GluGroovyJsonUtils.rebuildException(args.remove('exception'))
      if(exception)
        exitValueProvider = { throw exception }

      long startTime = args.remove('startTime') ?: 0
      long completionTime = args.remove('completionTime') ?: 0

      commandExecution = new CommandExecution(commandId, args)
      commandExecution.startTime = startTime
      commandExecution.completionTime = completionTime
      commandExecution.storage = createStorage(commandExecution)

      FutureTaskExecution fe = new FutureTaskExecution(exitValueProvider)
      fe.clock = clock
      commandExecution.futureExecution = fe

      try
      {
        fe.runSync()
      }
      catch(Throwable th)
      {
        // in case of error, then one will be raised!
        // no need to log it
      }

      commandExecution.command = gluCommandFactory.createGluCommand(commandExecution)

      return commandExecution
    }
    else
      return null
  }

  @Override
  protected AbstractCommandStreamStorage saveCommandExecution(CommandExecution commandExecution,
                                                              def stdin)
  {
    def storage = createStorage(commandExecution)

    // create the directory that will contain the command
    commandExecutionFileSystem.mkdirs(storage.baseDir)

    def commandFile = storage.commandResource

    // atomically try to create the command file => ensures uniqueness!
    if(!commandFile.file.createNewFile())
      throw new IllegalArgumentException("duplicate command id [${commandExecution.id}]")

    def args = [*:commandExecution.args]
    args.startTime = commandExecution.startTime

    // save the command to the file system
    new BufferedOutputStream(new FileOutputStream(commandFile.file)).withStream { out ->
      out << JsonUtils.compactPrint(args)
    }

    // save stdin if there is any
    if(stdin)
      storage.withOrWithoutStorageOutput(StreamType.stdin) { it << stdin }

    return storage
  }

  @Override
  protected def captureIO(CommandExecution commandExecution, Closure closure)
  {
    synchronized(commands)
    {
      if(commands.containsKey(commandExecution.id))
        throw new IllegalStateException("already capturing IO for ${commandExecution.id}")

      commands[commandExecution.id] = commandExecution
    }

    try
    {
      def res = null
      Throwable exception
      try
      {
        res = closure(commandExecution.storage)
        exception = res.exception
      }
      catch(Throwable th)
      {
        exception = th
      }

      // close all open output stream
      commandExecution.storage.close()

      // now we update the command with the exit value
      def args = [*:commandExecution.args]
      if(exception != null)
        args.exception = GluGroovyJsonUtils.exceptionToJSON(exception)
      else
        args.exitValue = res?.exitValue
      args.startTime = commandExecution.startTime
      args.completionTime = res?.completionTime ?: clock.currentTimeMillis()

      // save the command to the file system
      def fos = new FileOutputStream(commandExecution.storage.commandResource.file)
      new BufferedOutputStream(fos).withStream { out ->
        out << JsonUtils.compactPrint(args)
      }

      return [
        exitValue: args.exitValue,
        exception: exception,
        completionTime: args.completionTime
      ]
    }
    finally
    {
      synchronized(commands)
      {
        commands.remove(commandExecution.id)
      }
    }
  }

  private FileSystemStreamStorage createStorage(CommandExecution commandExecution)
  {
    new FileSystemStreamStorage(ioStorage: this,
                                commandExecution: commandExecution,
                                baseDir: computeDir(commandExecution))
  }

  private String computePath(String commandId)
  {
    // YP note: SimpleDateFormat is NOT thread safe!
    synchronized(dateFormat)
    {
      "${dateFormat.format(new Date(extractStartTimeFromCommandId(commandId)))}/${commandId}"
    }
  }

  /**
   * This method relies on the way the id is built in the abstract class!
   */
  private long extractStartTimeFromCommandId(String commandId)
  {
    int idx = commandId.indexOf("-")
    if(idx == -1)
      throw new IllegalArgumentException("invalid commandId [${commandId}]")
    try
    {
      Long.parseLong(commandId[0..<idx], 16) // hexa
    }
    catch(NumberFormatException e)
    {
      throw new IllegalArgumentException("invalid commandId [${commandId}]", e)
    }
  }

  private Resource computeDir(CommandExecution commandExecution)
  {
    commandExecutionFileSystem.toResource(computePath(commandExecution.id))
  }

  private Resource computeDir(String commandId)
  {
    commandExecutionFileSystem.toResource(computePath(commandId))
  }
}
