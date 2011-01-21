/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.slf4j.Logger
import org.linkedin.glu.agent.api.FutureExecution
import org.linkedin.util.lifecycle.Startable
import org.linkedin.util.lifecycle.Shutdownable
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.Timespan
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.util.clock.ClockUtils

/**
 * @author ypujante@linkedin.com */
class ScriptExecution implements Startable, Shutdownable
{
  private final def _source
  private final String _name
  private final Logger _log

  Clock clock = SystemClock.instance()
  Timespan heartbeat = Timespan.parse('1m')
  Timespan expiryDuration = Timespan.parse('1m')
  int expiryMaxElements = 50

  /**
   * The timeline is sorted by futureExecutionTime first then queueing order
   * ({@link FutureExecutionImpl#compareTo} method)
   */
  private final def _timeline = new TreeSet<FutureExecutionImpl>()

  /**
   * Contains the same info as timeline (future executions only) but indexed by unique id
   */
  private final def _futureExecutions = [:]

  /**
   * The denormalized map of timers: key is timer name
   */
  private final def _timers = [:]

  /**
   * A map of the recent executions (up to {@link #expiryDuration} old). We want to make sure that
   * the memory does not grow indefinitely, this is why we 'remove' old executions after a while.
   * @see #removeOldExecutions()
   */
  private def _pastExecutions = [:]

  /**
   * The currently being executed execution
   */
  private FutureExecutionImpl _current

  private volatile boolean _shutdown = false
  private int _counter = 0
  private volatile Thread _thread

  ScriptExecution(def source, String name, Logger log)
  {
    _source = source
    _name = name
    _log = log
  }

  /**
   * mostly for use from test only... 
   */
  def getLock()
  {
    return _futureExecutions
  }

  def getTimeline()
  {
    synchronized(lock)
    {
      _timeline.collect { it }
    }
  }

  def getPastExecutions()
  {
    synchronized(lock)
    {
      _pastExecutions.values().collect { it }
    }
  }

  void start()
  {
    synchronized(lock)
    {
      if(!_shutdown)
      {
        _thread = Thread.start(_name, executeFutureTasks)
        if(_log.isDebugEnabled())
          _log.debug "Starting thread ${_thread}"
      }
    }
  }

  void shutdown()
  {
    synchronized(lock)
    {
      if(!_shutdown)
      {
        _shutdown = true
        lock.notifyAll()
      }
    }
  }

  void waitForShutdown()
  {
    if(!_shutdown)
      throw new IllegalStateException('call shutdown first')

    if(_log.isDebugEnabled())
      _log.debug "Waiting for thread ${_thread} to terminate..."

    _thread?.join()

    if(_log.isDebugEnabled())
      _log.debug "Thread ${_thread} terminated."
  }

  void waitForShutdown(Object timeout)
  {
    if(!_shutdown)
      throw new IllegalStateException('call shutdown first')

    timeout = ClockUtils.toTimespan(timeout)

    if(_log.isDebugEnabled())
      _log.debug "Waiting for thread ${_thread} to terminate no longer than ${timeout}..."

    if(timeout == null)
      _thread?.join()
    else
    {
      _thread?.join(timeout.durationInMilliseconds)

      // if thread is still alive, then throws exception. Thread.join does not throw one!
      if(_thread?.isAlive())
      {
        if(_log.isDebugEnabled())
          _log.debug "Thread ${_thread} is still alive."

        throw new TimeoutException()
      }
    }

    if(_log.isDebugEnabled())
      _log.debug "Thread ${_thread} terminated."
  }

   /**
    * @return currently running execution 
    */
  FutureExecution getCurrent()
  {
    synchronized(lock)
    {
      return _current
    }
  }

  /**
   * @return the timers that have been set  */
  def getTimers()
  {
    synchronized(lock)
    {
      return _timers.keySet().collect { it }
    }
  }

  /**
   * Cancel callback
   */
  private def cancel = { FutureExecutionImpl futureExecution ->
    synchronized(lock)
    {
      _timeline.remove(futureExecution)
      _futureExecutions.remove(futureExecution.id)
      if(futureExecution instanceof TimerExecution)
      {
        _timers.remove(futureExecution.timer)
      }

      if(_log.isDebugEnabled())
      {
        _log.debug("cancelled: ${futureExecution}")
      }
    }
  }

  private Closure adjustCancelCallback(Closure cancelCallback)
  {
    if(cancelCallback)
    {
      // this is to avoid nesting callbacks infinitely on timer firing...
      if(cancelCallback.owner.is(this))
      {
        return cancelCallback
      }
      else
      {
        return { FutureExecutionImpl futureExecution ->
          cancel(futureExecution)
          cancelCallback(futureExecution)
        }
      }
    }
    else
      return cancel
  }

  /**
   * Executes the action for the given script. The id can be used to to make future calls without
   * having a reference to the future itself.
   *
   * @return a future execution
   */
  FutureExecution executeAction(String action, actionArgs, Closure cancelCallback)
  {
    synchronized(lock)
    {
      // when there is no pending action, we can do a fast fail check before even enqueing the 
      // action!
      if(!findFutureAction())
      {
        // this will fail if it is not possible to reach the end state
        _source.checkValidTransitionForAction(action)
      }

      enqueueExecution(new ActionExecution(source: _source,
                                           action: action,
                                           actionArgs: actionArgs,
                                           cancelCallback: adjustCancelCallback(cancelCallback)))


    }
  }

  /**
   * Executes the call for the given script. The id can be used to to make future calls without
   * having a reference to the future itself.
   *
   * @return a future execution
   */
  FutureExecution executeCall(String call, callArgs, Closure cancelCallback)
  {
    synchronized(lock)
    {
      enqueueExecution(new CallExecution(source: _source,
                                         action: call,
                                         actionArgs: callArgs,
                                         cancelCallback: adjustCancelCallback(cancelCallback)))


    }
  }

  /**
   * Schedule a timer.
   * @param timer there can only be one timer with a given name
   * @param initialFrequency how long to wait the first time
   * @param repeatFrequency how long to wait after the first time
   * @return a future execution
   */
  FutureExecution scheduleTimer(String timer,
                                def initialFrequency,
                                def repeatFrequency,
                                Closure cancelCallback)
  {
    synchronized(lock)
    {
      repeatFrequency = Timespan.parse(repeatFrequency?.toString())
      def execution = new TimerExecution(source: _source,
                                         timer: timer,
                                         frequency: repeatFrequency,
                                         cancelCallback: adjustCancelCallback(cancelCallback))

      initialFrequency = Timespan.parse(initialFrequency?.toString()) ?: repeatFrequency

      execution.futureExecutionTime = initialFrequency.futureTimeMillis(clock)

      enqueueExecution(execution)
    }
  }

  /**
   * Cancels a timer by name
   */
  boolean cancelTimer(String timer, boolean mayInterruptIfRunning)
  {
    TimerExecution timerExecution
    synchronized(lock)
    {
      timerExecution = _timers[timer]
    }
    cancel(timerExecution?.id, mayInterruptIfRunning)
  }

  /**
   * Adds the future execution to the list
   */
  private FutureExecutionImpl enqueueExecution(FutureExecutionImpl futureExecution)
  {
    synchronized(lock)
    {
      futureExecution.queueId = ++_counter
      _futureExecutions[futureExecution.id] = futureExecution
      _timeline << futureExecution
      if(futureExecution instanceof TimerExecution)
        _timers[futureExecution.timer] = futureExecution
      lock.notifyAll()

      if(_log.isDebugEnabled())
      {
        _log.debug("enqued: ${futureExecution}")
      }

      return futureExecution
    }
  }

  /**
   * Convenient call to future using its unique id
   */
  boolean isCancelled(String id)
  {
    def future = findFuture(id)
    if(future)
    {
      return future.isCancelled()
    }
    else
      return false
  }

  /**
   * Convenient call to future using its unique id
   */
  boolean isDone(String id)
  {
    def future = findFuture(id)
    if(future)
    {
      return future.isDone()
    }
    else
      return true
  }

  /**
   * Convenient call to future using its unique id
   */
  boolean cancel(String id, boolean mayInterruptIfRunning)
  {
    def future = findFuture(id)
    if(future)
    {
      return future.cancel(mayInterruptIfRunning)
    }
    else
      return false
  }

  /**
   * Convenient call to future using its unique id
   */
  def get(String id) throws InterruptedException, ExecutionException
  {
    findFuture(id)?.get()
  }

  /**
   * Convenient call to future using its unique id
   */
  def get(String id, def timeout)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    findFuture(id)?.get(timeout)
  }

  /**
   * Convenient call to future using its unique id
   */
  def get(String id, long timeout, TimeUnit unit)
    throws InterruptedException, ExecutionException, TimeoutException
  {
    findFuture(id)?.get(timeout, unit)
  }

  /**
   * @return the future by unique id. Note that if the future has completed more than
   * {@link #expiryDuration} ago, then there is no guarantee that the future will still be
   * returned.
   */
  FutureExecution findFuture(String id)
  {
    synchronized(lock)
    {
      return allFutures.find { it.id == id}
    }
  }

  /**
   * @return the list of all futures
   */
  private def getAllFutures()
  {
    def futures = []
    synchronized(lock)
    {
      if(_current)
        futures << _current
      futures.addAll(_timeline)
      futures.addAll(_pastExecutions.values())
    }
    return futures
  }

  /**
   * By name
   */
  FutureExecution findFutureActionByName(String action)
  {
    synchronized(lock)
    {
      return allFutures.find { it instanceof ActionExecution && it.action == action }
    }
  }

  /**
   * By id (only actions)
   */
  FutureExecution findFutureActionById(String id)
  {
    synchronized(lock)
    {
      return allFutures.find { it instanceof ActionExecution && it.id == id }
    }
  }

  /**
   * @return the 'next' future action (whether it is currently running or it will run soon) that has
   * not been cancelled
   */
  private ActionExecution findFutureAction()
  {
    synchronized(lock)
    {
      return allFutures.find { it instanceof ActionExecution && !it.isCancelled() && !it.isDone()}
    }
  }

  /**
   * @return <code>0L</code> if there is no wait time because either we are in shutdown mode or
   * there is a future execution to run
   */
  private long getWaitTime()
  {
    synchronized(lock)
    {
      long res = 0L

      if(!_shutdown)
      {
        if(_timeline.isEmpty())
        {
          // nothing to do... we wait for the heartbeat
          res = heartbeat.durationInMilliseconds
        }
        else
        {
          def first = _timeline.first()
          res = first.futureExecutionTime - clock.currentTimeMillis()
          if(res < 0L)
          {
            res = 0L
          }
        }
      }

      // no matter what, we do not wait longer than the heartbeat
      res = Math.min(heartbeat.durationInMilliseconds, res)

      return res
    }
  }

  /**
   * This is the main thread method: it will loop until shutdown and run future executions
   * according to their own schedule and ordered by the order in which they have been enqueued
   * in case of schedule conflict.
   */
  private executeFutureTasks = {
    if(_log.isDebugEnabled())
    {
      _log.debug("executeFutureTasks: starting thread")
    }
    while(!_shutdown)
    {
      synchronized(lock)
      {
        def timeToWait = waitTime

        while(timeToWait > 0)
        {
          try
          {
            if(_log.isDebugEnabled())
            {
              _log.debug("executeFutureTasks: waiting for ${new Timespan(timeToWait)}")
            }
            lock.wait(timeToWait)
            removeOldExecutions()
          }
          catch(InterruptedException e)
          {
            if(_log.isDebugEnabled())
            {
              _log.debug("Ignored exception", e)
            }
          }

          timeToWait = waitTime
        }

        if(!_shutdown)
        {
          _current = _timeline.first()
          _timeline.remove(_current)
          _futureExecutions.remove(_current.id)
        }
      }

      if(_current)
      {
        _current.startTime = clock.currentTimeMillis()
        if(_log.isDebugEnabled())
        {
          _log.debug("executeFutureTasks: running ${_current}")
        }
        _current.run()
        _current.completionTime = clock.currentTimeMillis()
        if(_log.isDebugEnabled())
        {
          _log.debug("executeFutureTasks: completed ${_current}")
        }
        synchronized(lock)
        {
          _pastExecutions[_current.id] = _current
          if(_current instanceof TimerExecution)
          {
            // we make sure that it has not been cancelled
            if(_timers[_current.timer])
            {
              scheduleTimer(_current.timer,
                            null,
                            _current.frequency,
                            _current.cancelCallback)
            }
          }
          _current = null
        }
      }
    }
    if(_log.isDebugEnabled())
    {
      _log.debug("executeFutureTasks: exiting thread")
    }
  }

  private void removeOldExecutions()
  {
    synchronized(lock)
    {
      def list = _pastExecutions.values().sort { it.completionTime }

      int sizeBefore = list.size()

      long cutoff = expiryDuration.pastTimeMillis(clock)

      list = list.findAll { it.completionTime > cutoff }

      if(list.size() > expiryMaxElements)
      {
        list = list[-expiryMaxElements..-1]
      }

      int sizeAfter = list.size()

      if(sizeAfter != sizeBefore)
      {
        if(_log.isDebugEnabled())
          _log.debug "removeOldExecutions: ${sizeBefore - sizeAfter} ${sizeBefore}/${sizeAfter}"
      }

      _pastExecutions = GroovyCollectionsUtils.toMapValue(list) { it.id }
    }
  }
}