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

package org.linkedin.glu.console.provisioner.services.storage

import org.linkedin.glu.orchestration.engine.deployment.DeploymentStorage
import org.linkedin.glu.orchestration.engine.deployment.ArchivedDeployment
import org.linkedin.glu.console.domain.DbDeployment
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.console.domain.LightDbDeployment
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * @author yan@pongasoft.com */
public class DeploymentStorageImpl implements DeploymentStorage
{
  public static final String MODULE = DeploymentStorageImpl.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  @Initializable
  int maxResults = 25

  @Override
  ArchivedDeployment getArchivedDeployment(String id)
  {
    createArchivedDeployment(DbDeployment.get(id as long))
  }

  @Override
  Map getArchivedDeployments(String fabric,
                             boolean includeDetails,
                             def params)
  {
    params = GluGroovyCollectionUtils.subMap(params, ['offset', 'max', 'sort', 'order'])

    if(params.offset == null)
      params.offset = 0
    params.max = Math.min(params.max ? params.max.toInteger() : maxResults, maxResults)
    params.sort = params.sort ?: 'startDate'
    params.order = params.order ?: 'desc'

    def deployments

    if(includeDetails)
      deployments = DbDeployment.findAllByFabric(fabric, params)
    else
      deployments = LightDbDeployment.findAllByFabric(fabric, params)

    [
        deployments: deployments.collect { createArchivedDeployment(it) },
        count: getArchivedDeploymentsCount(fabric),
    ]
  }

  @Override
  int getArchivedDeploymentsCount(String fabric)
  {
    DbDeployment.countByFabric(fabric)
  }

  protected ArchivedDeployment createArchivedDeployment(DbDeployment deployment)
  {
    if(deployment == null)
      return null
    
    return new ArchivedDeployment(id: deployment.id.toString(),
                                  startDate: deployment.startDate,
                                  endDate: deployment.endDate,
                                  username: deployment.username,
                                  fabric: deployment.fabric,
                                  description: deployment.description,
                                  status: deployment.status,
                                  details: deployment.details)
  }

  protected ArchivedDeployment createArchivedDeployment(LightDbDeployment deployment)
  {
    if(deployment == null)
      return null

    return new ArchivedDeployment(id: deployment.id.toString(),
                                  startDate: deployment.startDate,
                                  endDate: deployment.endDate,
                                  username: deployment.username,
                                  fabric: deployment.fabric,
                                  description: deployment.description,
                                  status: deployment.status,
                                  details: null)
  }

  @Override
  ArchivedDeployment startDeployment(String description,
                                     String fabric,
                                     String username,
                                     String details)
  {
    ArchivedDeployment archivedDeployment = null
    DbDeployment.withTransaction {
      def deployment = new DbDeployment(description: description,
                                        fabric: fabric,
                                        username: username,
                                        details: details)

      if(!deployment.save())
        throw new Exception("cannot save deployment ${description}: ${deployment.errors}")

       archivedDeployment = createArchivedDeployment(deployment)
    }

    return archivedDeployment
  }

  @Override
  ArchivedDeployment endDeployment(String id,
                                   IStepCompletionStatus status,
                                   String details)
  {
    ArchivedDeployment archivedDeployment = null

    DbDeployment.withTransaction { txStatus ->
      DbDeployment deployment = DbDeployment.get(id as long)
      if(!deployment)
      {
        log.warn("could not find deployment ${id}")
      }
      else
      {
        deployment.startDate = new Date(status.startTime)
        deployment.endDate = new Date(status.endTime)
        deployment.status = status.status.name()
        deployment.details = details

        if(!deployment.save())
        {
          log.warn("could not save deployment ${id}: ${deployment.errors}")
        }
        else
        {
          archivedDeployment = createArchivedDeployment(deployment)
        }
      }
    }

    return archivedDeployment
  }
}