/*
 * Copyright (c) 2012 Yan Pujante
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

package test.orchestration.engine.commands

import groovy.mock.interceptor.MockFor
import org.linkedin.glu.commands.impl.MemoryCommandExecutionIOStorage
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.glu.orchestration.engine.commands.CommandsServiceImpl
import org.linkedin.glu.orchestration.engine.commands.MemoryCommandExecutionStorage
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.util.clock.SettableClock

import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.orchestration.engine.commands.DbCommandExecution
import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.util.lang.MemorySize
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import java.util.concurrent.CancellationException
import org.linkedin.glu.utils.io.NullOutputStream
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils

/**
 * @author yan@pongasoft.com */
public class TestCommandsServiceImpl extends GroovyTestCase
{
  SettableClock clock = new SettableClock()

  MemoryCommandExecutionStorage storage = new MemoryCommandExecutionStorage()
  MemoryCommandExecutionIOStorage ioStorage = new MemoryCommandExecutionIOStorage(clock: clock)


  CommandsServiceImpl service =
    new CommandsServiceImpl(clock: clock,
                            commandExecutionStorage: storage,
                            commandExecutionIOStorage: ioStorage,
                            commandExecutionFirstBytesSize: MemorySize.parse("5"),
                            defaultSynchronousWaitTimeout: null)

  @Override
  protected void setUp()
  {
    super.setUp()
    clock.setCurrentTimeMillis(100000000)
  }

  /**
   * Test for the basic case
   */
  public void testHappyPath()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    def f1 = new Fabric(name: "f1")

    withAuthorizationService {

      def agentsServiceMock = new MockFor(AgentsService)

      def commandId = null

      // executeShellCommand
      agentsServiceMock.demand.executeShellCommand { fabric, agentName, args ->
        assertEquals(f1, fabric)
        assertEquals("a1", agentName)

        def copyOfArgs = [*:args]
        commandId = copyOfArgs.remove("id")

        assertEqualsIgnoreType([
                                 command: 'uptime',
                                 type: 'shell',
                                 fabric: 'f1',
                                 agent: 'a1',
                                 username: 'u1',
                                 redirectStderr:false
                               ],
                               copyOfArgs)

        clock.addDuration(Timespan.parse("10s"))

        tc.block("executeShellCommand.end")
      }

      // stream result (this will be called asynchronously!)
      agentsServiceMock.demand.streamCommandResults { Fabric fabric,
                                                      String agentName,
                                                      args,
                                                      Closure commandResultProcessor ->
        assertEquals(f1, fabric)
        assertEquals("a1", agentName)

        assertEqualsIgnoreType([
                                 id: commandId,
                                 exitValueStream: true,
                                 exitValueStreamTimeout: 0,
                                 stdoutStream:true,
                                 username: 'u1',
                                 stderrStream:true
                               ],
                               args)

        def streams = [:]

        InputStream exitValueInputStream = new InputGeneratorStream({ 14 })
        streams[StreamType.exitValue.multiplexName] = exitValueInputStream

        streams[StreamType.stdout.multiplexName] = new ByteArrayInputStream("O123456789".bytes)
        streams[StreamType.stderr.multiplexName] = new ByteArrayInputStream("E123456789".bytes)

        commandResultProcessor([stream: new MultiplexedInputStream(streams)])
      }

      AgentsService agentsService = agentsServiceMock.proxyInstance()
      service.agentsService = agentsService

      long startTime = clock.currentTimeMillis()

      // execute the shell command
      String cid = service.executeShellCommand(f1, "a1", [command: "uptime"])

      // we wait until the shell command is "executing"
      tc.waitForBlock("executeShellCommand.end")

      long completionTime = clock.currentTimeMillis()

      // we make sure that we got the same commandId
      assertEquals(cid, commandId)

      CommandExecution<DbCommandExecution> ce = service._currentCommandExecutions[cid]
      assertTrue(ce.command.isExecuting)

      def dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertFalse(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertNull(dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertNull(dbCommandExecution.stdinFirstBytes)
      assertNull(dbCommandExecution.stdinTotalBytesCount)
      assertNull(dbCommandExecution.stdoutFirstBytes)
      assertNull(dbCommandExecution.stdoutTotalBytesCount)
      assertNull(dbCommandExecution.stderrFirstBytes)
      assertNull(dbCommandExecution.stderrTotalBytesCount)
      assertTrue(dbCommandExecution.isExecuting)
      assertFalse(dbCommandExecution.isException)

      // test bulk methods
      def ces = service.findCommandExecutions(f1, null, null)
      assertEquals(1, ces.count)
      assertEquals(1, ces.commandExecutions.size())
      assertTrue(ces.commandExecutions[0].isExecuting)

      // we can now unblock the call
      tc.unblock("executeShellCommand.end")

      // get the results
      service.withCommandExecutionAndWithOrWithoutStreams(f1,
                                                          cid,
                                                          [
                                                            stdoutStream: true,
                                                            exitValueStream: true,
                                                            exitValueStreamTimeout: 0
                                                          ]) { args ->

        def streams =
          MultiplexedInputStream.demultiplexToString(args.stream,
                                                     [
                                                       StreamType.exitValue.multiplexName,
                                                       StreamType.stdout.multiplexName
                                                     ] as Set,
                                                     null)

        assertEquals("14", streams[StreamType.exitValue.multiplexName])
        assertEquals("O123456789", streams[StreamType.stdout.multiplexName])
      }

      agentsServiceMock.verify(agentsService)

      assertNull("command is completed", service._currentCommandExecutions[cid])
      dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertFalse(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertEquals(completionTime, dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertNull(dbCommandExecution.stdinFirstBytes)
      assertNull(dbCommandExecution.stdinTotalBytesCount)
      assertEquals("O1234", new String(dbCommandExecution.stdoutFirstBytes))
      assertEquals(10, dbCommandExecution.stdoutTotalBytesCount)
      assertEquals("E1234", new String(dbCommandExecution.stderrFirstBytes))
      assertEquals(10, dbCommandExecution.stderrTotalBytesCount)
      assertEquals("14", dbCommandExecution.exitValue)
      assertFalse(dbCommandExecution.isExecuting)
      assertFalse(dbCommandExecution.isException)
    }
  }

  /**
   * Test with stdin and redirectStderr
   */
  public void testStdinAndRedirectStderr()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    def f1 = new Fabric(name: "f1")

    withAuthorizationService {

      def agentsServiceMock = new MockFor(AgentsService)

      def commandId = null

      // executeShellCommand
      agentsServiceMock.demand.executeShellCommand { fabric, agentName, args ->
        assertEquals(f1, fabric)
        assertEquals("a1", agentName)

        def copyOfArgs = [*:args]
        commandId = copyOfArgs.remove("id")
        copyOfArgs.stdin = args.stdin.text

        assertEqualsIgnoreType([
                                 command: 'uptime',
                                 type: 'shell',
                                 fabric: 'f1',
                                 agent: 'a1',
                                 username: 'u1',
                                 redirectStderr:true,
                                 stdin: "I123456789"
                               ],
                               copyOfArgs)

        clock.addDuration(Timespan.parse("10s"))

        tc.block("executeShellCommand.end")
      }

      // stream result (this will be called asynchronously!)
      agentsServiceMock.demand.streamCommandResults { Fabric fabric,
                                                      String agentName,
                                                      args,
                                                      Closure commandResultProcessor ->
        assertEquals(f1, fabric)
        assertEquals("a1", agentName)

        assertEqualsIgnoreType([
                                 id: commandId,
                                 exitValueStream: true,
                                 exitValueStreamTimeout: 0,
                                 stdoutStream:true,
                                 username: 'u1',
                               ],
                               args)

        def streams = [:]

        InputStream exitValueInputStream = new InputGeneratorStream({ 14 })
        streams[StreamType.exitValue.multiplexName] = exitValueInputStream

        streams[StreamType.stdout.multiplexName] = new ByteArrayInputStream("O123456789".bytes)

        commandResultProcessor([stream: new MultiplexedInputStream(streams)])
      }

      AgentsService agentsService = agentsServiceMock.proxyInstance()
      service.agentsService = agentsService

      long startTime = clock.currentTimeMillis()

      // execute the shell command
      String cid = service.executeShellCommand(f1,
                                               "a1",
                                               [
                                                 command: "uptime",
                                                 stdin: new ByteArrayInputStream("I123456789".bytes),
                                                 redirectStderr: true
                                               ])

      // we wait until the shell command is "executing"
      tc.waitForBlock("executeShellCommand.end")

      long completionTime = clock.currentTimeMillis()

      // we make sure that we got the same commandId
      assertEquals(cid, commandId)

      CommandExecution<DbCommandExecution> ce = service._currentCommandExecutions[cid]
      assertTrue(ce.command.isExecuting)

      def dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertTrue(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertNull(dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertEquals("I1234", new String(dbCommandExecution.stdinFirstBytes))
      assertEquals(10, dbCommandExecution.stdinTotalBytesCount)
      assertNull(dbCommandExecution.stdoutFirstBytes)
      assertNull(dbCommandExecution.stdoutTotalBytesCount)
      assertNull(dbCommandExecution.stderrFirstBytes)
      assertNull(dbCommandExecution.stderrTotalBytesCount)
      assertTrue(dbCommandExecution.isExecuting)

      // test bulk methods
      def ces = service.findCommandExecutions(f1, null, null)
      assertEquals(1, ces.count)
      assertEquals(1, ces.commandExecutions.size())
      assertTrue(ces.commandExecutions[0].isExecuting)

      // we can now unblock the call
      tc.unblock("executeShellCommand.end")

      // get the results
      service.withCommandExecutionAndWithOrWithoutStreams(f1,
                                                          cid,
                                                          [
                                                            stdoutStream: true,
                                                            exitValueStream: true,
                                                            exitValueStreamTimeout: 0
                                                          ]) { args ->

        def streams =
          MultiplexedInputStream.demultiplexToString(args.stream,
                                                     [
                                                       StreamType.exitValue.multiplexName,
                                                       StreamType.stdout.multiplexName
                                                     ] as Set,
                                                     null)

        assertEquals("14", streams[StreamType.exitValue.multiplexName])
        assertEquals("O123456789", streams[StreamType.stdout.multiplexName])
      }

      agentsServiceMock.verify(agentsService)

      assertNull("command is completed", service._currentCommandExecutions[cid])
      dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertTrue(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertEquals(completionTime, dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertEquals("I1234", new String(dbCommandExecution.stdinFirstBytes))
      assertEquals(10, dbCommandExecution.stdinTotalBytesCount)
      assertEquals("O1234", new String(dbCommandExecution.stdoutFirstBytes))
      assertEquals(10, dbCommandExecution.stdoutTotalBytesCount)
      assertNull(dbCommandExecution.stderrFirstBytes)
      assertNull(dbCommandExecution.stderrTotalBytesCount)
      assertEquals("14", dbCommandExecution.exitValue)
      assertFalse(dbCommandExecution.isExecuting)
    }
  }

  /**
   * Test with failure when the command execute (note that typical use case here would be
   * that we cannot talk to the agent...)
   */
  public void testWithFailureInCommandExecution()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    def f1 = new Fabric(name: "f1")

    withAuthorizationService {

      def agentsServiceMock = new MockFor(AgentsService)

      // executeShellCommand
      agentsServiceMock.demand.executeShellCommand { fabric, agentName, args ->
        clock.addDuration(Timespan.parse("10s"))
        tc.blockWithException("executeShellCommand.end")
      }

      AgentsService agentsService = agentsServiceMock.proxyInstance()
      service.agentsService = agentsService

      long startTime = clock.currentTimeMillis()

      // execute the shell command
      String cid = service.executeShellCommand(f1,
                                               "a1",
                                               [
                                                 command: "uptime",
                                                 stdin: new ByteArrayInputStream("I123456789".bytes),
                                                 redirectStderr: true
                                               ])

      // we wait until the shell command is "executing"
      tc.waitForBlock("executeShellCommand.end")

      long completionTime = clock.currentTimeMillis()

      CommandExecution<DbCommandExecution> ce = service._currentCommandExecutions[cid]
      assertTrue(ce.command.isExecuting)

      def dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertTrue(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertNull(dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertEquals("I1234", new String(dbCommandExecution.stdinFirstBytes))
      assertEquals(10, dbCommandExecution.stdinTotalBytesCount)
      assertNull(dbCommandExecution.stdoutFirstBytes)
      assertNull(dbCommandExecution.stdoutTotalBytesCount)
      assertNull(dbCommandExecution.stderrFirstBytes)
      assertNull(dbCommandExecution.stderrTotalBytesCount)
      assertTrue(dbCommandExecution.isExecuting)
      assertFalse(dbCommandExecution.isException)

      // test bulk methods
      def ces = service.findCommandExecutions(f1, null, null)
      assertEquals(1, ces.count)
      assertEquals(1, ces.commandExecutions.size())
      assertTrue(ces.commandExecutions[0].isExecuting)

      def exception = new Exception("failure => agentsService.executeShellCommand")

      // we can now unblock the call
      tc.unblock("executeShellCommand.end", exception)

      // get the results
      service.withCommandExecutionAndWithOrWithoutStreams(f1,
                                                          cid,
                                                          [
                                                            exitValueStream: true,
                                                            exitValueStreamTimeout: 0
                                                          ]) { args ->

        try
        {
          MultiplexedInputStream.demultiplexToString(args.stream,
                                                     [
                                                       StreamType.exitValue.multiplexName,
                                                     ] as Set,
                                                     null)
          fail("should have failed!")
        }
        catch(IOException e)
        {
          assertEquals(exception, e.cause)
        }
      }

      agentsServiceMock.verify(agentsService)

      assertNull("command is completed", service._currentCommandExecutions[cid])
      dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertTrue(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertEquals(completionTime, dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertEquals("I1234", new String(dbCommandExecution.stdinFirstBytes))
      assertEquals(10, dbCommandExecution.stdinTotalBytesCount)
      assertNull(dbCommandExecution.stdoutFirstBytes)
      assertNull(dbCommandExecution.stdoutTotalBytesCount)
      assertNull(dbCommandExecution.stderrFirstBytes)
      assertNull(dbCommandExecution.stderrTotalBytesCount)
      assertEquals(GluGroovyLangUtils.getStackTrace(exception), dbCommandExecution.exitValue)
      assertFalse(dbCommandExecution.isExecuting)
      assertTrue(dbCommandExecution.isException)
    }
  }

  /**
   * Make sure that interrupts work as advertised!
   */
  public void testInterrupt()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    def f1 = new Fabric(name: "f1")

    withAuthorizationService {

      final def agentsServiceMock = new MockFor(AgentsService)

      final def lock = new Object()

      def simulateBlockingCommand = {
        synchronized(lock)
        {
          tc.blockWithException("simulateBlockingCommand.start")
          lock.wait()
        }
      }

      def future = new FutureTaskExecution(simulateBlockingCommand)
      future.clock = clock

      // executeShellCommand
      agentsServiceMock.demand.executeShellCommand { fabric, agentName, args ->
        clock.addDuration(Timespan.parse("10s"))
        future.runSync()
      }

      // interrupt command
      agentsServiceMock.demand.interruptCommand { fabric, agentName, args ->
        future.cancel(true)
      }

      AgentsService agentsService = agentsServiceMock.proxyInstance()
      service.agentsService = agentsService

      long startTime = clock.currentTimeMillis()

      // execute the shell command
      String cid = service.executeShellCommand(f1,
                                               "a1",
                                               [
                                                 command: "uptime",
                                                 redirectStderr: true
                                               ])

      // we wait until the shell command is "executing"
      tc.waitForBlock("simulateBlockingCommand.start")

      long completionTime = clock.currentTimeMillis()

      // get the results
      service.withCommandExecutionAndWithOrWithoutStreams(f1,
                                                          cid,
                                                          [
                                                            exitValueStream: true,
                                                            exitValueStreamTimeout: 0
                                                          ]) { args ->

        // at this stage the exit value stream is blocking: reading the stream will block
        // and throw an exception => interrupting the process
        assertTrue(service.interruptCommand(f1, "a1", cid))

        shouldFailWithCause(CancellationException) {
          NullOutputStream.INSTANCE << args.stream
        }
      }

      agentsServiceMock.verify(agentsService)

      CommandExecution ce = ioStorage.findCommandExecution(cid)
      assertTrue(ce.isCompleted())

      assertNull("command is completed", service._currentCommandExecutions[cid])
      DbCommandExecution dbCommandExecution = service.findCommandExecution(f1, cid)
      assertEquals(cid, dbCommandExecution.commandId)
      assertEquals("uptime", dbCommandExecution.command)
      assertTrue(dbCommandExecution.redirectStderr)
      assertEquals(DbCommandExecution.CommandType.SHELL, dbCommandExecution.commandType)
      assertEquals(startTime, dbCommandExecution.startTime)
      assertEquals(completionTime, dbCommandExecution.completionTime)
      assertEquals(f1.name, dbCommandExecution.fabric)
      assertEquals("a1", dbCommandExecution.agent)
      assertNull(dbCommandExecution.stdinFirstBytes)
      assertNull(dbCommandExecution.stdinTotalBytesCount)
      assertNull(dbCommandExecution.stdoutFirstBytes)
      assertNull(dbCommandExecution.stdoutTotalBytesCount)
      assertNull(dbCommandExecution.stderrFirstBytes)
      assertNull(dbCommandExecution.stderrTotalBytesCount)
      assertEquals(GluGroovyLangUtils.getStackTrace(ce.completionValue), dbCommandExecution.exitValue)
      assertTrue(dbCommandExecution.isException)
      assertFalse(dbCommandExecution.isExecuting)

      // now that the command is complete...
      service.withCommandExecutionAndWithOrWithoutStreams(f1,
                                                          cid,
                                                          [
                                                            exitValueStream: true
                                                          ]) { args ->
        dbCommandExecution = args.commandExecution
        assertTrue(dbCommandExecution.isException)

        // now that the execution is complete... there is no exit value so it should not fail
        assertEquals("", args.stream.text)
      }

      // now that the command is complete...
      service.withCommandExecutionAndWithOrWithoutStreams(f1,
                                                          cid,
                                                          [
                                                            exitErrorStream: true
                                                          ]) { args ->
        // now that the execution is complete... we should get the error stream
        assertEquals(GluGroovyJsonUtils.exceptionToJSON(ce.completionValue), args.stream.text)
      }
    }
  }

  private void withAuthorizationService(Closure closure)
  {
    def authorizationServiceMock = new MockFor(AuthorizationService)
    authorizationServiceMock.demand.getExecutingPrincipal { "u1" }

    AuthorizationService authorizationService = authorizationServiceMock.proxyInstance()
    service.authorizationService = authorizationService

    closure()

    authorizationServiceMock.verify(authorizationService)
  }

  /**
   * Convenient call to compare and ignore type
   */
  void assertEqualsIgnoreType(o1, o2)
  {
    assertEquals(JsonUtils.prettyPrint(o1), JsonUtils.prettyPrint(o2))
    assertTrue("expected <${o1}> but was <${o2}>", GroovyCollectionsUtils.compareIgnoreType(o1, o2))
  }

}