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

package org.linkedin.glu.console.controllers

import grails.test.ControllerUnitTestCase
import org.linkedin.glu.provisioner.core.model.SystemModel
import javax.servlet.http.HttpServletResponse
import groovy.mock.interceptor.MockFor
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.provisioner.core.model.JsonSystemModelRenderer

/**
 * @author yan@pongasoft.com */
class ModelControllerTests extends ControllerUnitTestCase
{
  def renderer = new JsonSystemModelRenderer()

  @Override
  protected void setUp()
  {
    super.setUp()
    controller.systemModelRenderer = renderer
  }

  void testGetStaticModelWithETag1()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    // test with no etag => returns the model
    controller.request.system = systemModel
    controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
    controller.params.prettyPrint = true
    controller.rest_get_static_model()

    assertEquals(renderer.prettyPrint(systemModel), controller.response.contentAsString)
    assertEquals("b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3", controller.response.getHeader('ETag'))
  }

  void testGetStaticModelWithETag2()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    // when providing If-None-Match => should get 304
    controller.request.system = systemModel
    controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
    controller.params.prettyPrint = true
    controller.request.addHeader('If-None-Match', "b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3")
    controller.rest_get_static_model()

    assertEquals(HttpServletResponse.SC_NOT_MODIFIED, controller.response.status)
  }

  void testGetStaticModelWithETag3()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'
    systemModel.metadata.p2 = 'v2'

    // model is different => should return new version with new etag
    controller.request.system = systemModel
    controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
    controller.params.prettyPrint = true
    controller.request.addHeader('If-None-Match', "b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3")
    controller.rest_get_static_model()

    assertEquals(renderer.prettyPrint(systemModel), controller.response.contentAsString)
    assertEquals("0280aa36c429a83554c69c5fcf5192011a0ea066", controller.response.getHeader('ETag'))
  }

  void testGetStaticModelWithETag4()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    // query string is different => should return new version with new etag
    controller.request.system = systemModel
    controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true&legacy=true"
    controller.params.prettyPrint = true
    controller.params.legacy = true
    controller.request.addHeader('If-None-Match', "b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3")
    controller.rest_get_static_model()

    assertEquals(renderer.prettyPrint(systemModel), controller.response.contentAsString)
    assertEquals("5fccb2ee7406abeecea9026f14f8980b941053a5", controller.response.getHeader('ETag'))
  }

  void testGetLiveModelWithETag1()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    withAgentsService(systemModel) {
      // test with no etag => returns the model
      controller.request.system = systemModel
      controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
      controller.params.prettyPrint = true
      controller.rest_get_live_model()

      assertEquals(renderer.prettyPrint(systemModel), controller.response.contentAsString)
      assertEquals("b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3", controller.response.getHeader('ETag'))
    }
  }

  void testGetLiveModelWithETag2()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    withAgentsService(systemModel) {
      // when providing If-None-Match => should get 304
      controller.request.system = systemModel
      controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
      controller.params.prettyPrint = true
      controller.request.addHeader('If-None-Match', "b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3")
      controller.rest_get_live_model()

      assertEquals(HttpServletResponse.SC_NOT_MODIFIED, controller.response.status)
    }
  }

  void testGetLiveModelWithETag3()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'
    systemModel.metadata.p2 = 'v2'

    withAgentsService(systemModel) {
      // model is different => should return new version with new etag
      controller.request.system = systemModel
      controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
      controller.params.prettyPrint = true
      controller.request.addHeader('If-None-Match', "b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3")
      controller.rest_get_live_model()

      assertEquals(renderer.prettyPrint(systemModel), controller.response.contentAsString)
      assertEquals("0280aa36c429a83554c69c5fcf5192011a0ea066", controller.response.getHeader('ETag'))
    }
  }

  void testGetLiveModelWithETag4()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    withAgentsService(systemModel) {
      // query string is different => should return new version with new etag
      controller.request.system = systemModel
      controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true&legacy=true"
      controller.params.prettyPrint = true
      controller.params.legacy = true
      controller.request.addHeader('If-None-Match', "b2b6bb2f0592a918be17d1c2d55ac1411fcf3af3")
      controller.rest_get_live_model()

      assertEquals(renderer.prettyPrint(systemModel), controller.response.contentAsString)
      assertEquals("5fccb2ee7406abeecea9026f14f8980b941053a5", controller.response.getHeader('ETag'))
    }
  }

  private void withAgentsService(SystemModel systemModel, Closure closure)
  {
    def agentsServiceMock = new MockFor(AgentsService)
    agentsServiceMock.demand.getCurrentSystemModel { return systemModel }
    def agentsService = agentsServiceMock.proxyInstance()
    controller.agentsService = agentsService

    closure()

    agentsServiceMock.verify(agentsService)
  }
}
