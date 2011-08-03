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

package test.script.jetty

import org.linkedin.glu.scripts.testFwk.GluScriptBaseTest
import org.linkedin.glu.agent.api.ScriptExecutionCauseException
import org.linkedin.groovy.util.lang.GroovyLangUtils
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils

/**
 * @author yan@pongasoft.com */
public class TestJettyGluScript extends GluScriptBaseTest
{
  URI skeletonURI
  URI warURI

  @Override
  protected void setUp()
  {
    super.setUp()

    skeletonURI = new File(System.properties.skeleton).toURI()
    warURI = new File(System.properties.war).toURI()

    initParameters = [
      skeleton: skeletonURI,
      port: 9000,
      webapps: [
        contextPath: '/cp1',
        monitor: '/monitor',
        war: warURI
      ]
    ]
  }

  /**
   * basic script testing (happy path)
   */
  public void testScript()
  {
    // first we make sure there is nothing running...
    shouldFail(ConnectException) {
      shell.httpHead('http://localhost:9000/cp1/monitor')
    }

    deploy()

    // now the server should be up and running
    def head = shell.httpHead('http://localhost:9000/cp1/monitor')
    assertEquals(200, head.responseCode)
    assertEquals('GOOD', head.responseMessage)

    // test various values from the deployed script
    assertEquals('running', stateMachineState.currentState)
    assertNull(stateMachineState.error)
    assertEquals(9000, getExportedScriptFieldValue('port'))
    assertEquals('/cp1', getExportedScriptFieldValue('webapps').'/cp1'.'contextPath')
    assertEquals('/monitor', getExportedScriptFieldValue('webapps').'/cp1'.'monitor')
    assertEquals(warURI, getExportedScriptFieldValue('webapps').'/cp1'.'remoteWar')

    undeploy()

    // the server should not be running anymore
    shouldFail(ConnectException) {
      shell.httpHead('http://localhost:9000/cp1/monitor')
    }
  }

  /**
   * test bad skeleton
   */
  public void testBadSkeleton()
  {
    initParameters.skeleton = new URI('file:/not/a/valid/skeleton')
    installScript()

    ScriptExecutionCauseException ex = scriptShouldFail {
      install()
    }

    assertEquals('[java.io.FileNotFoundException]: /not/a/valid/skeleton (No such file or directory)',
                 ex.message)
  }

  /**
   * test bad war
   */
  public void testBadWar()
  {
    initParameters.webapps['war'] = new URI('file:/not/a/valid/war')
    installScript()
    install()

    ScriptExecutionCauseException ex = scriptShouldFail {
      configure()
    }

    assertEquals('[java.io.FileNotFoundException]: /not/a/valid/war (No such file or directory)',
                 ex.message)
  }

  /**
   * Test that the monitor properly detects when the application is busy or down
   */
  public void testMonitor()
  {
    initParameters.serverMonitorFrequency = '100' // making it faster for this test

    // first we make sure there is nothing running...
    shouldFail(ConnectException) {
      shell.httpHead('http://localhost:9000/cp1/monitor')
    }

    deploy()

    // now the server should be up and running
    def head = shell.httpHead('http://localhost:9000/cp1/monitor')
    assertEquals(200, head.responseCode)
    assertEquals('GOOD', head.responseMessage)

    assertEquals('running', stateMachineState.currentState)
    assertNull(stateMachineState.error)

    def post = shell.httpPost('http://localhost:9000/cp1/monitor', ['monitor.webapp.state': 'BUSY'])
    assertEquals(200, post.responseCode)

    // the monitor will detect the error
    GroovyConcurrentUtils.waitForCondition(clock, '10s', '50') {
      stateMachineState.error != null
    }

    // now the monitor servlet returns BUSY
    head = shell.httpHead('http://localhost:9000/cp1/monitor')
    assertEquals(503, head.responseCode)
    assertEquals('BUSY', head.responseMessage)

    // state machine is still running but some apps are in error
    assertEquals('running', stateMachineState.currentState)
    assertEquals('Server is up but some webapps are busy. Check the log file for errors.', stateMachineState.error)

    // now I stop jetty (outside of glu)
    shell.exec("${getExportedScriptFieldValue('serverCmd')} stop")

    // the monitor will detect the error
    GroovyConcurrentUtils.waitForCondition(clock, '10s', '50') {
      stateMachineState.currentState == 'stopped'
    }

    // at this stage, the container is completely down
    shouldFail(ConnectException) {
      shell.httpHead('http://localhost:9000/cp1/monitor')
    }

    // state machine is now in stopped state
    assertEquals('stopped', stateMachineState.currentState)
    assertEquals('Server down detected. Check the log file for errors.', stateMachineState.error)

    // now I restart jetty (outside of glu)
    shell.exec("${getExportedScriptFieldValue('serverCmd')} start > /dev/null 2>&1 &")

    // the monitor will detect the restart
    GroovyConcurrentUtils.waitForCondition(clock, '10s', '50') {
      stateMachineState.currentState == 'running'
    }

    // everything should be back to normal
    head = shell.httpHead('http://localhost:9000/cp1/monitor')
    assertEquals(200, head.responseCode)
    assertEquals('GOOD', head.responseMessage)

    assertEquals('running', stateMachineState.currentState)
    assertNull(stateMachineState.error)

    undeploy()

    // the server should not be running anymore
    shouldFail(ConnectException) {
      shell.httpHead('http://localhost:9000/cp1/monitor')
    }
  }
}