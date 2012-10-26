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
import org.linkedin.glu.agent.impl.script.CallExecution
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.agent.api.NoSuchCommandException
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.impl.concurrent.FutureTaskExecution
import org.apache.commons.io.input.TeeInputStream
import java.util.concurrent.TimeoutException
import org.linkedin.groovy.util.config.Config

/**
 * @author yan@pongasoft.com */
public class CommandManagerImpl implements CommandManager
{
  public static final String MODULE = CommandManagerImpl.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = true)
  AgentContext agentContext

  @Initializable(required = true)
  ExecutorService executorService = Executors.newCachedThreadPool()

  @Initializable(required = true)
  CommandExecutionIOStorage storage

  private final Map<String, CommandNode> _commands = [:]

  /**
   * {@inheritdoc}
   */
  @Override
  CommandNode executeShellCommand(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['command', 'redirectStderr', 'stdin'])

    args.type = 'shell'

    CommandNodeWithStorage command = storage.createStorageForCommand(args)

    def asyncProcessing = {
      command.captureIO { CommandStreamStorage storage ->
        if(args.stdin)
        {
          // in parallel, write stdin to IO storage
          def stream = storage.findStdinStorage()
          if(stream)
            args.stdin = new TeeInputStream(args.stdin, stream, true)
        }

        // handle stdout...
        def stdout = storage.findStdoutStorage()
        if(stdout)
          args.stdout = stdout

        // handle stderr...
        if(!Config.getOptionalBoolean(args, 'redirectStderr', false))
        {
          def stderr = storage.findStderrStorage()
          if(stderr)
            args.stderr = stderr
        }

        // finally run the command
        def callExecution = new CallExecution(action: 'run',
                                              actionArgs: args,
                                              clock: agentContext.clock,
                                              source: command)
        callExecution.runSync()
      }
    }

    def futureExecution = new FutureTaskExecution(asyncProcessing)
    futureExecution.clock = agentContext.clock
    futureExecution.onCompletionCallback = {
      synchronized(_commands)
      {
        _commands.remove(command.id)
      }
    }
    command.futureExecution = futureExecution

    synchronized(_commands)
    {
      _commands[command.id] = command
      try
      {
        command.runAsync(executorService)
      }
      catch(Throwable th)
      {
        // this is to avoid the case when the command is added to the map but we cannot
        // run the asynchronous execution which will remove it from the map when complete
        _commands.remove(command.id)
        throw th
      }
      command.log.info("execute(...)")
      return command
    }
  }

  /**
   * {@inheritdoc}
   */
  @Override
  def waitForCommand(def args) throws AgentException
  {
    CommandNode node = getCommand(args.id)

    try
    {
      def res = node.waitForCompletion(args.timeout)
      node.log.info("waitForCommand(${args}): ${res}")
      return res
    }
    catch(TimeoutException e)
    {
      node.log.info("waitForCommand(${args}): <timeout>")
      throw e
    }
  }

  @Override
  def findCommandNodeAndStreams(def args)
  {
    return storage.findCommandNodeAndStreams(args.id, args)
  }

  /**
   * {@inheritdoc}
   */
  @Override
  boolean interruptCommand(def args)
  {
    boolean res = false

    def node = findCommand(args.id)
    if(node)
    {
      res = node.interruptExecution()
      node.log.info("interruptCommand(${args})")
    }

    return res
  }

  CommandNode findCommand(String id)
  {
    CommandNode commandNode

    // first we look to see if the command is still currently running
    synchronized(_commands)
    {
      commandNode = _commands[id]
    }

    // not found... should be completed => look in storage
    if(!commandNode)
      commandNode = storage.findCommandNode(id)

    return commandNode
  }

  CommandNode getCommand(String id)
  {
    CommandNode command = findCommand(id)
    if(!command)
      throw new NoSuchCommandException(id)
    return command
  }
}