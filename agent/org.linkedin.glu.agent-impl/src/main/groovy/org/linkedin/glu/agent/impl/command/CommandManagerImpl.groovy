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
import org.linkedin.glu.groovy.utils.concurrent.CallExecution
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.agent.api.NoSuchCommandException
import org.linkedin.glu.agent.api.AgentException
import java.util.concurrent.TimeoutException
import org.linkedin.glu.commands.impl.CommandStreamStorage
import org.linkedin.glu.commands.impl.CommandExecutionIOStorage
import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.glu.commands.impl.GluCommandFactory
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.utils.concurrent.Submitter
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution

/**
 * @author yan@pongasoft.com */
public class CommandManagerImpl implements CommandManager
{
  public static final String MODULE = CommandManagerImpl.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = true)
  AgentContext agentContext

  @Initializable(required = true)
  Submitter submitter = FutureTaskExecution.DEFAULT_SUBMITTER

  @Initializable(required = true)
  CommandExecutionIOStorage storage

  @Initializable(required = true)
  Timespan interruptCommandGracePeriod = Timespan.parse("1s")

  private final Map<String, CommandExecution> _commands = [:]

  void setStorage(CommandExecutionIOStorage storage)
  {
    storage.gluCommandFactory = createGluCommand as GluCommandFactory
    this.storage = storage
  }

  /**
   * {@inheritdoc}
   */
  @Override
  CommandExecution executeShellCommand(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['id', 'command', 'redirectStderr', 'stdin'])

    args.type = 'shell'

    CommandExecution command = storage.createStorageForCommandExecution(args)

    def asyncProcessing = { CommandStreamStorage storage ->

      def actionArgs = [*:command.args]

      // handle stdin...
      storage.withOrWithoutStorageInput(StreamType.STDIN) { stdin ->
        if(stdin)
          actionArgs.stdin = stdin

        // handle stdout...
        storage.withOrWithoutStorageOutput(StreamType.STDOUT) { stdout ->
          if(stdout)
            actionArgs.stdout = stdout

          // handle stderr
          storage.withOrWithoutStorageOutput(StreamType.STDERR) { stderr ->
            if(stderr)
              actionArgs.stderr = stderr

            // finally run the command
            def callExecution = new CallExecution(action: 'run',
                                                  actionArgs: actionArgs,
                                                  clock: agentContext.clock,
                                                  source: [invocable: command.command])

            return [exitValue: callExecution.runSync()]
          }
        }
      }
    }

    // what to do when the command ends
    def endExecution = {
      synchronized(_commands)
      {
        _commands.remove(command.id)
      }
    }

    synchronized(_commands)
    {
      _commands[command.id] = command
      try
      {
        def future = command.asyncCaptureIO(submitter, asyncProcessing)
        future.onCompletionCallback = endExecution
      }
      catch(Throwable th)
      {
        // this is to avoid the case when the command is added to the map but we cannot
        // run the asynchronous execution which will remove it from the map when complete
        endExecution()
        throw th
      }
      command.log.info("execute(${GluGroovyCollectionUtils.xorMap(args, ['stdin'])}${args.stdin ? ', stdin:<...>': ''})")
      return command
    }
  }

  /**
   * {@inheritdoc}
   */
  @Override
  def waitForCommand(def args) throws AgentException
  {
    CommandExecution commandExecution = getCommand(args.id)

    try
    {
      def res = commandExecution.getExitValue(args.timeout)
      commandExecution.log.info("waitForCommand(${GluGroovyCollectionUtils.xorMap(args, ['id'])}): ${res}")
      return res
    }
    catch(TimeoutException e)
    {
      commandExecution.log.info("waitForCommand(${GluGroovyCollectionUtils.xorMap(args, ['id'])}): <timeout>")
      throw e
    }
  }

  @Override
  def findCommandExecutionAndStreams(def args)
  {
    CommandExecution commandExecution = findCommand(args.id)
    println "${args.id} => ${commandExecution}"
    if(commandExecution)
    {
      commandExecution.log.info("findCommandExecutionAndStreams(${GluGroovyCollectionUtils.xorMap(args, ['id'])})")
      return [commandExecution: commandExecution, stream: commandExecution.storage.findStorageInput(args)]
    }
    else
    {
      log.info("findCommandExecutionAndStreams(${args})): not found")
      return null
    }
  }

  /**
   * {@inheritdoc}
   */
  @Override
  boolean interruptCommand(def args)
  {
    boolean res = false

    def commandExecution = findCommand(args.id)
    if(commandExecution)
    {
      res = !commandExecution.isCompleted()

      if(res)
      {
        // first we "destroy" the command itself which should end the subprocess
        commandExecution.command.destroy()

        // second, we give a bit of time for the operation to complete
        res = commandExecution.waitForCompletionNoException(interruptCommandGracePeriod)

        if(!res)
        {
          log.warn("Command did not terminate after ${interruptCommandGracePeriod} grace period")

          // then we interrupt the execution (no effect if completed...)
          commandExecution.interruptExecution()

          // lastly we wait again, we give a bit of time for the operation to complete
          res = commandExecution.waitForCompletionNoException(interruptCommandGracePeriod)
        }
      }

      commandExecution.log.info("interruptCommand(${GluGroovyCollectionUtils.xorMap(args, ['id'])}): ${res}")
    }
    else
    {
      log.info("interruptCommand(${args})): not found")
    }

    return res
  }

  CommandExecution findCommand(String id)
  {
    CommandExecution commandExecution

    // first we look to see if the command is still currently running
    synchronized(_commands)
    {
      commandExecution = _commands[id]
    }

    // not found... should be completed => look in storage
    if(!commandExecution)
      commandExecution = storage.findCommandExecution(id)

    return commandExecution
  }

  CommandExecution getCommand(String id)
  {
    CommandExecution command = findCommand(id)
    if(!command)
      throw new NoSuchCommandException(id)
    return command
  }
  
  /**
   * Create the correct glu command
   */
  def createGluCommand = { CommandExecution command ->

    final String commandId = command.id

    if(command.args.type != "shell")
      throw new UnsupportedOperationException("cannot create non shell commands")

    def shellCommand = new ShellGluCommand()

    def commandProperties = [:]

    def log = LoggerFactory.getLogger("org.linkedin.glu.agent.command.${commandId}")

    commandProperties.putAll(
    [
            getId: { commandId },
            getShell: { agentContext.shellForCommands },
            getLog: { log },
            getSelf: { findCommand(commandId).command },
    ])

    return agentContext.mop.wrapScript(script: shellCommand,
                                       scriptProperties: commandProperties)
  }
  
}