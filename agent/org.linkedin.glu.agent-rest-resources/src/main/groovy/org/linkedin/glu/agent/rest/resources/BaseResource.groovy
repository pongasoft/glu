/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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
import org.restlet.engine.header.Header
import org.restlet.resource.ResourceException
import org.restlet.data.Form
import org.restlet.data.MediaType
import org.restlet.data.Reference
import org.restlet.data.Status
import org.restlet.ext.json.JsonRepresentation
import org.restlet.representation.Representation
import org.restlet.representation.Variant
import org.linkedin.glu.utils.exceptions.DisabledFeatureException
import org.restlet.resource.ServerResource
import org.restlet.util.Series

/**
 * Base class for resources to the agent
 *
 * @author ypujante@linkedin.com
 */
class BaseResource extends ServerResource
{
  public static final String MODULE = BaseResource.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private static final String HEADERS_KEY = "org.restlet.http.headers"

  private String _resourceMountPoint
  private Agent _agent

  @Override
  protected void doInit() throws ResourceException
  {
    _resourceMountPoint = context.attributes[getClass().name]
    _agent = context.attributes['agent']
    variants.add(new Variant(MediaType.APPLICATION_JSON))
  }

  def static toArgs(Representation representation)
  {
    JSONObject json = new JsonRepresentation(representation).getJsonObject()
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

  protected Representation noException(Closure closure)
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
    catch(DisabledFeatureException e)
    {
      handleException(Status.SERVER_ERROR_NOT_IMPLEMENTED, e)
    }
    catch(Throwable th)
    {
      log.warn('unexpected error while processing request', th)
      handleException(Status.SERVER_ERROR_INTERNAL, th)
    }
  }

  private Representation handleException(Status status, Throwable th)
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
    return ref.getPath(true) - resourceMountPoint
  }

  protected Series<Header> getResponseHeader()
  {
    def attributes = response.attributes;
    Series<Header> headers = (Series<Header>) attributes.get(HEADERS_KEY);
    if(headers == null)
    {
      headers = new Series<Header>(Header.class)
      Series<Header> prev = (Series<Header>) attributes.putIfAbsent(HEADERS_KEY, headers)
      if(prev != null)
        headers = prev
    }
    return headers
  }

  protected void addResponseHeader(String name, def value)
  {
    responseHeader.add(name, value.toString())
  }
}
