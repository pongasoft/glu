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

import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.utils.io.LimitedInputStream
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.annotations.Initializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeoutException
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils

/**
 * @author yan@pongasoft.com */
public abstract class AbstractCommandExecutionIOStorage implements CommandExecutionIOStorage
{
  public static final String MODULE = AbstractCommandExecutionIOStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = false)
  Clock clock = SystemClock.INSTANCE

  @Initializable(required = true)
  GluCommandFactory gluCommandFactory

  @Override
  CommandExecution createStorageForCommandExecution(def args)
  {
    String commandId =
      "${Long.toHexString(clock.currentTimeMillis())}-${UUID.randomUUID().toString()}"

    CommandExecution commandExecution =
      new CommandExecution(args, gluCommandFactory.createGluCommand(commandId, args))
    commandExecution.storage = this

    return saveCommandExecution(commandExecution)
  }

  /**
   * Should save the command in persistent state
   * @return the command (eventually tweaked)
   */
  protected abstract CommandExecution saveCommandExecution(CommandExecution commandExecution)

  /**
   * {@inheritdoc}
   */
  @Override
  def findCommandExecutionAndStreams(String commandId, Object args)
  {
    int numberOfStreams = 0

    CommandExecution commandExecution = findCommandExecution(commandId)

    if(commandExecution == null)
      return null

    def res = [:]

    res.commandExecution = commandExecution

    Map<String, InputStream> streams = [:]

    def exitValueStream = Config.getOptionalBoolean(args, 'exitValueStream', false)

    // Factory to compute the exit value
    def exitValueFactory = { null }

    if(exitValueStream)
      numberOfStreams++

    def timeout = args.exitValueStreamTimeout

    if(exitValueStream && (timeout != null || commandExecution.isCompleted()))
    {
      exitValueFactory = {
        try
        {
          return commandExecution.getExitValue(timeout).toString()
        }
        catch(TimeoutException e)
        {
          if(log.isDebugEnabled())
            log.debug("timeout reached", e)
          // ok: ignored...
          return null
        }
      }

      InputStream exitValueInputStream = new InputGeneratorStream(exitValueFactory)
      streams[StreamType.EXIT_VALUE.multiplexName] = exitValueInputStream
    }

    [StreamType.STDIN, StreamType.STDOUT, StreamType.STDERR].each { streamType ->

      def name = streamType.toString().toLowerCase()

      // is the stream requested?
      if(Config.getOptionalBoolean(args, "${name}Stream", false))
      {
        numberOfStreams++

        def inputStreamFactory = {

          // either wait or not
          exitValueFactory()

          def m = findInputStreamWithSize(commandId, streamType)
          if(m != null)
          {
            long offset = GluGroovyLangUtils.getOptionalLong(args, "${name}Offset", 0)
            long len = GluGroovyLangUtils.getOptionalLong(args, "${name}Len", -1)

            InputStream is = m.stream


            if(offset < 0)
              offset = m.size + offset

            if(offset > 0)
              is.skip(offset)

            if(len > -1)
              is = new LimitedInputStream(is, len)

            return is
          }
        }

        streams[streamType.multiplexName] = new InputGeneratorStream(inputStreamFactory)
      }
    }

    // no streams were found
    if(streams.size() > 0)
    {
      // case when requesting only 1 stream
      if(numberOfStreams == 1)
      {
        // we return the only stream
        res.stream = streams.values().iterator().next()
      }
      else
      {
        // we multiplex the result
        res.stream = new MultiplexedInputStream(streams)
      }
    }

    return res
  }

  /**
   * @return an input stream for the commandId as well as its size
   */
  protected abstract def findInputStreamWithSize(String commandId, StreamType streamType)

  /**
   * Will be called back to capture the IO
   * @return whatever the closure returns
   */
  protected abstract def captureIO(CommandExecution commandExecution, Closure closure)
}