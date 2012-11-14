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

import org.linkedin.glu.commands.impl.FileSystemCommandExecutionIOStorage
import org.linkedin.util.clock.SettableClock
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.glu.commands.impl.GluCommandFactory
import java.text.SimpleDateFormat
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.glu.commands.impl.CommandStreamStorage
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.util.clock.Timespan
import org.linkedin.util.io.resource.Resource
import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils
import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils
import org.linkedin.glu.groovy.utils.plugins.PluginServiceImpl

/**
 * @author yan@pongasoft.com */
public class TestFileSystemCommandExecutionIOStorage extends GroovyTestCase
{
  FileSystemImpl fs
  SettableClock clock = new SettableClock()
  GluCommandFactory factory = { ce ->
    [fromCommandFactory: [id: ce.id, args: [*:ce.args]]]
  } as GluCommandFactory

  def shutdownSequence = []

  @Override
  protected void setUp()
  {
    super.setUp()

    clock.setCurrentTimeMillis(100000000)

    fs = FileSystemImpl.createTempFileSystem()
    shutdownSequence << { fs.destroy() }
  }

  @Override
  protected void tearDown()
  {
    GluGroovyLangUtils.onlyOneException(shutdownSequence.reverse() << { super.tearDown() })
  }

  /**
   * Basic happy path
   */
  public void testHappyPath()
  {
    FileSystemCommandExecutionIOStorage ioStorage =
      new FileSystemCommandExecutionIOStorage(clock: clock,
                                              commandExecutionFileSystem: fs,
                                              gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0', xtra0: "x0"])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    // check the file stored on the file system
    def path = new SimpleDateFormat('yyyy/MM/dd/HH/z').format(clock.currentDate())
    def commandResource = fs.toResource("${path}/${commandId}/${ioStorage.commandFileName}")
    assertTrue(commandResource.exists())

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

    // make sure that the information is properly stored
    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime],
                           commandResource)

    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    long completionTime = 0L

    def processing = { CommandStreamStorage storage ->
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

    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertEquals("out0", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
    assertEquals("err0", fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").file.text)

    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             exitValue: 14,
                             completionTime: completionTime],
                           commandResource)

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValueIfCompleted)
      assertEquals(startTime, command.startTime)
      assertEquals(completionTime, command.completionTime)
      assertTrue(command.isCompleted())
      assertFalse(command.isRedirectStderr())
      assertFalse(command.hasStdin())

      // command factory should have been called
      assertEqualsIgnoreType([
                               fromCommandFactory: [
                                 id: commandId,
                                 args: [
                                   id: commandId,
                                   redirectStderr: false,
                                   command: 'c0',
                                   xtra0: 'x0'],
                               ]],
                             command.command)
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
      assertEquals("err0", m.stream.text)
    }
  }

  /**
   * Test stdin
   */
  public void testStdin()
  {
    FileSystemCommandExecutionIOStorage ioStorage =
      new FileSystemCommandExecutionIOStorage(clock: clock,
                                              commandExecutionFileSystem: fs,
                                              gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0',
                                                          xtra0: "x0",
                                                          stdin: new ByteArrayInputStream("in0".bytes)])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    // check the file stored on the file system
    def path = new SimpleDateFormat('yyyy/MM/dd/HH/z').format(clock.currentDate())
    def commandResource = fs.toResource("${path}/${commandId}/${ioStorage.commandFileName}")
    assertTrue(commandResource.exists())

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

    // make sure that the information is properly stored
    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             stdin: true],
                           commandResource)

    assertEquals("in0", fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").file.text)
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    def processing = { CommandStreamStorage storage ->
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

    assertEquals("in0", fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").file.text)
    assertEquals("out0", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
    assertEquals("err0", fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").file.text)

    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             exitValue: 14,
                             completionTime: completionTime,
                             stdin: true,
                           ],
                           commandResource)

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertTrue(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValueIfCompleted)
      assertEquals(startTime, command.startTime)
      assertEquals(completionTime, command.completionTime)
      assertTrue(command.isCompleted())
      assertFalse(command.isRedirectStderr())
      assertTrue(command.hasStdin())

      // command factory should have been called
      assertEqualsIgnoreType([
                               fromCommandFactory: [
                                 id: commandId,
                                 args: [
                                   id: commandId,
                                   redirectStderr: false,
                                   command: 'c0',
                                   xtra0: 'x0',
                                   stdin: true,
                                 ],
                               ]],
                             command.command)
    }

    // now that the command has completed, we should be able to rebuild it exactly from the
    // filesystem
    checkCommand(ioStorage.findCommandExecution(commandId))

    // not requesting any stream
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [:]) { m ->
      checkCommand(m.commandExecution)
      assertNull(m.stream)
    }

    // stdin
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
    FileSystemCommandExecutionIOStorage ioStorage =
      new FileSystemCommandExecutionIOStorage(clock: clock,
                                              commandExecutionFileSystem: fs,
                                              gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0', xtra0: "x0", redirectStderr: true])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    // check the file stored on the file system
    def path = new SimpleDateFormat('yyyy/MM/dd/HH/z').format(clock.currentDate())
    def commandResource = fs.toResource("${path}/${commandId}/${ioStorage.commandFileName}")
    assertTrue(commandResource.exists())

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

    // make sure that the information is properly stored
    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: true,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime],
                           commandResource)

    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    def processing = { CommandStreamStorage storage ->
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

    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertEquals("out0", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: true,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             exitValue: 14,
                             completionTime: completionTime],
                           commandResource)

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertTrue(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValueIfCompleted)
      assertEquals(startTime, command.startTime)
      assertEquals(completionTime, command.completionTime)
      assertTrue(command.isCompleted())
      assertTrue(command.isRedirectStderr())
      assertFalse(command.hasStdin())

      // command factory should have been called
      assertEqualsIgnoreType([
                               fromCommandFactory: [
                                 id: commandId,
                                 args: [
                                   id: commandId,
                                   redirectStderr: true,
                                   command: 'c0',
                                   xtra0: 'x0'],
                               ]],
                             command.command)
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
    FileSystemCommandExecutionIOStorage ioStorage =
      new FileSystemCommandExecutionIOStorage(clock: clock,
                                              commandExecutionFileSystem: fs,
                                              gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0',
                                                          xtra0: "x0",
                                                          stdin: new ByteArrayInputStream("in0".bytes)])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    // check the file stored on the file system
    def path = new SimpleDateFormat('yyyy/MM/dd/HH/z').format(clock.currentDate())
    def commandResource = fs.toResource("${path}/${commandId}/${ioStorage.commandFileName}")
    assertTrue(commandResource.exists())

    long startTime = clock.currentTimeMillis()

    assertTrue(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    def processing = { CommandStreamStorage storage ->
      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->

        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->

          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr->

            stdout.write("o0".bytes)
            stderr.write("e0".bytes)

            // due to buffering, there should be anything written yet
            assertEquals("", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
            assertEquals("", fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").file.text)

            // but if we use the api it should flush the output
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

            assertEquals("o0", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
            assertEquals("e0", fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").file.text)


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

    assertEquals("in0", fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").file.text)
    assertEquals("o0o1", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
    assertEquals("e0e1", fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").file.text)
  }

  /**
   * Test exception case
   */
  public void testException()
  {
    FileSystemCommandExecutionIOStorage ioStorage =
      new FileSystemCommandExecutionIOStorage(clock: clock,
                                              commandExecutionFileSystem: fs,
                                              gluCommandFactory: factory)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0',
                                                          xtra0: "x0"])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    // check the file stored on the file system
    def path = new SimpleDateFormat('yyyy/MM/dd/HH/z').format(clock.currentDate())
    def commandResource = fs.toResource("${path}/${commandId}/${ioStorage.commandFileName}")
    assertTrue(commandResource.exists())

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

    // make sure that the information is properly stored
    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime],
                           commandResource)

    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    Exception exception = null

    def processing = { CommandStreamStorage storage ->
      storage.withOrWithoutStorageInput(StreamType.stdin) { stdin ->

        storage.withOrWithoutStorageOutput(StreamType.stdout) { stdout ->

          storage.withOrWithoutStorageOutput(StreamType.stderr) { stderr->
            stdout << "out0"
            stderr << "err0"

            // simulate delay
            clock.addDuration(Timespan.parse("1s"))

            // force an exception
            exception = new Exception("e1")

            throw exception
          }
        }
      }
    }

    def checkExceptionFailure = { Closure c ->
      assertEquals("e1", shouldFail(Exception, c))
    }

    checkExceptionFailure { ce.syncCaptureIO(processing) }

    long completionTime = clock.currentTimeMillis()
    assertEquals(completionTime, startTime + Timespan.parse("1s").durationInMilliseconds)

    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}").exists())
    assertEquals("out0", fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").file.text)
    assertEquals("err0", fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").file.text)

    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             completionTime: completionTime,
                             exception: GluGroovyJsonUtils.exceptionToJSON(exception),
                           ],
                           commandResource)

    checkExceptionFailure { ce.exitValueIfCompleted }
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      checkExceptionFailure { ce.exitValueIfCompleted }
      assertEquals(startTime, command.startTime)
      assertEquals(completionTime, command.completionTime)
      assertTrue(command.isCompleted())
      assertFalse(command.isRedirectStderr())
      assertFalse(command.hasStdin())

      // command factory should have been called
      assertEqualsIgnoreType([
                               fromCommandFactory: [
                                 id: commandId,
                                 args: [
                                   id: commandId,
                                   redirectStderr: false,
                                   command: 'c0',
                                   xtra0: 'x0',
                                 ],
                               ]],
                             command.command)
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
      assertEquals("err0", m.stream.text)
    }
  }

  /**
   * Test with a plugin
   */
  public void testPlugin()
  {
    String password = "abcdefgh"

    def plugins = [
      FileSystemCommandExecutionIOStorage_createInputStream: { args ->
        decryptInputStream(args.resource, password)
      },

      FileSystemCommandExecutionIOStorage_createOutputStream: { args ->
        encryptOutputStream(args.resource, password)
      }
    ]
    PluginServiceImpl pluginService = new PluginServiceImpl()
    pluginService.initializePlugin(plugins, [:])

    FileSystemCommandExecutionIOStorage ioStorage =
      new FileSystemCommandExecutionIOStorage(clock: clock,
                                              commandExecutionFileSystem: fs,
                                              gluCommandFactory: factory,
                                              pluginService: pluginService)

    def ce = ioStorage.createStorageForCommandExecution([command: 'c0',
                                                          xtra0: "x0",
                                                          stdin: new ByteArrayInputStream("in0".bytes)])

    // commandId should start with the current time
    def commandId = ce.id

    assertTrue(commandId.startsWith("${Long.toHexString(clock.currentTimeMillis())}-"))

    // check the file stored on the file system
    def path = new SimpleDateFormat('yyyy/MM/dd/HH/z').format(clock.currentDate())
    def commandResource = fs.toResource("${path}/${commandId}/${ioStorage.commandFileName}")
    assertTrue(commandResource.exists())

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

    // make sure that the information is properly stored
    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             stdin: true],
                           commandResource,
                           password)

    assertEquals("in0", decryptInputStream(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}"), password).text)
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}").exists())
    assertFalse(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}").exists())

    def processing = { CommandStreamStorage storage ->
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

    assertEquals("in0", decryptInputStream(fs.toResource("${path}/${commandId}/${ioStorage.stdinStreamFileName}"), password).text)
    assertEquals("out0", decryptInputStream(fs.toResource("${path}/${commandId}/${ioStorage.stdoutStreamFileName}"), password).text)
    assertEquals("err0", decryptInputStream(fs.toResource("${path}/${commandId}/${ioStorage.stderrStreamFileName}"), password).text)

    assertEqualsIgnoreType([
                             id: commandId,
                             redirectStderr: false,
                             command: 'c0',
                             xtra0: 'x0',
                             startTime: startTime,
                             exitValue: 14,
                             completionTime: completionTime,
                             stdin: true,
                           ],
                           commandResource,
                           password)

    assertEquals(14, ce.exitValueIfCompleted)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertTrue(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValueIfCompleted)
      assertEquals(startTime, command.startTime)
      assertEquals(completionTime, command.completionTime)
      assertTrue(command.isCompleted())
      assertFalse(command.isRedirectStderr())
      assertTrue(command.hasStdin())

      // command factory should have been called
      assertEqualsIgnoreType([
                               fromCommandFactory: [
                                 id: commandId,
                                 args: [
                                   id: commandId,
                                   redirectStderr: false,
                                   command: 'c0',
                                   xtra0: 'x0',
                                   stdin: true,
                                 ],
                               ]],
                             command.command)
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
   * Convenient call to compare and ignore type
   */
  void assertEqualsIgnoreType(Map o1, String s2)
  {
    assertEqualsIgnoreType(o1, JsonUtils.fromJSON(s2))
  }

  /**
   * Convenient call to compare and ignore type
   */
  void assertEqualsIgnoreType(Map o1, Resource r2, String password = null)
  {
    String s2 = (password == null ? r2.file : decryptInputStream(r2, password)).text
    assertEqualsIgnoreType(o1, s2)
  }

  /**
   * Convenient call to compare and ignore type
   */
  void assertEqualsIgnoreType(o1, o2)
  {
    assertEquals(JsonUtils.prettyPrint(o1), JsonUtils.prettyPrint(o2))
    assertTrue("expected <${o1}> but was <${o2}>", GroovyCollectionsUtils.compareIgnoreType(o1, o2))
  }

  /**
   * Decrypts the input stream
   *
   * @return
   */
  private InputStream decryptInputStream(Resource resource, String password)
  {
    GluGroovyIOUtils.decryptStream(password, resource.inputStream)
  }

  /**
   * Encrypts the output stream
   *
   * @return
   */
  private OutputStream encryptOutputStream(Resource resource, String password)
  {
    GluGroovyIOUtils.encryptStream(password, new FileOutputStream(resource.file))
  }
}