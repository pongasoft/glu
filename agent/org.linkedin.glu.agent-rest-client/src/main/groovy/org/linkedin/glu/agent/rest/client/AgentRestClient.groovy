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


package org.linkedin.glu.agent.rest.client

import org.json.JSONArray
import org.json.JSONObject
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.glu.agent.rest.common.AgentRestUtils
import org.linkedin.glu.agent.rest.common.InputStreamOutputRepresentation
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.utils.io.EmptyInputStream
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.rest.RestException
import org.linkedin.util.io.PathUtils
import org.restlet.Uniform
import org.restlet.data.MediaType
import org.restlet.data.Reference
import org.restlet.data.Status
import org.restlet.ext.json.JsonRepresentation
import org.restlet.representation.EmptyRepresentation
import org.restlet.representation.Representation
import org.restlet.resource.ClientResource
import org.restlet.resource.ResourceException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This is the implementation of the {@link Agent} interface using a REST api under the cover
 * to talk to the real agent.
 *
 * @author ypujante@linkedin.com
 */
class AgentRestClient implements Agent
{
  public static final String MODULE = AgentRestClient.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

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
      client.post(toArgs([executeCall: GluGroovyCollectionUtils.subMap(args, ['call', 'callArgs'])]))
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

  public boolean interruptAction(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['mountPoint', 'action', 'actionId'])

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
      state.scriptState.stateMachine.error = AgentRestUtils.rebuildAgentException(RestException.fromJSON(error))
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

    GluGroovyCollectionUtils.subMap(args, ['maxLine', 'maxSize']).each { k,v ->
      if(v)
      {
        ref.addQueryParameter(k.toString(), v.toString())
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

  private static def FILE_CONTENT_EXPECTED_HEADERS =
    [
      'tailStreamMaxLength' : { it as long },
      'length': { it as long },
      'lastModified': { it as long },
      'canonicalPath': { it as String },
      'isSymbolicLink': { GluGroovyLangUtils.getOptionalBoolean(it, false) },
    ]

  def getFileContent(args)
  {
    def ref = _references.file
    ref = addPath(ref, args.location)

    GluGroovyCollectionUtils.subMap(args, ['maxLine', 'maxSize', 'offset', 'includeMimeTypes']).each { k,v ->
      if(v != null)
      {
        ref.addQueryParameter(k.toString(), v.toString())
      }
    }

    def map = [:]

    def response =
      handleResponse(ref,
                     [
                       (Status.CLIENT_ERROR_NOT_FOUND): { ClientResource client ->
                         return null
                       }
                     ]) { ClientResource client ->

      Representation res = client.get()

      def headers = client.responseAttributes.'org.restlet.http.headers'

        FILE_CONTENT_EXPECTED_HEADERS.each { k, Closure dynamicCast ->
        def value = headers?.getFirstValue("X-glu-file-${k}".toString())
        if(value != null)
          map[k] = dynamicCast(value)
      }

      return res
    }

    if(args.containsKey('offset'))
    {
      if(!map)
      {
        return null
      }

      map.tailStream = getRes(response) ?: EmptyInputStream.INSTANCE

      return map
    }
    else
    {
      return getRes(response)
    }
  }

  @Override
  def executeShellCommand(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['id', 'command', 'redirectStderr', 'stdin'])

    def ref = _references.commands.targetRef

    def stdin = args.remove('stdin')
    args.type = 'shell'

    args.each { k, v ->
      if(v != null)
        ref.addQueryParameter(k.toString(), v.toString())
    }

    def response = handleResponse(ref) { ClientResource client ->
      Representation res

      if(stdin)
        res = client.post(new InputStreamOutputRepresentation(stdin))
      else
      {
        def empty = new EmptyRepresentation()
        empty.mediaType = MediaType.APPLICATION_OCTET_STREAM
        res = client.post(empty)
      }

      return res
    }

    return getRes(response)
  }


  @Override
  def waitForCommand(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['id', 'timeout'])

    def ref = _references.command.targetRef

    ref = addPath(ref, args.remove('id').toString(), "exitValue")

    args.each { k, v ->
      if(v != null)
        ref.addQueryParameter(k.toString(), v.toString())
    }

    def response = handleResponse(ref) { ClientResource client ->
      client.get()
    }

    getRes(response)
  }

  private def streamCommandResultsInputArgs = [
    'id',
    'exitErrorStream',
    'exitValueStream',
    'exitValueStreamTimeout',
    'stdinStream',
    'stdinOffset',
    'stdinLen',
    'stdoutStream',
    'stdoutOffset',
    'stdoutLen',
    'stderrStream',
    'stderrOffset',
    'stderrLen',
  ]

  @Override
  def streamCommandResults(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, streamCommandResultsInputArgs)

    def ref = _references.command.targetRef

    ref = addPath(ref, args.remove('id').toString(), "streams")

    args.each { k, v ->
      if(v != null)
        ref.addQueryParameter(k.toString(), v.toString())
    }

    def map = [:]

    def expectedHeaders = ['startTime', 'completionTime']

    def response = handleResponse(ref) { ClientResource client ->
      Representation res = client.get()

      def headers = client.responseAttributes.'org.restlet.http.headers'

      expectedHeaders.each { k ->
        def value = headers?.getFirstValue("X-glu-command-${k}".toString())
        if(value)
          map[k] = value
      }

      return res
    }

    if(response instanceof InputStream)
      map.stream = response

    return map
  }

  @Override
  boolean interruptCommand(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['id'])

    def ref = _references.command.targetRef

    ref = addPath(ref, args.remove('id').toString(), "exitValue")

    def response = handleResponse(ref) { ClientResource client ->
      client.delete()
    }

    getRes(response) as boolean
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

    // need to properly escape all weird characters
    mountPoint = mountPoint.split('/').collect { URLEncoder.encode(it, "UTF-8") }.join('/')

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

  private Reference addPath(Reference ref, String... paths)
  {
    if(paths)
    {
      String path = ref.path
      paths.each { path = PathUtils.addPaths(path, it) }
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

  private <T> T handleResponse(Reference reference, Closure closure)
  {
    handleResponse(reference, null, closure)
  }

  private <T> T handleResponse(Reference reference,
                               Map<Status, Closure> statusErrorHandlers,
                               Closure closure)
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
        handleError(clientResource, null, statusErrorHandlers)
      }
    }
    catch(ResourceException e)
    {
      handleError(clientResource, e, statusErrorHandlers)
    }

    return null
  }

  private <T> T extractRepresentation(ClientResource clientResource, Representation representation)
  {
    switch(representation?.mediaType)
    {
      case MediaType.APPLICATION_JSON:
        return (T) JsonUtils.fromJSON(representation.text)

      case MediaType.APPLICATION_OCTET_STREAM:
        return (T) representation.stream
    }

    return (T) clientResource.status
  }

  private void handleError(ClientResource clientResource,
                           Throwable throwable,
                           Map<Status, Closure> statusErrorHandlers)
  {
    def representation = extractRepresentation(clientResource, clientResource.responseEntity)
    if(representation instanceof Status)
    {
      handleRecoverableError(clientResource, representation, statusErrorHandlers)
    }
    else
    {
      if(representation instanceof InputStream)
      {
        throw new AgentException(representation.text, throwable)
      }
      else
      {
        AgentRestUtils.throwAgentException(clientResource.status,
                                           RestException.fromJSON(representation))
      }
    }
  }

  protected def handleRecoverableError(ClientResource clientResource,
                                       Status status,
                                       Map<Status, Closure> statusErrorHandlers)
  {
    if(statusErrorHandlers && statusErrorHandlers[status])
    {
      statusErrorHandlers[status](clientResource)
    }
    else
    {
      if(status.isRecoverableError())
        throw new RecoverableAgentException(status)
      else
        throw new AgentException(status.toString())
    }
  }
}
