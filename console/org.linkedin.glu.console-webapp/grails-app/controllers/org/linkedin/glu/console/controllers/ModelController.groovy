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

package org.linkedin.glu.console.controllers

import javax.servlet.http.HttpServletResponse

import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer

import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.console.provisioner.services.storage.SystemStorageException
import org.linkedin.glu.grails.utils.ConsoleHelper
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.console.domain.DbSystemModel

/**
 * @author: ypujante@linkedin.com
 */
public class ModelController extends ControllerBase
{
  AgentsService agentsService
  SystemService systemService
  ConsoleConfig consoleConfig

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

    [
        systems: map.systems,
        total: map.count
    ]
  }

  /**
   * Viewing a single system (json textarea)
   */
  def view = {
    def system = systemService.findDetailsBySystemId(params.id)
    [systemDetails: system]
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
        flash.success = "Model loaded succesfully"
        redirect(controller: 'dashboard')
      }
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

    if(params.jsonUri)
    {
      source = new URI(params.jsonUri)
    }
    else
    {
      source = request.getFile('jsonFile').inputStream
    }

    SystemModel model = systemService.parseSystemModel(source)

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

  def rest_upload_model = {
    try
    {
      def source
      
      if(params.modelUrl)
      {
        source = new URI(params.modelUrl)
      }
      else
      {
        source = request.inputStream
      }

      SystemModel model = systemService.parseSystemModel(source)

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
    catch(Throwable th)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, th.message)
    }
  }

  /**
   * Handle GET /model/static
   */
  def rest_get_static_model = {
    renderModelWithETag(request.system)
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
   */
  private void renderModelWithETag(SystemModel model)
  {
    // the etag is a combination of the model content + request uri (path + query string)
    String etag = """
${model.computeContentSha1()}
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

    String modelString

    if(params.prettyPrint)
      modelString = model.toString()
    else
      modelString = JSONSystemModelSerializer.INSTANCE.serialize(model)

    response.setHeader('Etag', etag)
    response.setContentType('text/json')
    render modelString
  }
}