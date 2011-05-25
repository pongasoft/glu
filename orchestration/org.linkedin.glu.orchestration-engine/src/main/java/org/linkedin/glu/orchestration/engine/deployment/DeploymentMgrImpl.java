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

package org.linkedin.glu.orchestration.engine.deployment;

import org.linkedin.glu.orchestration.engine.delta.DeltaMgr;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.util.annotations.Initializer;

import java.util.Collection;
import java.util.List;

/**
 * @author yan@pongasoft.com
 */
public class DeploymentMgrImpl implements DeploymentMgr
{
  private DeltaMgr _deltaMgr;

  /**
   * Constructor
   */
  public DeploymentMgrImpl()
  {
  }

  @Override
  public Plan computeDeploymentPlan(SystemModel expectedModel, SystemModel currentModel)
  {
    SystemModelDelta systemModelDelta = _deltaMgr.computeDelta(expectedModel, currentModel);

    return null;
  }

  public DeltaMgr getDeltaMgr()
  {
    return _deltaMgr;
  }

  @Initializer(required = true)
  public void setDeltaMgr(DeltaMgr deltaMgr)
  {
    _deltaMgr = deltaMgr;
  }
}
