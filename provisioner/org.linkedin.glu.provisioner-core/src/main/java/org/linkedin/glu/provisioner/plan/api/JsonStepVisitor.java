/*
 * Copyright (c) 2011 Yan Pujante
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

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author yan@pongasoft.com
 */
public class JsonStepVisitor<T> implements IStepVisitor<T>
{
  private final JSONObject _steps;

  /**
   * Constructor
   */
  public JsonStepVisitor(JSONObject steps)
  {
    _steps = steps;
  }

  @Override
  public void startVisit()
  {
  }

  @Override
  public void visitLeafStep(LeafStep<T> step)
  {
    visitStep(step, "leaf");
  }

  @Override
  public IStepVisitor<T> visitSequentialStep(SequentialStep<T> step)
  {
    return visitCompositeStep(step, "sequential");
  }

  @Override
  public IStepVisitor<T> visitParallelStep(ParallelStep<T> step)
  {
    return visitCompositeStep(step, "parallel");
  }

  private IStepVisitor<T> visitCompositeStep(CompositeStep<T> step, String type)
  {
    JSONObject jsonStep = visitStep(step, type);
    return new JsonStepVisitor<T>(jsonStep);
  }

  private JSONObject visitStep(IStep<T> step, String type)
  {
    try
    {
      JSONObject jsonStep = new JSONObject();
      jsonStep.put("type", type);
      jsonStep.put("metadata", step.getMetadata());
      _steps.accumulate("steps", jsonStep);
      return jsonStep;
    }
    catch(JSONException e)
    {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void endVisit()
  {
  }
}
