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

package org.linkedin.glu.provisioner.plan.api;

/**
 * @author ypujante@linkedin.com
 */
public class CompositeStepBuilderVisitor<T> implements IStepVisitor<T>
{
  private final ICompositeStepBuilder<T> _builder;
  private final IStepFilter<T> _filter;

  /**
   * Constructor
   */
  public CompositeStepBuilderVisitor(ICompositeStepBuilder<T> builder, IStepFilter<T> filter)
  {
    _builder = builder;
    _filter = filter;
  }

  /**
   * Called on visit start
   */
  @Override
  public void startVisit()
  {
  }

  /**
   * Visits a leaf step.
   */
  @Override
  public void visitLeafStep(LeafStep<T> tLeafStep)
  {
    if(_filter.accept(tLeafStep))
      _builder.addLeafStep(tLeafStep);
  }

  /**
   * Visit a sequential step.
   *
   * @return <code>null</code> if you want to stop the recursion, otherwise another visitor
   */
  @Override
  public IStepVisitor<T> visitParallelStep(ParallelStep<T> parallelStep)
  {
    return visitCompositeStep(parallelStep);
  }

  /**
   * Visit a sequential step.
   *
   * @return <code>null</code> if you want to stop the recursion, otherwise another visitor
   */
  @Override
  public IStepVisitor<T> visitSequentialStep(SequentialStep<T> tSequentialStep)
  {
    return visitCompositeStep(tSequentialStep);
  }

  /**
   * common code
   */
  private IStepVisitor<T> visitCompositeStep(CompositeStep<T> step)
  {
    if(_filter.accept(step))
    {
      ICompositeStepBuilder<T> builder = _builder.addCompositeSteps(step.getType());

      builder.setMetadata(step.getMetadata());
      builder.setId(step.getId());

      return new CompositeStepBuilderVisitor<T>(builder, _filter);
    }
    else
      return null;
  }

  public ICompositeStepBuilder<T> getBuilder()
  {
    return _builder;
  }

  public IStepFilter<T> getFilter()
  {
    return _filter;
  }

  /**
   * Called on visit end
   */
  @Override
  public void endVisit()
  {
  }
}
