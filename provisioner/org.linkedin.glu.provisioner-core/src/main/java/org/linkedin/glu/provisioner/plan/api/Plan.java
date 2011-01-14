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

package org.linkedin.glu.provisioner.plan.api;

import org.linkedin.util.xml.XMLIndent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * A plan is represented by the step to execute and the dependency graph (between leaves).
 *
 * @author ypujante@linkedin.com
 */
public class Plan<T>
{
  private final Map<String, Object> _metadata = new LinkedHashMap<String, Object>();
  private final IStep<T> _step;

  // cache
  private volatile Collection<LeafStep<T>> _leafSteps = null;

  /**
   * Constructor
   */
  public Plan(Map<String, Object> metadata, IStep<T> step)
  {
    if(metadata == null && step != null)
      metadata = step.getMetadata();
    if(metadata != null)
      _metadata.putAll(metadata);
    _step = step;
  }

  /**
   * Builds an empty plan
   */
  public Plan()
  {
    this(null, null);
  }

  /**
   * Constructor
   */
  public Plan(IStep<T> step)
  {
    this(step == null ? null : step.getMetadata(), step);
  }

  public void setMetadata(String name, Object value)
  {
    _metadata.put(name, value);
  }

  public Map<String, Object> getMetadata()
  {
    return _metadata;
  }

  public void setMetadata(Map<String, Object> metadata)
  {
    if(metadata != null)
    {
      _metadata.putAll(metadata);
    }
  }

  public String getName()
  {
    return (String) _metadata.get("name");
  }

  /**
   * Change the name of a plan
   */
  public void setName(String name)
  {
    _metadata.put("name", name);
  }

  public String getId()
  {
    if(_step == null)
      return Integer.toHexString(System.identityHashCode(this));
    else
      return getStep().getId();
  }

  public IStep<T> getStep()
  {
    return _step;
  }

  /**
   * @return all the leaf steps of this plan.
   */
  public Collection<LeafStep<T>> getLeafSteps()
  {
    // implementation note: not synchronized on purpose. The variable is volatile so if 2 threads
    // happens to call this at the same time, they may compute the result twice but since the
    // object is immutable it is not an issue.
    if(_leafSteps == null)
    {
      _leafSteps = doGetLeafSteps();
    }
    return _leafSteps;
  }

  /**
   * @return all the leaf steps of this plan.
   */
  private Collection<LeafStep<T>> doGetLeafSteps()
  {
    final Collection<LeafStep<T>> leaves = new ArrayList<LeafStep<T>>();

    if(_step != null)
    {
      _step.acceptVisitor(new IStepVisitor<T>()
      {
        @Override
        public void startVisit()
        {
        }

        @Override
        public void visitLeafStep(LeafStep<T> tLeafStep)
        {
          leaves.add(tLeafStep);
        }

        @Override
        public IStepVisitor<T> visitSequentialStep(SequentialStep<T> tSequentialStep)
        {
          return this;
        }

        @Override
        public IStepVisitor<T> visitParallelStep(ParallelStep<T> parallelStep)
        {
          return this;
        }

        @Override
        public void endVisit()
        {
        }
      });
    }

    return leaves;
  }


  /**
   * @return <code>true</code> if this plan has any leaf step
   */
  public boolean hasLeafSteps()
  {
    return !getLeafSteps().isEmpty();
  }

  /**
   * @return the number of leaf steps
   */
  public int getLeafStepsCount()
  {
    return getLeafSteps().size();
  }

  /**
   * @return a plan builder representing this plan (usually used to do minor modifications to the
   *         plan, like adding some steps or removing some).
   */
  public IPlanBuilder<T> toPlanBuilder()
  {
    return toPlanBuilder(StepFilters.<T>acceptAll());
  }

  /**
   * Same as {@link #toPlanBuilder()} except it also filters out unwanted steps.
   *
   * @param filter the filter to filter out unwanted steps
   * @return a new plan builder
   */
  public IPlanBuilder<T> toPlanBuilder(IStepFilter<T> filter)
  {
    PlanBuilder<T> builder = new PlanBuilder<T>();
    builder.setMetadata(_metadata);

    if(_step != null)
      _step.acceptVisitor(new CompositeStepBuilderVisitor<T>(builder, filter));

    return builder;
  }

  /**
   * @return an empty plan builder based of the dependencies in this plan.
   */
  public IPlanBuilder<T> createEmptyPlanBuilder()
  {
    return new PlanBuilder<T>();
  }

  /**
   * @return an xml representation of the plan
   */
  public String toXml()
  {
    return toXml(null);
  }

  /**
   * @return an xml representation of the plan
   */
  public String toXml(Map<String, Object> context)
  {
    XmlStepVisitor<T> visitor = new XmlStepVisitor<T>();

    XMLIndent xml = visitor.getXml();

    xml.addXMLDecl("1.0");

    Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    attributes.putAll(_metadata);

    if(context != null)
    {
      attributes.putAll(context);
    }

    if(_step != null)
    {
      xml.addOpeningTag("plan", attributes);
      _step.acceptVisitor(visitor);
      xml.addClosingTag("plan");
    }
    else
    {
      xml.addEmptyTag("plan", attributes);
    }

    return xml.getXML();
  }

  @Override
  public String toString()
  {
    return toXml();
  }
}
