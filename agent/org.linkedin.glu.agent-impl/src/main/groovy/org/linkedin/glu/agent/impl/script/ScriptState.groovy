/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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


package org.linkedin.glu.agent.impl.script

import java.lang.reflect.Modifier
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.groovy.util.state.StateChangeListener
import org.linkedin.util.lang.LangUtils
import java.lang.reflect.Field

/**
 * Contains the state of the script (state machine + script itself)
 *
 * @author ypujante@linkedin.com
 */
class ScriptState
{
  public static final String MODULE = ScriptState.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  public static final SCRIPT_STATE_NAMES = ['script', 'stateMachine', 'timers']

  final ScriptDefinition scriptDefinition
  final StateMachine stateMachine
  final def script

  StateChangeListener stateChangeListener

  // part of the state which changes as things evolve: note that the map it holds is never updated
  // so it is safe to pass it around. A new one is created every time the state changes
  def volatile _scriptState = [:]

  final private Object _lock = new Object()

  def ScriptState(ScriptDefinition scriptDefinition,
                  StateMachine stateMachine,
                  script)
  {
    this.scriptDefinition = scriptDefinition
    this.stateMachine = stateMachine;

    script.metaClass.setProperty = { String name, newValue ->
      def scriptDelegate = delegate
      def metaProperty = scriptDelegate.metaClass.getMetaProperty(name)
      changeState {
        if(metaProperty)
        {
          def newState = [*:(_scriptState.script ?: [:])]

          metaProperty.setProperty(scriptDelegate, newValue)

          if (isPartOfScriptPermanentState(metaProperty))
          {
            newState[name] = newValue
          }
          else
          {
            newState.remove(name)
          }

          _scriptState = createScriptState(script: newState)

        }
        else
          throw new MissingPropertyException(name, script.class)
      }
    }

    this.script = script;
    stateMachine.stateChangeListener = stateMachineStateChangeListener as StateChangeListener
    
    _scriptState = collectScriptState()
  }

  void addTimer(args)
  {
    changeState {
      // we copy the list of timers
      def timersList = (_scriptState.timers ?: []).collect { it }

      // and we add the timer to it
      timersList << args

      _scriptState = createScriptState(timers: timersList)
    }
  }

  private def createScriptState(args)
  {
    def scriptState = [:]

    SCRIPT_STATE_NAMES.each { k ->
      def value = args[k]
      if(value == null)
        value = _scriptState[k]
      scriptState[k] = value
    }

    if(!scriptState.timers)
      scriptState.remove('timers')

    return scriptState
  }

  void removeTimer(String timer)
  {
    changeState {
      // we copy the list of timers
      def timersList = (_scriptState.timers ?: []).collect { it }

      // we remove the give timer from it
      timersList.findAll { it.timer == timer }.each { timersList.remove(it) }

      _scriptState = createScriptState(timers: timersList)
    }
  }

  void setStateChangeListener(listener)
  {
    synchronized(_lock)
    {
      if(listener == null)
        stateChangeListener = null
      else
      {
        listener = listener as StateChangeListener
        stateChangeListener = { oldState, newState ->
          if(log.isDebugEnabled())
            log.debug("stateChanged: ${oldState} => ${newState}")
          try
          {
            listener.onStateChange(oldState, newState)
          }
          catch(Throwable th)
          {
            log.warn("Unexpected exception in listener (ignored)", th)
          }
        } as StateChangeListener
        stateChangeListener.onStateChange(null, cloneState(internalFullState))
      }
    }
  }

  def changeState(Closure closure)
  {
    synchronized(_lock)
    {
      if(stateChangeListener)
      {
        def oldState = internalFullState
        try
        {
          closure()
        }
        finally
        {
          def newState = internalFullState
          if(oldState != newState)
          {
            oldState = cloneState(oldState)
            newState = cloneState(newState)
            stateChangeListener?.onStateChange(oldState, newState)
          }
        }
      }
      else
        closure()
    }
  }

  def getExternalFullState()
  {
    def state = internalFullState
    state.scriptDefinition = state.scriptDefinition.toExternalRepresentation()
    return cloneState(state)
  }

  private def cloneState(state)
  {
    return LangUtils.deepClone(state)
  }

  private def getInternalFullState()
  {
    [
        scriptDefinition: scriptDefinition,
        scriptState: _scriptState
    ]
  }

    /**
     * Determine whether a property is part of the script permanent state.
     * @param property the property under evaluation
     * @return true if part of the permanent state, false otherwise
     */
  private def isPartOfScriptPermanentState(MetaProperty property)
  {
      /*
        Here the field must be tested for transient and static modifiers
        as Groovy does not support the transient modifier for properties.
       */
      def field = script.metaClass.javaClass.getDeclaredField(property.name)
      if (!Modifier.isStatic(field.modifiers) && !Modifier.isTransient(field.modifiers))
      {
        /*
          The property value must be retrieved as private fields cannot be
          accessed in this way in the Java world.
         */
        def value = property.getProperty(script)
        if(value instanceof Serializable && !(value instanceof Closure))
        {
            return true
        }
      }
      return false
  }

  private def collectScriptPermanentState()
  {
    def state = [:]

    script.metaClass.properties { property ->
      if(isPartOfScriptPermanentState(property))
      {
          state[property.name] = property.getProperty(script)
      }
    }

    return state
  }

  private def collectStateMachinePermanentState()
  {
    return stateMachine.state
  }

  private def collectScriptState()
  {
    createScriptState(script: collectScriptPermanentState(),
                      stateMachine: collectStateMachinePermanentState())
  }

  def restore(state)
  {
    restorePermanentState(state.scriptState)
  }

  def restorePermanentState(permanentState)
  {
    restoreStateMachinePermanentState(permanentState.stateMachine)
    restoreScriptPermanentState(permanentState.script)
  }

  private def restoreScriptPermanentState(scriptState)
  {
    // restore the attributes of the script
    scriptState?.each { k,v ->
      script."${k}" = v
    }
  }

  private def restoreStateMachinePermanentState(stateMachineState)
  {
    stateMachine.currentState = stateMachineState.currentState
    stateMachine.error = stateMachineState.error
  }

  private stateMachineStateChangeListener = { oldState, newState ->
    changeState {
      _scriptState = createScriptState(stateMachine: newState)
    }
  }
}
