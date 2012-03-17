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

package org.linkedin.glu.provisioner.core.state

import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.groovy.util.state.StateMachineImpl
import org.linkedin.util.reflect.ReflectUtils

/**
 * This class encapsulates the global state machine. By default it
 * matches the 'standard' state machine as defined by the agent but there is a way to override
 * the default when necessary.
 *
 * @author yan@pongasoft.com */
public class DefaultStateMachine
{
  public static final String MODULE = "org.linkedin.glu.provisioner.core.state.DefaultStateMachine";
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  /**
   * Default transitions when none provide
   */
  def static DEFAULT_TRANSITIONS =
  [
    NONE: [[to: 'installed', action: 'install']],
    installed: [[to: 'stopped', action: 'configure'], [to: 'NONE', action: 'uninstall']],
    stopped: [[to: 'running', action: 'start'], [to: 'installed', action: 'unconfigure']],
    running: [[to: 'stopped', action: 'stop']]
  ]

  public static StateMachine INSTANCE
  public static String DEFAULT_ENTRY_STATE

  // let the code initialize itself using various techniques. If no technique work then use default
  // values
  static {
    def definition = [:]

    // we try to look for a groovy string to evaluate that would contain variables called
    // defaultTransitions and defaultEntryState

    // 1. we look for a system property for the groovy string
    def defaultStateMachine = System.getProperty(DefaultStateMachine.class.name)

    if(defaultStateMachine)
      definition = extractDefinition(defaultStateMachine)

    // 2. we look for a file in the classpath
    if(!definition)
    {
      def stream =
        ReflectUtils.defaultClassLoader.getResourceAsStream('glu/DefaultStateMachine.groovy')
      definition = extractDefinition(stream?.text)
    }

    if(!definition.defaultTransitions)
      definition.defaultTransitions = DEFAULT_TRANSITIONS
    else
      log.info("Changing state machine to ${definition.defaultTransitions}")


    if(!definition.defaultEntryState)
      definition.defaultEntryState = "running"
    else
      log.info("Changing default entry state to ${definition.defaultEntryState}")

    INSTANCE = new StateMachineImpl(transitions: definition.defaultTransitions)
    DEFAULT_ENTRY_STATE = definition.defaultEntryState
  }

  private static Map extractDefinition(String text)
  {
    if(text)
    {
      Binding binding = new Binding()

      binding.defaultTransitions = null
      binding.defaultEntryState = null
      
      GroovyShell shell = new GroovyShell(binding)
      shell.evaluate(text)

      [
        defaultTransitions: binding.defaultTransitions,
        defaultEntryState: binding.defaultEntryState
      ]
    }
    else
    {
      [:]
    }
  }
}