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


package test.commands.impl

import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.glu.commands.impl.CommandStreamStorage
import org.linkedin.glu.commands.impl.GluCommandFactory
import org.linkedin.glu.commands.impl.MemoryCommandExecutionIOStorage
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.clock.SettableClock
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.utils.io.MultiplexedInputStream

/**
 * @author yan@pongasoft.com */
public class TestMemoryCommandExecutionIOStorage extends GroovyTestCase
{
  SettableClock clock = new SettableClock()
  GluCommandFactory factory = { ce ->
    [fromCommandFactory: [id: ce.id, args: [*:ce.args]]]
  } as GluCommandFactory

  @Override
  protected void setUp()
  {
    super.setUp()

    clock.setCurrentTimeMillis(100000000)
  }

  /**
   * Basic happy path
   */
  public void testHappyPath()
  {
    MemoryCommandExecutionIOStorage ioStorage =
      new MemoryCommandExecutionIOStorage(clock: clock,
                                          maxNumberOfElements: 2,
                                          gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0', xtra0: "x0"])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    assertTrue(ioStorage.completedCommands.isEmpty())
    assertTrue(ioStorage.executingCommands.isEmpty())

    long startTime = clock.currentTimeMillis()

    // make sure that the glu command factory is actually called with the right set of arguments
    assertEqualsIgnoreType([
                             fromCommandFactory: [
                               id: commandId,
                               args: [
                                 id: commandId,
                                 redirectStderr: false,
                                 command: 'c0',
                                 xtra0: 'x0'],
                             ]],
                             ce.command)

    long completionTime = 0L

    def processing = { CommandStreamStorage storage ->

      assertTrue(ioStorage.completedCommands.isEmpty())
      assertEquals(1, ioStorage.executingCommands.size())
      assertTrue(ioStorage.executingCommands[ce.id].is(ce))

      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->
        assertNull("no stdin", stdin)

        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->

          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr->
            stdout << "out0"
            stderr << "err0"

            // simulate delay
            clock.addDuration(Timespan.parse("1s"))

            completionTime = clock.currentTimeMillis()

            // simulate delay
            clock.addDuration(Timespan.parse("1s"))

            return [exitValue: 14, completionTime: completionTime]
          }
        }
      }
    }

    assertEquals(14, ce.syncCaptureIO(processing))

    assertEquals(completionTime, startTime + Timespan.parse("1s").durationInMilliseconds)

    assertEquals(1, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce.id].is(ce))
    assertTrue(ioStorage.executingCommands.isEmpty())

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())

    def checkCommand = { CommandExecution command ->
      assertTrue(command.is(ce))
    }

    // now that the command has completed, it should still be available
    checkCommand(ioStorage.findCommandExecution(commandId))

    // not requesting any stream
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [:]) { m ->
      checkCommand(m.commandExecution)
      assertNull(m.stream)
    }

    // no stdin...
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdinStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("", m.stream.text)
    }

    // stdout
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdoutStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("out0", m.stream.text)
    }

    // stderr
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stderrStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("err0", m.stream.text)
    }
  }

  /**
   * Test stdin
   */
  public void testStdin()
  {
    MemoryCommandExecutionIOStorage ioStorage =
      new MemoryCommandExecutionIOStorage(clock: clock,
                                          maxNumberOfElements: 2,
                                          gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0',
                                                          xtra0: "x0",
                                                          stdin: new ByteArrayInputStream("in0".bytes)])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    assertTrue(ioStorage.completedCommands.isEmpty())
    assertTrue(ioStorage.executingCommands.isEmpty())

    long startTime = clock.currentTimeMillis()

    // make sure that the glu command factory is actually called with the right set of arguments
    assertEqualsIgnoreType([
                             fromCommandFactory: [
                               id: commandId,
                               args: [
                                 id: commandId,
                                 redirectStderr: false,
                                 command: 'c0',
                                 xtra0: 'x0',
                                 stdin: true],
                             ]],
                             ce.command)

    def processing = { CommandStreamStorage storage ->

      assertTrue(ioStorage.completedCommands.isEmpty())
      assertEquals(1, ioStorage.executingCommands.size())
      assertTrue(ioStorage.executingCommands[ce.id].is(ce))

      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->
        assertEquals("in0", stdin.text)

        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->

          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr->
            stdout << "out0"
            stderr << "err0"

            // simulate delay
            clock.addDuration(Timespan.parse("1s"))

            return [exitValue: 14]
          }
        }
      }
    }

    assertEquals(14, ce.syncCaptureIO(processing))

    long completionTime = clock.currentTimeMillis()
    assertEquals(completionTime, startTime + Timespan.parse("1s").durationInMilliseconds)

    assertEquals(1, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce.id].is(ce))
    assertTrue(ioStorage.executingCommands.isEmpty())

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertTrue(ce.hasStdin())

    def checkCommand = { CommandExecution command ->
      assertTrue(command.is(ce))
    }

    // now that the command has completed, we should be able to rebuild it exactly from the
    // filesystem
    checkCommand(ioStorage.findCommandExecution(commandId))

    // not requesting any stream
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [:]) { m ->
      checkCommand(m.commandExecution)
      assertNull(m.stream)
    }

    // no stdin...
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdinStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("in0", m.stream.text)
    }

    // stdout
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdoutStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("out0", m.stream.text)
    }

    // stderr
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stderrStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("err0", m.stream.text)
    }
  }

  /**
   * Test redirect stderr
   */
  public void testRedirectStderr()
  {
    MemoryCommandExecutionIOStorage ioStorage =
      new MemoryCommandExecutionIOStorage(clock: clock,
                                          maxNumberOfElements: 2,
                                          gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0', xtra0: "x0", redirectStderr: true])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    assertTrue(ioStorage.completedCommands.isEmpty())
    assertTrue(ioStorage.executingCommands.isEmpty())

    long startTime = clock.currentTimeMillis()

    // make sure that the glu command factory is actually called with the right set of arguments
    assertEqualsIgnoreType([
                             fromCommandFactory: [
                               id: commandId,
                               args: [
                                 id: commandId,
                                 redirectStderr: true,
                                 command: 'c0',
                                 xtra0: 'x0'],
                             ]],
                             ce.command)


    def processing = { CommandStreamStorage storage ->

      assertTrue(ioStorage.completedCommands.isEmpty())
      assertEquals(1, ioStorage.executingCommands.size())
      assertTrue(ioStorage.executingCommands[ce.id].is(ce))

      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->
        assertNull("no stdin", stdin)

        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->

          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr->
            assertNull("stderr redirected", stderr)

            stdout << "out0"

            // simulate delay
            clock.addDuration(Timespan.parse("1s"))

            return [exitValue: 14]
          }
        }
      }
    }

    assertEquals(14, ce.syncCaptureIO(processing))

    long completionTime = clock.currentTimeMillis()
    assertEquals(completionTime, startTime + Timespan.parse("1s").durationInMilliseconds)

    assertEquals(1, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce.id].is(ce))
    assertTrue(ioStorage.executingCommands.isEmpty())

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertTrue(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())


    def checkCommand = { CommandExecution command ->
      assertTrue(command.is(ce))
    }

    // now that the command has completed, we should be able to rebuild it exactly from the
    // filesystem
    checkCommand(ioStorage.findCommandExecution(commandId))

    // not requesting any stream
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [:]) { m ->
      checkCommand(m.commandExecution)
      assertNull(m.stream)
    }

    // no stdin...
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdinStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("", m.stream.text)
    }

    // stdout
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdoutStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("out0", m.stream.text)
    }

    // stderr
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stderrStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("", m.stream.text)
    }
  }

  /**
   * Test read while executing
   */
  public void testReadWhileExecuting()
  {
    MemoryCommandExecutionIOStorage ioStorage =
      new MemoryCommandExecutionIOStorage(clock: clock,
                                          maxNumberOfElements: 2,
                                          gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0',
                                                          xtra0: "x0",
                                                          stdin: new ByteArrayInputStream("in0".bytes)])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    assertTrue(ioStorage.completedCommands.isEmpty())
    assertTrue(ioStorage.executingCommands.isEmpty())

    long startTime = clock.currentTimeMillis()

    def processing = { CommandStreamStorage storage ->
      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->

        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->

          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr->

            stdout.write("o0".bytes)
            stderr.write("e0".bytes)

            // if we use the api it should flush the output
            ioStorage.withOrWithoutCommandExecutionAndStreams(commandId,
                                                              [stdinStream: true,
                                                                stdoutStream: true,
                                                                stderrStream: true]) { m ->
              def res =
                MultiplexedInputStream.demultiplexToString(m.stream,
                   [StreamType.stdin, StreamType.stdout, StreamType.stderr].collect { it.multiplexName } as Set,
                   null)

              assertEquals("in0", res[StreamType.stdin.multiplexName])
              assertEquals("o0", res[StreamType.stdout.multiplexName])
              assertEquals("e0", res[StreamType.stderr.multiplexName])
            }

            stdout << "o1"
            stderr << "e1"

            assertEquals("in0", stdin.text)

            // simulate delay
            clock.addDuration(Timespan.parse("1s"))

            return [exitValue: 14]
          }
        }
      }
    }

    assertEquals(14, ce.syncCaptureIO(processing))

    long completionTime = clock.currentTimeMillis()
    assertEquals(completionTime, startTime + Timespan.parse("1s").durationInMilliseconds)

    // if we use the api it should flush the output
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId,
                                                      [stdinStream: true,
                                                        stdoutStream: true,
                                                        stderrStream: true]) { m ->
      def res =
        MultiplexedInputStream.demultiplexToString(m.stream,
                   [StreamType.stdin, StreamType.stdout, StreamType.stderr].collect { it.multiplexName } as Set,
                   null)

      assertEquals("in0", res[StreamType.stdin.multiplexName])
      assertEquals("o0o1", res[StreamType.stdout.multiplexName])
      assertEquals("e0e1", res[StreamType.stderr.multiplexName])
    }
  }

  /**
   * Testing that we don't keep more completed commands than expected  */
  public void testMaxNumberElements()
  {
    MemoryCommandExecutionIOStorage ioStorage =
      new MemoryCommandExecutionIOStorage(clock: clock,
                                          maxNumberOfElements: 2,
                                          gluCommandFactory: factory)

    // command 0
    def ce0 = ioStorage.createStorageForCommandExecution([command: 'c0', xtra0: "x0"])

    assertTrue(ioStorage.completedCommands.isEmpty())
    assertTrue(ioStorage.executingCommands.isEmpty())

    def processing = { CommandStreamStorage storage ->
      assertTrue(ioStorage.completedCommands.isEmpty())
      assertEquals(1, ioStorage.executingCommands.size())
      assertTrue(ioStorage.executingCommands[ce0.id].is(ce0))
      return [exitValue: 14]
    }

    assertEquals(14, ce0.syncCaptureIO(processing))

    assertEquals(1, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce0.id].is(ce0))
    assertTrue(ioStorage.executingCommands.isEmpty())

    // command 1
    def ce1 = ioStorage.createStorageForCommandExecution([command: 'c1', xtra0: "x1"])

    assertEquals(1, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce0.id].is(ce0))
    assertTrue(ioStorage.executingCommands.isEmpty())

    processing = { CommandStreamStorage storage ->
      assertEquals(1, ioStorage.completedCommands.size())
      assertTrue(ioStorage.completedCommands[ce0.id].is(ce0))
      assertEquals(1, ioStorage.executingCommands.size())
      assertTrue(ioStorage.executingCommands[ce1.id].is(ce1))
      return [exitValue: 15]
    }

    assertEquals(15, ce1.syncCaptureIO(processing))

    assertEquals(2, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce0.id].is(ce0))
    assertTrue(ioStorage.completedCommands[ce1.id].is(ce1))
    assertTrue(ioStorage.executingCommands.isEmpty())

    // command 2
    def ce2 = ioStorage.createStorageForCommandExecution([command: 'c2', xtra0: "x2"])

    assertEquals(2, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce0.id].is(ce0))
    assertTrue(ioStorage.completedCommands[ce1.id].is(ce1))
    assertTrue(ioStorage.executingCommands.isEmpty())

    processing = { CommandStreamStorage storage ->
      assertEquals(2, ioStorage.completedCommands.size())
      assertTrue(ioStorage.completedCommands[ce0.id].is(ce0))
      assertTrue(ioStorage.completedCommands[ce1.id].is(ce1))
      assertEquals(1, ioStorage.executingCommands.size())
      assertTrue(ioStorage.executingCommands[ce2.id].is(ce2))
      return [exitValue: 16]
    }

    assertEquals(16, ce2.syncCaptureIO(processing))

    // make sure that command 0 has been evicted
    assertEquals(2, ioStorage.completedCommands.size())
    assertTrue(ioStorage.completedCommands[ce1.id].is(ce1))
    assertTrue(ioStorage.completedCommands[ce2.id].is(ce2))
    assertTrue(ioStorage.executingCommands.isEmpty())

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