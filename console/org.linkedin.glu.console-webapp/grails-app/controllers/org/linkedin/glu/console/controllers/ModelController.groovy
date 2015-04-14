/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2015 Yan Pujante
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

package org.linkedin.glu.console.controllers

import com.fasterxml.jackson.core.JsonParseException
import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.console.provisioner.services.storage.SystemStorageException
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.grails.utils.ConsoleHelper
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemModelRenderer
import org.linkedin.glu.provisioner.core.model.builder.ModelBuilderParseException
import org.linkedin.groovy.util.net.GroovyNetUtils

import javax.servlet.http.HttpServletResponse

/**
 * @author: ypujante@linkedin.com
 */
public class ModelController extends ControllerBase
{
  AgentsService agentsService
  SystemService systemService
  ConsoleConfig consoleConfig
  SystemModelRenderer systemModelRenderer

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * Listing all systems (paginated)
   */
  def list = {
    def map =
      systemService.findSystems(request.fabric.name, false, params)

    def current = systemService.findCurrentSystemDetails(request.fabric.name)

    if(current.systemId != request.system?.id)
    {
      flash.error("It looks like the current model has been changed... refresh this page")
    }

    [
        systems: map.systems,
        total: map.count,
        current: current
    ]
  }

  /**
   * Viewing a single system (json textarea)
   */
  def view = {
    def system = systemService.findDetailsBySystemId(params.id)
    def current = systemService.findCurrentSystemDetails(request.fabric.name)
    if(current.systemId != request.system?.id)
    {
      flash.error("It looks like the current model has been changed... refresh this page")
    }
    [systemDetails: system, renderer: systemModelRenderer, current: current]
  }

  /**
   * Saving the current system
   */
  def save = {
    def system = DbSystemModel.findBySystemId(params.id)

    try
    {
      def systemModel = DbSystemModel.SERIALIZER.deserialize(params.content)
      if(systemModel.fabric != request.fabric.name)
      {
        flash.error = "Fabric mismatch: ${systemModel.fabric} != ${request.fabric.name}"
        render(view: 'view', id: params.id, model: [system: system])
        return
      }
      systemModel.id = null
      if(systemService.saveCurrentSystem(systemModel))
        flash.success = "New system properly saved [${systemModel.id}]"
      else
        flash.info = "Already current system"
      redirect(action: 'view', id: systemModel.id)
    }
    catch(JsonParseException e)
    {
      flash.error = "Error with the model syntax: ${e.message}"
      render(view: 'view', id: params.id, model: [systemDetails: system])
    }
    catch(Throwable th)
    {
      flashException("Could not save the new model: ${th.message}", th)
      render(view: 'view', id: params.id, model: [systemDetails: system])
    }
  }

  /**
   * Set the system provided as the current one
   */
  def setAsCurrent = {
    try
    {
      boolean res = systemService.setAsCurrentSystem(request.fabric.name, params.id)

      if(res)
        flash.success = "Current system has been set to [${params.id}]"
      else
        flash.info = "Current system is already [${params.id}]"

      redirect(action: 'list')
    }
    catch(Throwable th)
    {
      flashException("Could not set the model as the current one: ${th.message}", th)
      render(action: 'list')
    }
  }

  def choose = {
    return [:]
  }

  def load = {
    try
    {
      def res = saveCurrentSystem()

      if(res.errors)
      {
        render(view: 'model_errors', model: [errors: res.errors])
      }
      else
      {
        flash.success = "Model loaded successfully"
        redirect(controller: 'dashboard')
      }
    }
    catch(ModelBuilderParseException e)
    {
      render(view: 'choose', model: [syntaxError: e])
      return
    }
    catch(Throwable th)
    {
      flashException(th)
      render(view: 'choose')
      return
    }
  }

  def upload = load

  private def saveCurrentSystem()
  {
    def source
    def filename

    if(params.jsonUri)
    {
      source = new URI(params.jsonUri)
      filename = GroovyNetUtils.guessFilename(source)
    }
    else
    {
      def multiPartFile = request.getFile('jsonFile')
      source = multiPartFile.inputStream
      filename = multiPartFile.originalFilename
    }

    SystemModel model = systemService.parseSystemModel(source, filename)

    return saveCurrentSystem(model)
  }

  private def saveCurrentSystem(SystemModel model)
  {
    withLock('ModelController.saveCurrentSystem') {
      if(model)
      {
        if(model.fabric != request.fabric.name)
          throw new IllegalArgumentException("mismatch fabric ${request.fabric.name} != ${model.fabric}")
        try
        {
          boolean saved = systemService.saveCurrentSystem(model)
          return [system: model, saved: saved]
        }
        catch (SystemStorageException e)
        {
          return [system: model, errors: e.errors]
        }
      }
      else
      {
        return null
      }
    }
  }

  /**
   * POST on /model/static with id
   */
  def rest_set_as_current = {
    try
    {
      boolean res = systemService.setAsCurrentSystem(request.fabric.name, params.id)
      if(res)
        response.setStatus(HttpServletResponse.SC_OK)
      else
        response.setStatus(HttpServletResponse.SC_NO_CONTENT)
      render ''
    }
    catch(IllegalArgumentException e)
    {
      response.setStatus HttpServletResponse.SC_NOT_FOUND
      render ''
    }
  }

  /**
   * POST on /model/static
   */
  def rest_upload_model = {
    try
    {
      // handle set as current
      if(params.id)
      {
        rest_set_as_current()
        return
      }

      def source
      def filename

      if(params.modelUrl)
      {
        source = new URI(params.modelUrl)
        filename = GroovyNetUtils.guessFilename(source)
      }
      else
      {
        source = request.inputStream
        switch(request.contentType)
        {
          case 'text/json':
            filename = '<inputStream>.json'
            break

          case 'text/json+groovy':
            filename = '<inputStream>.json.groovy'
            break

          default:
            filename = null
            break
        }

        if(params.filename)
          filename = params.filename
      }

      SystemModel model = systemService.parseSystemModel(source, filename)

      def res = saveCurrentSystem(model)

      if(!res.errors)
      {
        if(res.saved)
        {
          response.setStatus(HttpServletResponse.SC_CREATED)
          render "id=${res.system.id}"
        }
        else
        {
          response.setStatus(HttpServletResponse.SC_NO_CONTENT)
          render ''
        }
      }
      else
        response.sendError HttpServletResponse.SC_NOT_FOUND
    }
    catch(ModelBuilderParseException e)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "${e.message}\n${e.excerpt}")
    }
    catch(Throwable th)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, th.message)
    }
  }

  /**
   * Handle HEAD /models/static
   */
  def rest_count_static_models = {
    response.addHeader("X-glu-current", request.system.id)
    response.addHeader("X-glu-totalCount",
                       systemService.getSystemsCount(request.fabric.name).toString())
    response.setStatus(HttpServletResponse.SC_OK)
    render ''
  }

  static def MODEL_SORT_MAPPING = [
    id         : 'systemId',
    fabric     : 'fabric',
    name       : 'name',
    size       : 'size',
    timeCreated: 'id',
    viewURL    : 'systemId'
  ]

  /**
   * Handle GET /models/static
   */
  def rest_list_static_models = {

    if(!params.sort || !MODEL_SORT_MAPPING.containsKey(params.sort))
      params.sort = 'timeCreated'

    def requestedSort = params.sort

    params.sort = MODEL_SORT_MAPPING[params.sort] ?: 'id'

    def map = systemService.findSystems(request.fabric.name, false, params)

    params.sort = requestedSort

    def res = []

    map.systems.each { def model ->
      res << [
        id: model.systemId,
        timeCreated: model.dateCreated.time,
        fabric: model.fabric,
        size: model.size,
        name: model.name,
        viewURL: g.createLink(absolute: true,
                              mapping: 'restStaticModel',
                              id: model.systemId,
                              params: [fabric: request.fabric.name]).toString()
      ]
    }

    response.addHeader("X-glu-current", request.system.id)
    response.addHeader("X-glu-count", map.systems.size().toString())
    response.addHeader("X-glu-totalCount", map.count.toString())
    ['max', 'offset', 'sort', 'order'].each { k ->
      response.addHeader("X-glu-${k}", params[k].toString())
    }

    response.setContentType('text/json')
    render prettyPrintJsonWhenRequested(res)
  }

  /**
   * Handle GET /model/static/$id?
   */
  def rest_get_static_model = {
    def system = request.system

    if(params.id)
    {
      system = systemService.findDetailsBySystemId(params.id)?.systemModel
    }

    if(!system)
    {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
      render ''
    }
    else
      renderModelWithETag(system)
  }

  /**
   * Handle GET /model/live
   */
  def rest_get_live_model = {
    def model = agentsService.getCurrentSystemModel(request.fabric)
    model = model.filterBy(request.system.filters)
    renderModelWithETag(model)
  }

  /**
   * Handle the rendering of the model taking into account etag
   *
   * YP note: since the use of jackson, the output from any of the <code>xxPrint</code> method
   * is always consistent (meaning if you call it twice for the same model, you always get the
   * same output), so there is no need of separately computing the systemId to include in the ETag
   * which removes an expensive call!
   */
  private void renderModelWithETag(SystemModel model)
  {
    String modelString

    if(params.prettyPrint)
      modelString = systemModelRenderer.prettyPrint(model)
    else
    {
      if(params.canonicalPrint)
        modelString = systemModelRenderer.canonicalPrint(model)
      else
        modelString = systemModelRenderer.compactPrint(model)
    }

    // the etag is a combination of the model content + request uri (path + query string)
    String etag = """
${modelString}
${request['javax.servlet.forward.servlet_path']}
${request['javax.servlet.forward.query_string']}
"""
    etag = ConsoleHelper.computeChecksum(etag)

    // handling ETag
    if(request.getHeader('If-None-Match') == etag)
    {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
      render ''
      return
    }

    response.setHeader('Etag', etag)
    response.setContentType('text/json')
    render modelString
  }
}