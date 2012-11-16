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

import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.glu.commands.impl.CommandStreamStorage
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.glu.groovy.utils.concurrent.CallExecution
import org.linkedin.glu.commands.impl.CommandExecutionIOStorage
import org.linkedin.util.annotations.Initializable

/**
 * The glu script that wraps the execution of a glu command
 *
 * @author yan@pongasoft.com */
public class CommandGluScript
{
  @Initializable(required = true)
  String commandId

  @Initializable(required = true)
  transient CommandExecutionIOStorage ioStorage

  // will be set
  transient volatile CommandExecution commandExecution

  def install = { args ->
    commandExecution = args.commandExecution
  }

  def uninstall = {
    commandExecution = null
  }

  /**
   * Used in case
   */
  private CommandExecution findCommandExecution()
  {
    if(!commandExecution)
      commandId = ioStorage.findCommandExecution(commandId)
    commandExecution
  }

  /**
   * Execute the command execution
   */
  def start = { args ->

    CommandExecution commandExecution = findCommandExecution()

    commandExecution.syncCaptureIO { CommandStreamStorage storage ->

      def actionArgs = [*:commandExecution.args]

      // handle stdin...
      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->
        if(stdin)
          actionArgs.stdin = stdin

        // handle stdout...
        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->
          if(stdout)
            actionArgs.stdout = stdout

          // handle stderr
          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr ->
            if(stderr)
              actionArgs.stderr = stderr

            // finally run the commandExecution
            def callExecution = new CallExecution(action: 'run',
                                                  actionArgs: actionArgs,
                                                  clock: shell.clock,
                                                  source: [invocable: commandExecution.command])

            return [exitValue: callExecution.runSync()]
          }
        }
      }
    }
  }
}