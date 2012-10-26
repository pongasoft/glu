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

package org.linkedin.glu.agent.impl.command

import org.linkedin.glu.agent.impl.script.AgentContext
import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.utils.io.LimitedInputStream
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.annotations.Initializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeoutException

/**
 * @author yan@pongasoft.com */
public abstract class AbstractCommandExecutionIOStorage implements CommandExecutionIOStorage
{
  public static final String MODULE = AbstractCommandExecutionIOStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = true)
  AgentContext agentContext

  @Override
  CommandNodeWithStorage createStorageForCommand(def args)
  {
    String commandId =
      "${Long.toHexString(agentContext.clock.currentTimeMillis())}-${UUID.randomUUID().toString()}"

    CommandNodeWithStorage command = new CommandNodeWithStorage(createGluCommand(args.type, commandId),
                                                                commandId,
                                                                this)

    return saveCommandNode(command)
  }

  /**
   * {@inheritdoc}
   */
  @Override
  def findCommandNodeAndStreams(String commandId, def args)
  {
    int numberOfStreams = 0

    CommandNode node = findCommandNode(commandId)

    if(node == null)
      return null

    def res = [:]

    res.commandNode = node

    Map<String, InputStream> streams = [:]

    def exitValueStream = Config.getOptionalBoolean(args, 'exitValueStream', false)

    // Factory to compute the exit value
    def exitValueFactory = { null }

    if(exitValueStream)
      numberOfStreams++

    def timeout = args.exitValueStreamTimeout

    if(exitValueStream && (timeout != null || node.isCompleted()))
    {
      exitValueFactory = {
        try
        {
          return node.getExitValue(timeout).toString()
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
            long offset = getOptionalLong(args, "${name}Offset", 0)
            long len = getOptionalLong(args, "${name}Len", -1)

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
   * Should save the command in persistent state
   * @return the command (eventually tweaked)
   */
  protected abstract CommandNodeWithStorage saveCommandNode(CommandNodeWithStorage commandNode)

  /**
   * Create the correct glu command
   */
  private def createGluCommand(String type, String id)
  {
    if(type != "shell")
      throw new UnsupportedOperationException("cannot create non shell commands")

    def shellCommand = new ShellGluCommand()

    def commandProperties = [:]

    def log = LoggerFactory.getLogger("org.linkedin.glu.agent.command.${id}")

    commandProperties.putAll(
    [
            getId: { id },
            getShell: { agentContext.shellForCommands },
            getLog: { log },
            getSelf: { findCommandNode(id) },
    ])

    return agentContext.mop.wrapScript(script: shellCommand,
                                       scriptProperties: commandProperties)
  }

  static long getOptionalLong(config, String name, long defaultValue)
  {
    def value = config?."${name}"

    if(value == null)
      return defaultValue

    return value as long
  }
}