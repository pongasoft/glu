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


package org.linkedin.glu.agent.rest.resources

import org.json.JSONObject
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.api.DuplicateMountPointException
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.glu.agent.api.ScriptIllegalStateException
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.rest.RestException
import org.restlet.Context
import org.restlet.Request
import org.restlet.Response
import org.restlet.data.Form
import org.restlet.data.MediaType
import org.restlet.data.Reference
import org.restlet.data.Status
import org.restlet.ext.json.JsonRepresentation
import org.restlet.representation.Representation
import org.restlet.representation.Variant
import org.restlet.resource.Resource

/**
 * Base class for resources to the agent
 *
 * @author ypujante@linkedin.com
 */
class BaseResource extends Resource
{
  public static final String MODULE = BaseResource.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final String _resourceMountPoint
  private final Agent _agent

  BaseResource(Context context, Request request, Response response)
  {
    super(context, request, response);
    _resourceMountPoint = context.attributes[getClass().name]
    _agent = context.attributes['agent']
    variants.add(new Variant(MediaType.APPLICATION_JSON))
  }

  def static toArgs(Representation representation)
  {
    JSONObject json = new JsonRepresentation(representation).toJsonObject()
    return JsonUtils.toValue(json.get('args'))
  }

  def static toArgs(Form form)
  {
    def args = [:]

    form?.each { p ->
      args[p.name] = p.value
    }

    return args
  }

  def getRequestArgs()
  {
    def form = request.originalRef.queryAsForm
    if(form)
    {
      return toArgs(form)
    }
    else
      return [:]
  }

  def static toRepresentation(res)
  {
    return new JsonRepresentation(JsonUtils.toJSON(res))
  }

  protected <T> T noException(Closure closure)
  {
    try
    {
      return closure()
    }
    catch(NoSuchMountPointException e)
    {
      handleException(Status.CLIENT_ERROR_NOT_FOUND, e)
    }
    catch(DuplicateMountPointException e)
    {
      handleException(Status.CLIENT_ERROR_CONFLICT, e)
    }
    catch(ScriptIllegalStateException e)
    {
      handleException(Status.CLIENT_ERROR_PRECONDITION_FAILED, e)
    }
    catch(ScriptException e)
    {
      handleException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e)
    }
    catch(AgentException e)
    {
      handleException(Status.SERVER_ERROR_INTERNAL, e)
    }
    catch(Throwable th)
    {
      log.warn('unexpected error while processing request', th)
      handleException(Status.SERVER_ERROR_INTERNAL, th)
    }
  }

  private def handleException(Status status, Throwable th)
  {
    response.setStatus(status, th)
    def entity = new JsonRepresentation(RestException.toJSON(th))
    response.setEntity(entity)
    return entity

  }

  Agent getAgent()
  {
    return _agent
  }

  String getResourceMountPoint()
  {
    return _resourceMountPoint
  }

  protected String getPath()
  {
    Reference ref = request.originalRef
    return ref.path - resourceMountPoint
  }

  protected void addResponseHeader(String name, def value)
  {
    Form form = (Form) response.attributes.'org.restlet.http.headers'
    if(form == null)
    {
      form = new Form()
      response.attributes.'org.restlet.http.headers' = form
    }
    if(value != null)
      form.add(name, value.toString())
  }
}
