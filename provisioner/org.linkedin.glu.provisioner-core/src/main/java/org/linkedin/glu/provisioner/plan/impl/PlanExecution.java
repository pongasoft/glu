/*
 * Copyright 2010-2010 LinkedIn, Inc
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

import org.linkedin.glu.provisioner.plan.api.IPlanExecution;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.IStepExecution;
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.XmlStepCompletionStatusVisitor;
import org.linkedin.util.clock.Timespan;
import org.linkedin.util.xml.XMLIndent;

import java.util.concurrent.TimeoutException;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * @author ypujante@linkedin.com
 */
public class PlanExecution<T> implements IPlanExecution<T>
{
  private final Plan<T> _plan;
  private final IStepExecution<T> _stepExecution;

  /**
   * Constructor
   */
  public PlanExecution(Plan<T> plan, IStepExecution<T> stepExecution)
  {
    _plan = plan;
    _stepExecution = stepExecution;
  }

  /**
   * @return the plan being executed
   */
  @Override
  public Plan<T> getPlan()
  {
    return _plan;
  }

  @Override
  public void pause()
  {
    _stepExecution.pause();
  }

  @Override
  public void resume()
  {
    _stepExecution.resume();
  }

  @Override
  public void cancel(boolean mayInterruptIfRunning)
  {
    _stepExecution.cancel(mayInterruptIfRunning);
  }

  @Override
  public IStepCompletionStatus<T> getCompletionStatus()
  {
    return _stepExecution.getCompletionStatus();
  }

  @Override
  public long getStartTime()
  {
    return _stepExecution.getStartTime();
  }

  /**
   * @return how long the execution took (or has been taking so far if not completed)
   */
  @Override
  public Timespan getDuration()
  {
    return _stepExecution.getDuration();
  }

  @Override
  public IStep<T> getStep()
  {
    return _stepExecution.getStep();
  }

  @Override
  public boolean isCancelled()
  {
    return _stepExecution.isCancelled();
  }

  @Override
  public boolean isCompleted()
  {
    return _stepExecution.isCompleted();
  }

  @Override
  public boolean isPaused()
  {
    return _stepExecution.isPaused();
  }

  @Override
  public IStepCompletionStatus<T> waitForCompletion()
    throws InterruptedException
  {
    return _stepExecution.waitForCompletion();
  }

  @Override
  public IStepCompletionStatus<T> waitForCompletion(Timespan timeout)
    throws InterruptedException, TimeoutException
  {
    return _stepExecution.waitForCompletion(timeout);
  }

  @Override
  public String toXml()
  {
    return toXml(null);
  }

  @Override
  public String toXml(Map<String, Object> context)
  {
    if(_stepExecution.isCompleted())
    {
      XmlStepCompletionStatusVisitor<T> visitor = new XmlStepCompletionStatusVisitor<T>();

      XMLIndent xml = visitor.getXml();

      xml.addXMLDecl("1.0");

      Map<String, Object> attributes = new LinkedHashMap<String, Object>();
      attributes.putAll(_plan.getMetadata());
      if(context != null)
      {
        attributes.putAll(context);
      }

      xml.addOpeningTag("plan", attributes);
      _stepExecution.getCompletionStatus().acceptVisitor(visitor);
      xml.addClosingTag("plan");

      return xml.getXML();
    }
    else
    {
      return _plan.toXml();
    }
  }

  @Override
  public String toString()
  {
    return toXml();
  }
}
