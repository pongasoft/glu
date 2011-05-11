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


package org.linkedin.glu.agent.rest.client

import org.json.JSONObject
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.rest.RestException
import org.linkedin.util.io.PathUtils
import org.linkedin.util.reflect.ReflectUtils
import org.restlet.Uniform
import org.restlet.data.MediaType
import org.restlet.data.Reference
import org.restlet.data.Status
import org.restlet.ext.json.JsonRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.ClientResource
import org.restlet.resource.ResourceException
import org.restlet.representation.EmptyRepresentation
import org.json.JSONArray

/**
 * This is the implementation of the {@link Agent} interface using a REST api under the cover
 * to talk to the real agent.
 *
 * @author ypujante@linkedin.com
 */
class AgentRestClient implements Agent
{
  public static final String MODULE = AgentRestClient.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final Uniform _client
  private final Map<String, Reference> _references

  AgentRestClient(Uniform client, Map<String, Reference> references)
  {
    _client = client
    _references = references
  }

  public void clearError(args)
  {
    handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.post(toArgs([clearError: args]))
    }
  }

  public String executeAction(args)
  {
    def response = handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.post(toArgs([executeAction: args]))
    }
    return getRes(response)
  }

  public executeCall(args)
  {
    def response = handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.post(toArgs([executeCall: args?.subMap(['call', 'callArgs'])]))
    }
    return response
  }
  
  def waitForAction(Object args)
  {
    def ref = toMountPointReference(args)

    ref.addQueryParameter('actionId', args.actionId)
    if(args.timeout)
      ref.addQueryParameter('timeout', args.timeout.toString())

    def response = handleResponse(ref) { ClientResource client ->
      client.get()
    }

    return getRes(response)
  }

  def executeActionAndWait(Object args)
  {
    def response = handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.post(toArgs([executeActionAndWait: args]))
    }
    return getRes(response)
  }

  public boolean executeActionAndWaitForState(args)
  {
    def response = handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.post(toArgs([executeActionAndWaitForState: args]))
    }
    return getRes(response) as boolean
  }

  public boolean interruptAction(Object args)
  {
    def response = handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.post(toArgs([interruptAction: args]))
    }
    return getRes(response) as boolean
  }

  public getMountPoints()
  {
    def response = handleResponse(_references.agent.targetRef) { ClientResource client ->
      client.get()
    }
    return response.mountPoints.collect { MountPoint.fromPath(it) }
  }

  public void sync()
  {
    handleResponse(_references.agent.targetRef) { ClientResource client ->
      client.put(toArgs([:]))
    }
  }

  public getHostInfo()
  {
    def response = handleResponse(_references.host.targetRef) { ClientResource client ->
      client.get()
    }
    return response
  }

  public ps()
  {
    def response = handleResponse(_references.process.targetRef) { ClientResource client ->
      client.get()
    }
    return response
  }

  public void kill(long pid, int signal)
  {
    handleResponse(toProcessReference(pid)) { ClientResource client ->
      client.put(toArgs(signal: signal))
    }
  }

  public getState(args)
  {
    return getFullState(args).scriptState.stateMachine
  }

  public getFullState(args)
  {
    def state =
      handleResponse(toMountPointReference(args)) { ClientResource client ->
        client.get()
      }.fullState

    def error = state.scriptState.stateMachine.error
    if(error instanceof Map)
    {
      state.scriptState.stateMachine.error = doRebuildAgentException(RestException.fromJSON(error))
    }

    return state
  }

  public void installScript(args)
  {
    handleResponse(toMountPointReference(args)) { ClientResource client ->
      client.put(toArgs(args))
    }
  }

  public void uninstallScript(args)
  {
    def ref = toMountPointReference(args)

    if(args.force != null)
      ref.setQuery("force=${args.force}")
    
    handleResponse(ref) { ClientResource client ->
      client.delete()
    }
  }

  public boolean waitForState(args)
  {
    def ref = toMountPointReference(args)

    ref.addQueryParameter('state', args.state)
    if(args.timeout)
      ref.addQueryParameter('timeout', args.timeout.toString())

    def response = handleResponse(ref) { ClientResource client ->
      client.get()
    }

    return getRes(response)
  }

  public InputStream tailAgentLog(args)
  {
    def ref = _references.log
    ref = addPath(ref, args.log ?: '/')

    args.subMap(['maxLine', 'maxSize']).each { k,v ->
      if(v)
      {
        ref.addQueryParameter(k, v.toString())
      }
    }

    def response = handleResponse(ref) { ClientResource client ->
      client.get()
    }

    if(response == null)
      return new ByteArrayInputStream([] as byte[])

    if(response  instanceof InputStream)
      return response
    else
      return null
  }

  def getFileContent(args)
  {
    def ref = _references.file
    ref = addPath(ref, args.location)

    args.subMap(['maxLine', 'maxSize']).each { k,v ->
      if(v)
      {
        ref.addQueryParameter(k, v.toString())
      }
    }

    def response = handleResponse(ref) { ClientResource client ->
      client.get()
    }

    if(response == null)
      return new ByteArrayInputStream([] as byte[]) 

    return getRes(response)
  }

  @Override
  int getTagsCount()
  {
    return tags.size()
  }

  @Override
  boolean hasTags()
  {
    return tags.isEmpty()
  }

  @Override
  Set<String> getTags()
  {
    handleResponse(toTagsReference([])) { ClientResource client ->
      client.get()
    } as Set
  }

  @Override
  boolean hasTag(String tag)
  {
    hasAnyTag([tag])
  }

  @Override
  boolean hasAllTags(Collection<String> tags)
  {
    Reference ref = toTagsReference(tags)
    ref.addQueryParameter('match', 'all')
    handleResponse(ref) { ClientResource client ->
      client.head()
    } == Status.SUCCESS_OK
  }

  @Override
  boolean hasAnyTag(Collection<String> tags)
  {
    handleResponse(toTagsReference(tags)) { ClientResource client ->
      client.head()
    } == Status.SUCCESS_OK
  }

  @Override
  boolean addTag(String tag)
  {
    return addTags([tag]).size() == 0
  }

  @Override
  Set<String> addTags(Collection<String> tags)
  {
    handleResponse(toTagsReference(tags)) { ClientResource client ->
      client.post(new JsonRepresentation(new JSONArray(tags)))
    } as Set
  }

  @Override
  boolean removeTag(String tag)
  {
    return removeTags([tag]).size() == 0
  }

  @Override
  Set<String> removeTags(Collection<String> tags)
  {
    handleResponse(toTagsReference(tags)) { ClientResource client ->
      client.delete()
    } as Set
  }

  @Override
  void setTags(Collection<String> tags)
  {
    handleResponse(toTagsReference(tags)) { ClientResource client ->
      client.put(new JsonRepresentation(new JSONArray(tags)))
    }
  }

  private Reference toMountPointReference(args)
  {
    String mountPoint = args.mountPoint?.toString()
    if(!mountPoint)
      throw new NoSuchMountPointException('null mount point')

    return addPath(_references.mountPoint, mountPoint)
  }

  private Reference toProcessReference(long pid)
  {
    return addPath(_references.process, pid.toString())
  }

  private Reference toTagsReference(tags)
  {
    String tagsPath

    if(tags instanceof Collection)
    {
      tagsPath = tags.join(';')
    }
    else
      tagsPath = tags.toString()

    return addPath(_references.tags, tagsPath)
  }

  private Reference addPath(Reference ref, String path)
  {
    if(path)
    {
      path = PathUtils.addPaths(ref.path, path)
      ref = new Reference(ref, path)
    }

    return ref.targetRef
  }

  private JsonRepresentation toArgs(args)
  {
    JSONObject json = new JSONObject()
    json.put('args', JsonUtils.toJSON(args))
    return new JsonRepresentation(json)
  }

  private def getRes(def response)
  {
    if(response instanceof Status)
      return null

    if(response instanceof InputStream)
      return response

    return response?.res
  }

  private def handleResponse(Reference reference, Closure closure)
  {
    def clientResource = new ClientResource(reference)
    clientResource.next = _client

    try
    {
      Representation representation = closure(clientResource)

      if(clientResource.status.isSuccess())
      {
        return extractRepresentation(clientResource, representation)
      }
      else
      {
        handleError(clientResource)
      }

    }
    catch(ResourceException e)
    {
      handleError(clientResource)
    }

    return null
  }

  private def extractRepresentation(ClientResource clientResource, Representation representation)
  {
    switch(representation?.mediaType)
    {
      case MediaType.APPLICATION_JSON:
        return JsonUtils.fromJSON(representation.text)

      case MediaType.APPLICATION_OCTET_STREAM:
        return representation.stream
    }

    return clientResource.status
  }

  private void handleError(ClientResource clientResource)
  {
    def representation = extractRepresentation(clientResource, clientResource.responseEntity)
    if(representation instanceof Status)
    {
      throw new AgentException(representation.toString())
    }
    else
    {
      throwAgentException(clientResource.status, RestException.fromJSON(representation))
    }
  }

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  private AgentException throwAgentException(Status status, RestException restException)
  {
    Throwable exception = doRebuildAgentException(restException)

    if(exception instanceof AgentException)
    {
      throw exception
    }
    else
    {
      throw new AgentException(status.toString(), restException)
    }
  }

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  private Throwable doRebuildAgentException(RestException restException)
  {
    Throwable originalException = restException
    try
    {
      def exceptionClass = ReflectUtils.forName(restException.originalClassName)
      originalException = exceptionClass.newInstance([restException.originalMessage] as Object[])

      originalException.setStackTrace(restException.stackTrace)

      if(restException.cause)
        originalException.initCause(doRebuildAgentException(restException.cause))
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled())
      {
        log.debug("Cannot instantiate: ${restException.originalClassName}... ignored", e)
      }
    }

    return originalException
  }
}
