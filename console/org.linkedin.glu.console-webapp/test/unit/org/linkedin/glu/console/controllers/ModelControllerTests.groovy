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


/**
 * @author yan@pongasoft.com */
class ModelControllerTests extends ControllerUnitTestCase
{
  void testGetStaticModelWithETag1()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    // test with no etag => returns the model
    controller.request.system = systemModel
    controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
    controller.params.prettyPrint = true
    controller.rest_get_static_model()

    assertEquals(systemModel.toString(), controller.response.contentAsString)
    assertEquals("f8a5658c3e695fa2ea4d14bb08ed48959b35ea08", controller.response.getHeader('ETag'))
  }

  void testGetStaticModelWithETag2()
  {
    SystemModel systemModel = new SystemModel()
    systemModel.metadata.p1 = 'v1'

    // when providing If-None-Match => should get 304
    controller.request.system = systemModel
    controller.request['javax.servlet.forward.query_string'] = "prettyPrint=true"
    controller.params.prettyPrint = true
    controller.request.addHeader('If-None-Match', "f8a5658c3e695fa2ea4d14bb08ed48959b35ea08")
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
    controller.request.addHeader('If-None-Match', "f8a5658c3e695fa2ea4d14bb08ed48959b35ea08")
    controller.rest_get_static_model()

    assertEquals(systemModel.toString(), controller.response.contentAsString)
    assertEquals("45b35ce1364c5a66d4b3a696fc002f923b288974", controller.response.getHeader('ETag'))
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
    controller.request.addHeader('If-None-Match', "f8a5658c3e695fa2ea4d14bb08ed48959b35ea08")
    controller.rest_get_static_model()

    assertEquals(systemModel.toString(), controller.response.contentAsString)
    assertEquals("f603f374b32d2a460aed128be819340a86e21d2e", controller.response.getHeader('ETag'))
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

      assertEquals(systemModel.toString(), controller.response.contentAsString)
      assertEquals("f8a5658c3e695fa2ea4d14bb08ed48959b35ea08", controller.response.getHeader('ETag'))
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
      controller.request.addHeader('If-None-Match', "f8a5658c3e695fa2ea4d14bb08ed48959b35ea08")
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
      controller.request.addHeader('If-None-Match', "f8a5658c3e695fa2ea4d14bb08ed48959b35ea08")
      controller.rest_get_live_model()

      assertEquals(systemModel.toString(), controller.response.contentAsString)
      assertEquals("45b35ce1364c5a66d4b3a696fc002f923b288974", controller.response.getHeader('ETag'))
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
      controller.request.addHeader('If-None-Match', "f8a5658c3e695fa2ea4d14bb08ed48959b35ea08")
      controller.rest_get_live_model()

      assertEquals(systemModel.toString(), controller.response.contentAsString)
      assertEquals("f603f374b32d2a460aed128be819340a86e21d2e", controller.response.getHeader('ETag'))
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
