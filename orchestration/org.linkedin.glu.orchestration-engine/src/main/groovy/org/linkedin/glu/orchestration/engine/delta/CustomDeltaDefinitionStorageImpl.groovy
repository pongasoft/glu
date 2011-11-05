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

package org.linkedin.glu.orchestration.engine.delta

import org.linkedin.util.annotations.Initializable

/**
 * @author yan@pongasoft.com */
public class CustomDeltaDefinitionStorageImpl implements CustomDeltaDefinitionStorage
{
  @Initializable(required = true)
  CustomDeltaDefinitionSerializer customDeltaDefinitionSerializer

  @Override
  boolean save(UserCustomDeltaDefinition definition)
  {
    definition.save([flush: true])
  }

  @Override
  boolean delete(UserCustomDeltaDefinition definition)
  {
    // TODO MED YP: somehow definition.delete([flush: true]) is not working and giving the following
    // messsage: org.springframework.dao.InvalidDataAccessApiUsageException: Write operations are
    // not allowed in read-only mode (FlushMode.MANUAL): Turn your Session into
    // FlushMode.COMMIT/AUTO or remove 'readOnly' marker from transaction definition.
    
    UserCustomDeltaDefinition.executeUpdate("delete UserCustomDeltaDefinition u where u.id=?",
                                            [definition.id])
  }

  @Override
  UserCustomDeltaDefinition findByUsernameAndName(String username, String name)
  {
    UserCustomDeltaDefinition.findByUsernameAndName(username, name)
  }

  @Override
  Map findAllByUsername(String username, boolean includeDetails, Object params)
  {
    params = processParams(params)

    int count = LightUserCustomDeltaDefinition.countByUsername(username)

    Collection list

    if(includeDetails)
    {
      list = UserCustomDeltaDefinition.findAllByUsername(username, params)
    }
    else
    {
      list = LightUserCustomDeltaDefinition.findAllByUsername(username, params)
    }

    return [
      list: list,
      count: Math.max(count, list.size())
    ]
  }

  @Override
  Map findAllShareable(boolean includeDetails, Object params)
  {
    params = processParams(params)

    int count = LightUserCustomDeltaDefinition.countByShareable(true)

    Collection list

    if(includeDetails)
    {
      list = UserCustomDeltaDefinition.findAllByShareable(true, params)
    }
    else
    {
      list = LightUserCustomDeltaDefinition.findAllByShareable(true, params)
    }

    return [
      list: list,
      count: Math.max(count, list.size())
    ]
  }

  protected def processParams(params)
  {
    if(params.offset == null)
      params.offset = 0

    params.max = params.max?.toInteger() ?: Integer.MAX_VALUE
    params.sort = params.sort ?: 'name'
    params.order = params.order ?: 'asc'

    return params
  }
}