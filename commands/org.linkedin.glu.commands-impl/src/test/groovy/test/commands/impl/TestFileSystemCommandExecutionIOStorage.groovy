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
      storage.withStorageInput(StreamType.STDIN) { stdin ->
        assertNull("no stdin", stdin)

        storage.withStorageOutput(StreamType.STDOUT) { stdout ->

          storage.withStorageOutput(StreamType.STDERR) { stderr->
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

    assertEquals(14, ce.exitValue)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValue)
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
      assertNull(m.stream)
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
      storage.withStorageInput(StreamType.STDIN) { stdin ->
        assertEquals("in0", stdin.text)

        storage.withStorageOutput(StreamType.STDOUT) { stdout ->

          storage.withStorageOutput(StreamType.STDERR) { stderr->
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

    assertEquals(14, ce.exitValue)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertFalse(ce.isRedirectStderr())
    assertTrue(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValue)
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
      storage.withStorageInput(StreamType.STDIN) { stdin ->
        assertNull("no stdin", stdin)

        storage.withStorageOutput(StreamType.STDOUT) { stdout ->

          storage.withStorageOutput(StreamType.STDERR) { stderr->
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

    assertEquals(14, ce.exitValue)
    assertEquals(startTime, ce.startTime)
    assertEquals(completionTime, ce.completionTime)
    assertTrue(ce.isCompleted())
    assertTrue(ce.isRedirectStderr())
    assertFalse(ce.hasStdin())

    // after the call there is no more command in the map!
    assertTrue(ioStorage.commands.isEmpty())

    def checkCommand = { CommandExecution command ->
      // all values should have been "restored"
      assertEquals(14, command.exitValue)
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
      assertNull(m.stream)
    }

    // stdout
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stdoutStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertEquals("out0", m.stream.text)
    }

    // stderr
    ioStorage.withOrWithoutCommandExecutionAndStreams(commandId, [stderrStream: true]) { m ->
      checkCommand(m.commandExecution)
      assertNull(m.stream)
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
  void assertEqualsIgnoreType(Map o1, Resource r2)
  {
    assertEqualsIgnoreType(o1, r2.file.text)
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