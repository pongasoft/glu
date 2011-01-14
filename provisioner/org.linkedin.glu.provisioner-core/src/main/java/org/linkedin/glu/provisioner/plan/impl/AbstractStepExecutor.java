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

package org.linkedin.glu.provisioner.plan.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus;
import org.linkedin.util.clock.Timespan;
import org.linkedin.util.concurrent.ConcurrentUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * @author ypujante@linkedin.com
 */
public abstract class AbstractStepExecutor<T> implements IStepExecutor<T>
{
  public static final String MODULE = AbstractStepExecutor.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final IStep<T> _step;
  private final StepExecutionContext<T> _context;

  private Future<Void> _future;
  private volatile IStepCompletionStatus<T> _completionStatus;
  private long _startTime = 0;

  public boolean _paused = false;
  public boolean _cancelled = false;

  /**
   * Constructor
   */
  protected AbstractStepExecutor(IStep<T> step, StepExecutionContext<T> context)
  {
    _step = step;
    _context = context;
  }

  /**
   * @return the step being executed
   */
  @Override
  public IStep<T> getStep()
  {
    return _step;
  }

  protected StepExecutionContext<T> getContext()
  {
    return _context;
  }

  /**
   * @return <code>true</code> if the execution is completed (or aborted).
   */
  @Override
  public boolean isCompleted()
  {
    return _completionStatus != null;
  }

  /**
   * Attempts to cancel execution of this step.
   *
   * @param mayInterruptIfRunning true if the thread executing this step should be interrupted;
   *                              otherwise, in-progress steps are allowed to complete
   */
  @Override
  public synchronized void cancel(boolean mayInterruptIfRunning)
  {
    if(!_cancelled)
    {
      _cancelled = true;

      if(_future == null)
      {
        if(log.isDebugEnabled())
          debug("cancel (not started)");

        _startTime = _context.currentTimeMillis();
        _context.onStepStart(this);
        setCompletionStatus(doCancel(false));
      }
      else
      {
        if(_future.cancel(mayInterruptIfRunning))
        {
          // it means that the future has been cancelled
          if(log.isDebugEnabled())
            debug("cancel (started)");

          setCompletionStatus(doCancel(true));
        }
        else
        {
          if(log.isDebugEnabled())
            debug("cancel (already completed)");
        }
      }

      _context.onCancelled(getStep());
      
      notifyAll();
    }
  }

  /**
   * Executes the step
   */
  @Override
  public synchronized void execute()
  {
    if(_future == null && !_cancelled)
    {
      if(log.isDebugEnabled())
        debug("execute (submitting)");

      _future = _context.submit(new Callable<Void>()
      {
        @Override
        public Void call() throws Exception
        {
          try
          {
            if(log.isDebugEnabled())
              debug("execute (waiting for resume)");

            IStepCompletionStatus<T> completionStatus;

            try
            {
              if(waitForResume())
              {
                if(log.isDebugEnabled())
                  debug("execute (executing)");

                _startTime = _context.currentTimeMillis();
                completionStatus = doExecute();
                setCompletionStatus(completionStatus);
              }
              else
              {
                if(log.isDebugEnabled())
                  debug("execute (not executing)");
              }
            }
            catch(Throwable th)
            {
              if(log.isDebugEnabled())
                debug("exception in execute (ignored)", th);
              completionStatus = createCompletionStatus(IStepCompletionStatus.Status.FAILED, th);
              setCompletionStatus(completionStatus);
            }

            return null;
          }
          catch(Throwable th)
          {
            // this should not really happen
            log.warn("exception in execute (ignored)", th);
            return null;
          }
        }
      });

      _context.onStepStart(this);
    }
  }

  protected abstract IStepCompletionStatus<T> doExecute() throws InterruptedException;

  protected abstract IStepCompletionStatus<T> doCancel(boolean started);

  protected abstract IStepCompletionStatus<T> createCompletionStatus(IStepCompletionStatus.Status status,
                                                                     Throwable throwable);

  @Override
  public long getStartTime()
  {
    return _startTime;
  }

  /**
   * @return how long the execution took (or has been taking so far if not completed)
   */
  @Override
  public Timespan getDuration()
  {
    if(isCompleted())
      return getCompletionStatus().getDuration();
    else
      return new Timespan(_context.currentTimeMillis() - getStartTime());
  }

  /**
   * @return the completion status
   */
  @Override
  public IStepCompletionStatus<T> getCompletionStatus()
  {
    return _completionStatus;
  }

  public void setCompletionStatus(IStepCompletionStatus<T> completionStatus)
  {
    synchronized(this)
    {
      if(_completionStatus == null)
      {
        if(log.isDebugEnabled())
          debug("setCompletionStatus " + completionStatus.getStatus());

        _completionStatus = completionStatus;
        _context.onStepEnd(completionStatus);
        notifyAll();
      }
      else
      {
        if(log.isDebugEnabled())
          debug("setCompletionStatus " + completionStatus.getStatus() + " ignored: already set to " + _completionStatus.getStatus());
      }
    }
  }

  /**
   * Wait for the execution to be completed.
   *
   * @return the status
   * @throws InterruptedException if interrupted while waiting
   */
  @Override
  public IStepCompletionStatus<T> waitForCompletion() throws InterruptedException
  {
    synchronized(this)
    {
      while(_completionStatus == null)
        wait();

      return _completionStatus;
    }
  }

  /**
   * Wait for the execution to be completed.
   *
   * @return the status
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException if the timeout gets reached and the execution is not yet completed
   */
  @Override
  public IStepCompletionStatus<T> waitForCompletion(Timespan timeout)
    throws InterruptedException, TimeoutException
  {
    long endTime = timeout.futureTimeMillis(_context.getClock());

    synchronized(this)
    {
      while(_completionStatus == null)
      {
        ConcurrentUtils.awaitUntil(_context.getClock(), this, endTime);
      }

      return _completionStatus;
    }
  }

  @Override
  public synchronized void pause()
  {
    if(!_paused && !_cancelled)
    {
      _paused = true;
      _context.onPause(getStep());
    }
  }

  @Override
  public synchronized void resume()
  {
    if(_paused && !_cancelled)
    {
      _paused = false;
      notifyAll();
      _context.onResume(getStep());
    }
  }

  @Override
  public synchronized boolean isPaused()
  {
    return _paused;
  }

  @Override
  public synchronized boolean isCancelled()
  {
    return _cancelled;
  }

  /**
   * Blocking call until not in paused mode or aborted.
   *
   * @return <code>true</code> if ok to continue... <code>false</code> if aborted.
   * @throws InterruptedException
   */
  public synchronized boolean waitForResume() throws InterruptedException
  {
    while(_paused && !_cancelled)
    {
      wait();
    }

    return !_cancelled;
  }

  protected void debug(String message)
  {
    if(log.isDebugEnabled())
    {
      log.debug(getLogPrefix(getStep()) + message);
    }
  }

  protected void debug(String message, Throwable th)
  {
    if(log.isDebugEnabled())
    {
      log.debug(getLogPrefix(getStep()) + message, th);
    }
  }

  private static String getLogPrefix(IStep step)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(step.getType());
    sb.append("@");
    sb.append(step.getId());
    sb.append("/");
    sb.append(Thread.currentThread().getId());
    sb.append(": ");
    return sb.toString();
  }
}