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


package test.agent.rest.client

import junit.framework.Assert
import org.linkedin.glu.agent.api.DuplicateMountPointException
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.glu.agent.api.ScriptIllegalStateException
import org.linkedin.glu.agent.impl.AgentImpl
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.storage.RAMStorage
import org.linkedin.glu.agent.rest.client.AgentFactoryImpl
import org.linkedin.glu.agent.rest.client.AgentRestClient
import org.linkedin.glu.agent.rest.common.RestServerFactoryImpl
import org.linkedin.glu.agent.rest.resources.AgentResource
import org.linkedin.glu.agent.rest.resources.FileResource
import org.linkedin.glu.agent.rest.resources.LogResource
import org.linkedin.glu.agent.rest.resources.MountPointResource
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.util.clock.SettableClock
import org.linkedin.util.concurrent.ConcurrentUtils
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.io.ram.RAMDirectory
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.io.resource.ResourceInfo
import org.linkedin.util.io.resource.internal.RAMResourceProvider
import org.restlet.routing.Router
import org.restlet.Component
import org.restlet.routing.Template
import org.linkedin.glu.agent.rest.resources.TagsResource
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.glu.agent.impl.storage.TagsStorage
import org.linkedin.glu.agent.impl.storage.Storage
import org.linkedin.glu.agent.api.ScriptExecutionCauseException
import org.linkedin.glu.agent.rest.resources.CommandsResource
import org.linkedin.glu.agent.api.Shell

import org.linkedin.glu.agent.rest.resources.CommandExitValueResource
import org.linkedin.glu.agent.rest.resources.CommandStreamsResource
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import org.linkedin.util.clock.Timespan
import org.linkedin.util.clock.Chronos
import org.linkedin.glu.groovy.utils.test.GluGroovyTestUtils

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

/**
 * The code which is in {@link AgentRestClient} is essentially the code to use for calling the rest
 * api. There is no point in duplicating this code at this stage of the game...
 *
 * @author ypujante@linkedin.com
 */
class TestAgentRestClient extends GroovyTestCase
{
  // must be declared static because the script is created by another class
  static def ThreadControl TC = new ThreadControl()

  public static final String APTN = 'TestAgentRestClient.tags'
  
  Component component
  Router router
  URI serverURI
  AgentImpl agent
  def clock = new SettableClock()

  def ram
  def fileSystem
  def logFileSystem
  def shell
  def ramStorage = [:]
  AgentProperties agentProperties = new AgentProperties('org.linkedin.app.name': 'glu-agent')
  def sync = []

  protected void setUp()
  {
    super.setUp();

    ram = new RAMDirectory()
    RAMResourceProvider rp = new RAMResourceProvider(ram)
    fileSystem = [
      mkdirs: { dir ->
        ram.mkdirhier(dir.toString())
        return rp.createResource(dir.toString())
      },
      rmdirs: { dir ->
        ram.rm(dir.toString())
      },

      getRoot: { rp.createResource('/') },

      getTmpRoot: { rp.createResource('/tmp') },

      newFileSystem: { r,t -> fileSystem }
    ] as FileSystem

    shell = new ShellImpl(fileSystem: fileSystem)

    logFileSystem = FileSystemImpl.createTempFileSystem()

    Storage storage = new RAMStorage(ramStorage, agentProperties)

    agent = new AgentImpl()
    agent.boot(name: 'glu-agent',
               shellForScripts: shell,
               rootShell: new ShellImpl(fileSystem: new FileSystemImpl(new File('/')),
                                        agentProperties: agentProperties),
               agentLogDir: logFileSystem.root,
               storage: storage,
               taggeable: new TagsStorage(storage, APTN),
               sync: { sync << clock.currentTimeMillis() })

    component = new Component();
    def server = new RestServerFactoryImpl().createRestServer(false,
                                                              null,
                                                              0) // port => ephemeral
    component.getServers().add(server)
    def context = component.getContext().createChildContext()
    router = new Router(context)
    context.getAttributes().put('agent', agent)
    component.getDefaultHost().attach(router);
    component.start();

    serverURI = new URI("http://localhost:${server.ephemeralPort}")
  }

  protected void tearDown()
  {
    try
    {
      logFileSystem.destroy()
      component.stop()
    }
    finally
    {
      super.tearDown()
    }
  }

  void testMountPointResource()
  {
    router.attach("/agent", AgentResource)
    router.context.getAttributes().put(AgentResource.class.name, "/agent")

    router.attach("/mountPoint/", MountPointResource).matchingMode = Template.MODE_STARTS_WITH
    router.context.getAttributes().put(MountPointResource.class.name, "/mountPoint")

    AgentFactoryImpl.create(agentPath: "/agent",
                            mountPointPath: "/mountPoint",
                            sslEnabled: false).withRemoteAgent(serverURI) { arc ->

      assertEquals([MountPoint.ROOT], arc.getMountPoints())

      assertEquals([currentState: 'installed'], arc.getState(mountPoint: '/'))

      // non existent mount point
      assertEquals('/a/b', shouldFail(NoSuchMountPointException) {
        arc.getState(mountPoint: '/a/b')
      })

      // install the first script
      Thread th = Thread.start {
        arc.installScript(mountPoint: '/a/b',
                          scriptClassName: MyScriptTestScriptResource.class.name,
                          initParameters: [p1: 'v1'])
      }

      // we verify that the constructor is properly called!
      TC.waitForBlock("MyScriptTestScriptResource()")
      TC.unblock("MyScriptTestScriptResource()")

      ConcurrentUtils.joinFor(clock, th, '5s')

      // we should get the state ok
      assertEquals([currentState: 'NONE'], arc.getState(mountPoint: '/a/b'))

      def fullState = arc.getFullState(mountPoint: '/a/b')

      def expectedFullState = [
        scriptDefinition:[
          mountPoint: '/a/b',
          parent: '/',
          scriptFactory: [
            'class': 'org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory',
            className: 'test.agent.rest.client.MyScriptTestScriptResource'],
          initParameters: [p1: 'v1']
        ],
        scriptState:[stateMachine:[currentState:'NONE'], script:[:]]
      ]

      assertEquals(expectedFullState, fullState)

      // already installed mountPoint
      assertEquals('/a/b', shouldFail(DuplicateMountPointException) {
        arc.installScript(mountPoint: '/a/b',
                          scriptClassName: MyScriptTestScriptResource.class.name)
      })

      // no such mount point
      assertEquals('/a/b/c', shouldFail(NoSuchMountPointException) {
                   arc.executeAction(mountPoint: '/a/b/c',
                                     action: 'install',
                                     actionArgs: [p2: 'v2'])
                   })

      // non blocking call!
      arc.executeAction(mountPoint: '/a/b',
                        action: 'install',
                        actionArgs: [p2: 'v2'])

      TC.waitForBlock("MyScriptTestScriptResource.install")
      assertEquals([currentState: 'NONE', transitionState: 'NONE->installed', transitionAction: 'install'],
                   arc.getState(mountPoint: '/a/b'))
      TC.unblock("MyScriptTestScriptResource.install")

      // wait for state should work
      assertTrue(arc.waitForState(mountPoint: '/a/b', state: 'installed', timeout: '5s'))
      assertEquals([currentState: 'installed'], arc.getState(mountPoint: '/a/b'))

      // should timeout (and return false)
      assertFalse(arc.waitForState(mountPoint: '/a/b', state: 'started', timeout: 200))

      // no such mount point
      assertEquals('/a/b/c', shouldFail(NoSuchMountPointException) {
                   arc.waitForState(mountPoint: '/a/b/c', state: 'installed', timeout: '5s')
                   })

      arc.executeAction(mountPoint: '/a/b',
                        action: 'configure',
                        actionArgs: [c: 'd'])

      TC.waitForBlock("MyScriptTestScriptResource.configure")
      assertEquals([currentState: 'installed', transitionState: 'installed->stopped', transitionAction: 'configure'],
                   arc.getState(mountPoint: '/a/b'))
      TC.unblock("MyScriptTestScriptResource.configure", new Exception('mine'))

      // did not reach started state due to exception
      assertEquals("script=test.agent.rest.client.MyScriptTestScriptResource [/a/b], action=configure",
                   shouldFail(ScriptExecutionException) {
        arc.waitForState(mountPoint: '/a/b', state: 'started', timeout: 200)
      })

      def state = arc.getState(mountPoint: '/a/b')
      assertEquals(2, state.size())
      assertEquals('installed', state.currentState)
      assertTrue(state.error instanceof ScriptExecutionException)
      assertEquals('script=test.agent.rest.client.MyScriptTestScriptResource [/a/b], action=configure', state.error.message)

      // cannot uninstall because in error
      shouldFail(ScriptIllegalStateException) {arc.executeAction(mountPoint: '/a/b', action: 'uninstall')}

      // verifying that it had no effect
      assertEquals("script=test.agent.rest.client.MyScriptTestScriptResource [/a/b], action=configure",
                   shouldFail(ScriptExecutionException) {
        arc.waitForState(mountPoint: '/a/b', state: 'started', timeout: 200)
      })

      try
      {
        arc.waitForState(mountPoint: '/a/b', state: 'started', timeout: 200)
        fail('should have thrown exception')
      }
      catch(ScriptExecutionException e)
      {
        assertTrue(e.cause.getClass() == ScriptExecutionCauseException)
        assertEquals('java.lang.Exception', e.cause.originalClassname)
        assertEquals('[java.lang.Exception]: mine', e.cause.message)
      }

      // we now clear the error
      arc.clearError(mountPoint: '/a/b')

      // error should have been cleared
      assertEquals([currentState: 'installed'], arc.getState(mountPoint: '/a/b'))

      // cannot uninstall a script in 'installed' state
      assertEquals("cannot unsinstall script at /a/b: state is installed", shouldFail(ScriptIllegalStateException) {
        arc.uninstallScript(mountPoint: '/a/b')
      })

      // we configure again without exception this time
      def actionId = arc.executeAction(mountPoint: '/a/b',
                                       action: 'configure',
                                       actionArgs: [c: 'd'])

      TC.waitForBlock("MyScriptTestScriptResource.configure")
      assertEquals([currentState: 'installed', transitionState: 'installed->stopped', transitionAction: 'configure'],
                   arc.getState(mountPoint: '/a/b'))
      TC.unblock("MyScriptTestScriptResource.configure")

      arc.waitForAction(mountPoint: '/a/b', actionId: actionId, timeout: 200)

      // test the streaming api
      def inputStream = null
      // synchronous through executeCall
      th = Thread.start {
        inputStream = arc.executeCall(mountPoint: '/a/b',
                                      call: 'getStream',
                                      callArgs: [p2: 'c2'])

        TC.block("Thread.executeCall")
      }

      // we ensure that the getStream closure is called
      TC.unblock("MyScriptTestScriptResource.getStream")

      // we ensure that the inputStream variable has been set
      TC.unblock("Thread.executeCall")

      assertTrue(inputStream instanceof InputStream)
      assertEquals('this is a test v1/c2', inputStream.text)

      // asynchronous thourgh executeAction
      // now we should be able to run the start action
      th = Thread.start {
        inputStream = arc.executeActionAndWait(mountPoint: '/a/b',
                                               action: 'start',
                                               actionArgs: [p2: 'c2'],
                                               timeout: 200)

        TC.block("Thread.executeActionAndWait")
      }

      // we ensure that the getStream closure is called
      TC.unblock("MyScriptTestScriptResource.start")

      // we ensure that the inputStream variable has been set
      TC.unblock("Thread.executeActionAndWait")

      assertTrue(inputStream instanceof InputStream)
      assertEquals('this is a test v1/c2', inputStream.text)

      // stop
      actionId = arc.executeAction(mountPoint: '/a/b', action: 'stop')

      // wait for the action to complete
      arc.waitForAction(mountPoint: '/a/b', actionId: actionId, timeout: 200)

      // unconfigure
      actionId = arc.executeAction(mountPoint: '/a/b', action: 'unconfigure')

      // wait for the action to complete
      arc.waitForAction(mountPoint: '/a/b', actionId: actionId, timeout: 200)

      // uninstall
      arc.executeAction(mountPoint: '/a/b', action: 'uninstall')

      arc.waitForState(mountPoint: '/a/b', state: 'NONE', timeout: 200)

      // script is in uninstall state
      assertEquals([currentState: 'NONE'], arc.getState(mountPoint: '/a/b'))

      // non blocking call!
      arc.executeAction(mountPoint: '/a/b',
                        action: 'install',
                        actionArgs: [p2: 'v2'])

      TC.waitForBlock("MyScriptTestScriptResource.install")
      assertEquals([currentState: 'NONE', transitionState: 'NONE->installed', transitionAction: 'install'],
                   arc.getState(mountPoint: '/a/b'))

      // the action is in a blocking state... we should be able to interrupt it!
      assertFalse arc.interruptAction(mountPoint: '/a/b',
                                     action: 'foobar')

      assertFalse arc.interruptAction(mountPoint: '/foobar',
                                     action: 'install')

      assertTrue arc.interruptAction(mountPoint: '/a/b',
                                     action: 'install')

      try
      {
        arc.waitForState(mountPoint: '/a/b', state: 'installed', timeout: 200)
        fail('should have thrown exception')
      }
      catch(ScriptExecutionException e)
      {
        // ThreadControl wraps the exception in a RuntimeException
        assertTrue(e.cause instanceof ScriptExecutionCauseException)
        assertEquals('java.lang.RuntimeException', e.cause.originalClassname)

        assertTrue(e.cause.cause instanceof ScriptExecutionCauseException)
        assertEquals('java.lang.InterruptedException', e.cause.cause.originalClassname)
      }

      // now uninstall works
      arc.uninstallScript(mountPoint: '/a/b')

      // non existent mount point anymore
      assertEquals('/a/b', shouldFail(NoSuchMountPointException) {
        arc.getState(mountPoint: '/a/b')
      })

      assertEquals([], sync)

      arc.sync()

      assertEquals([clock.currentTimeMillis()], sync)


      // testing force: true in uninstall

      // reinstall the first script
      th = Thread.start {
        arc.installScript(mountPoint: '/a/b',
                          scriptClassName: MyScriptTestScriptResource.class.name,
                          initParameters: [p1: 'v1'])
      }

      // we verify that the constructor is properly called!
      TC.waitForBlock("MyScriptTestScriptResource()")
      TC.unblock("MyScriptTestScriptResource()")

      ConcurrentUtils.joinFor(clock, th, '5s')

      // non blocking call!
      arc.executeAction(mountPoint: '/a/b',
                        action: 'install',
                        actionArgs: [p2: 'v2'])

      TC.waitForBlock("MyScriptTestScriptResource.install")
      assertEquals([currentState: 'NONE', transitionState: 'NONE->installed', transitionAction: 'install'],
                   arc.getState(mountPoint: '/a/b'))
      TC.unblock("MyScriptTestScriptResource.install")

      // wait for state should work
      assertTrue(arc.waitForState(mountPoint: '/a/b', state: 'installed', timeout: '5s'))
      assertEquals([currentState: 'installed'], arc.getState(mountPoint: '/a/b'))

      // cannot uninstall a script in 'installed' state
      assertEquals("cannot unsinstall script at /a/b: state is installed",
                   shouldFail(ScriptIllegalStateException) {
        arc.uninstallScript(mountPoint: '/a/b')
      })

      // forcing uninstall should work
      arc.uninstallScript(mountPoint: '/a/b', force: true)
    }
  }

  void testAgentLog()
  {
    router.attach("/log/", LogResource).matchingMode = Template.MODE_STARTS_WITH
    router.context.getAttributes().put(LogResource.class.name, "/log")

    AgentFactoryImpl.create(logPath: "/log",
                            sslEnabled: false).withRemoteAgent(serverURI) { arc ->

      assertNull("does not exist yet", arc.tailAgentLog([maxLine: 4]))
      
      def agentLog = logFileSystem.withOutputStream('glu-agent.out') { file, out ->
        (1..1000).each { lineNumber ->
          out.write("gal: ${lineNumber}\n".getBytes('UTF-8'))
        }
        return file
      }

      assertEquals("""gal: 997
gal: 998
gal: 999
gal: 1000
""", arc.tailAgentLog([maxLine: 4]).text)

      def gcLog = logFileSystem.withOutputStream('gc.log') { file, out ->
        (1..1000).each { lineNumber ->
          out.write("gc: ${lineNumber}\n".getBytes('UTF-8'))
        }
        return file
      }

      assertEquals("""gc: 999
gc: 1000
""", arc.tailAgentLog([log: 'gc.log', maxLine: 2]).text)
    }
  }

  void testGetFileContent()
  {
    router.attach("/file/", FileResource).matchingMode = Template.MODE_STARTS_WITH
    router.context.getAttributes().put(FileResource.class.name, "/file")

    AgentFactoryImpl.create(filePath: "/file",
                            sslEnabled: false).withRemoteAgent(serverURI) { arc ->

      def gal = logFileSystem.toResource('glu-agent.out').file

      assertNull(arc.getFileContent([location: gal.canonicalPath, maxLine: 4]))

      def agentLog = logFileSystem.withOutputStream('glu-agent.out') { file, out ->
        (1..1000).each { lineNumber ->
          out.write("gal: ${lineNumber}\n".getBytes('UTF-8'))
        }
        return file
      }

      assertEquals("""gal: 997
gal: 998
gal: 999
gal: 1000
""", arc.getFileContent([location: gal.canonicalPath, maxLine: 4]).text)

      def gcLog = logFileSystem.withOutputStream('gc.log') { file, out ->
        (1..1000).each { lineNumber ->
          out.write("gc: ${lineNumber}\n".getBytes('UTF-8'))
        }
        return file
      }

      assertEquals("""gc: 999
gc: 1000
""", arc.getFileContent([location: gcLog.canonicalPath, maxLine: 2]).text)

      // adding a directory
      def abc = logFileSystem.mkdirs('ab c').file

      def directory = gcLog.parentFile
      def ls = arc.getFileContent([location: directory.canonicalPath])
      assertEquals(4, ls.size())

      [gal, gcLog, abc, directory].each { file ->
        def details = file == directory ? ls['.'] : ls[file.name]
        assertEquals(file.canonicalPath, details.canonicalPath)
        assertEquals(file.length(), details.length)
        checkTimeDifference(file.lastModified(), details.lastModified)
        assertEquals(file.isDirectory(), details.isDirectory)
      }

      // testing offset capability
      agent.rootShell.withTempFile { Resource tempResource ->

        assertNull(arc.getFileContent(location: '/do/not/exists', offset: 0))

        def tempFile = tempResource.file.canonicalPath

        agent.rootShell.saveContent(tempResource, "0123456789")

        BasicFileAttributes attributes =
          Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        def res = arc.getFileContent(location: tempFile, offset: 0)
        assertEquals("0123456789", res.tailStream.text)
        assertEquals(10, res.length)
        assertEquals(10, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: -3)
        assertEquals("789", res.tailStream.text)
        assertEquals(10, res.length)
        assertEquals(3, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: 10)
        assertEquals("", res.tailStream.text)
        assertEquals(10, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        agent.rootShell.saveContent(tempResource, "012345678901")

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: 10)
        assertEquals("01", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(2, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: 12)
        assertEquals("", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: -25)
        assertEquals("012345678901", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(12, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: 34)
        assertEquals("", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        // symbolic link
        agent.rootShell.withTempFile { Resource t2 ->
          Files.createSymbolicLink(t2.file.toPath(), tempResource.file.toPath())

          attributes = Files.readAttributes(t2.file.toPath(), BasicFileAttributes)
          res = arc.getFileContent(location: t2.file.path, offset: 10)
          assertEquals("01", res.tailStream.text)
          assertEquals(12, res.length)
          assertEquals(2, res.tailStreamMaxLength)
          checkTimeDifference(attributes.creationTime().toMillis(), res.created)
          checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
          checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
          assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
          assertTrue(res.isSymbolicLink)
        }

        // empty file
        agent.rootShell.saveContent(tempResource, "")

        attributes = Files.readAttributes(tempResource.file.toPath(), BasicFileAttributes)
        res = arc.getFileContent(location: tempFile, offset: 10)
        assertEquals("", res.tailStream.text)
        assertEquals(0, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(attributes.creationTime().toMillis(), res.created)
        checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
        checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
        assertEquals(tempResource.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        agent.rootShell.withTempFile { Resource dir ->

          // ${abc} url encoded is %24%7Babc%7D
          Resource content = agent.rootShell.saveContent(dir.createRelative('%24%7Babc%7D'), "0123456789")

          attributes =
            Files.readAttributes(content.file.toPath(), BasicFileAttributes)
          res = arc.getFileContent(location: content.toURI().rawPath, offset: 0)
          assertEquals("0123456789", res.tailStream.text)
          assertEquals(10, res.length)
          assertEquals(10, res.tailStreamMaxLength)
          checkTimeDifference(attributes.creationTime().toMillis(), res.created)
          checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
          checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
          assertEquals(content.file.canonicalPath, res.canonicalPath)
          assertFalse(res.isSymbolicLink)
          assertTrue(res.canonicalPath.endsWith('/${abc}'))

        }

        agent.rootShell.withTempFile { Resource dir ->

          // abc def
          Resource content = agent.rootShell.saveContent(dir.createRelative('abc%20def'), "0123456789")

          attributes =
            Files.readAttributes(content.file.toPath(), BasicFileAttributes)
          res = arc.getFileContent(location: content.toURI().rawPath, offset: 0)
          assertEquals("0123456789", res.tailStream.text)
          assertEquals(10, res.length)
          assertEquals(10, res.tailStreamMaxLength)
          checkTimeDifference(attributes.creationTime().toMillis(), res.created)
          checkTimeDifference(attributes.lastModifiedTime().toMillis(), res.lastModified)
          checkTimeDifference(attributes.lastAccessTime().toMillis(), res.lastAccessed)
          assertEquals(content.file.canonicalPath, res.canonicalPath)
          assertFalse(res.isSymbolicLink)
          assertTrue(res.canonicalPath.endsWith('/abc def'))
        }
      }
    }
  }

  /**
   * It seems that timing can vary by up to 1 second... due to os precision...
   */
  public void checkTimeDifference(long time1, long time2)
  {
    GluGroovyTestUtils.checkTimeDifference(this, time1, time2)
  }

  /**
   * Test for the tags api
   */
  void testTags()
  {
    router.attach("/tags", TagsResource).matchingMode = Template.MODE_STARTS_WITH
    router.context.getAttributes().put(TagsResource.class.name, "/tags")

    AgentFactoryImpl.create(tagsPath: "/tags",
                            sslEnabled: false).withRemoteAgent(serverURI) { Agent arc ->

      // empty
      assertTrue(arc.hasTags())
      assertEquals(0, arc.tagsCount)
      assertFalse(arc.hasTag('fruit'))
      assertFalse(arc.hasTag('vegetable'))
      assertFalse(arc.hasTag('rock'))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable']))
      assertFalse(arc.hasAnyTag(['fruit', 'vegetable']))
      assertNull(agentProperties[APTN])

      // + fruit
      assertTrue(arc.addTag('fruit'))
      assertFalse(arc.hasTags())
      assertEquals(1, arc.tagsCount)
      assertTrue(arc.hasTag('fruit'))
      assertFalse(arc.hasTag('vegetable'))
      assertFalse(arc.hasTag('rock'))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable']))
      assertTrue(arc.hasAnyTag(['fruit', 'vegetable']))
      assertEquals('fruit', agentProperties[APTN])

      // adding fruit again should not have any impact
      assertFalse(arc.addTag('fruit'))
      assertFalse(arc.hasTags())
      assertEquals(1, arc.tagsCount)
      assertTrue(arc.hasTag('fruit'))
      assertFalse(arc.hasTag('vegetable'))
      assertFalse(arc.hasTag('rock'))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable']))
      assertTrue(arc.hasAnyTag(['fruit', 'vegetable']))
      assertEquals('fruit', agentProperties[APTN])

      // + vegetable
      assertEquals(['fruit'] as Set, arc.addTags(['vegetable', 'fruit']))
      assertFalse(arc.hasTags())
      assertEquals(2, arc.tagsCount)
      assertTrue(arc.hasTag('fruit'))
      assertTrue(arc.hasTag('vegetable'))
      assertFalse(arc.hasTag('rock'))
      assertTrue(arc.hasAllTags(['fruit', 'vegetable']))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable', 'rock']))
      assertTrue(arc.hasAnyTag(['fruit', 'vegetable']))
      assertEquals('fruit;vegetable', agentProperties[APTN])

      // - fruit
      assertEquals(['rock'] as Set, arc.removeTags(['fruit', 'rock']))
      assertFalse(arc.hasTags())
      assertEquals(1, arc.tagsCount)
      assertFalse(arc.hasTag('fruit'))
      assertTrue(arc.hasTag('vegetable'))
      assertFalse(arc.hasTag('rock'))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable']))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable', 'rock']))
      assertTrue(arc.hasAnyTag(['fruit', 'vegetable']))
      assertEquals('vegetable', agentProperties[APTN])

      // set to rock & paper
      arc.setTags(['rock', 'paper'])
      assertFalse(arc.hasTags())
      assertEquals(2, arc.tagsCount)
      assertFalse(arc.hasTag('fruit'))
      assertFalse(arc.hasTag('vegetable'))
      assertTrue(arc.hasTag('rock'))
      assertTrue(arc.hasTag('paper'))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable']))
      assertFalse(arc.hasAllTags(['fruit', 'vegetable', 'rock']))
      assertTrue(arc.hasAnyTag(['rock', 'vegetable']))
      assertTrue(agentProperties[APTN] == 'rock;paper' || agentProperties[APTN] == 'paper;rock')
    }
  }

  /**
   * The content of this test is very similar to TestCapabilities.testGenericExec (minus the test
   * for arguments that don't make sense)
   */
  public void testCommands()
  {
    router.attach("/commands", CommandsResource)
    router.context.getAttributes().put(CommandsResource.class.name, "/commands")
    router.attach("/command/{id}/streams", CommandStreamsResource)
    router.context.getAttributes().put(CommandStreamsResource.class.name, "/command")
    router.attach("/command/{id}/exitValue", CommandExitValueResource)
    router.context.getAttributes().put(CommandExitValueResource.class.name, "/command")
    router.attach("/mountPoint/", MountPointResource).matchingMode = Template.MODE_STARTS_WITH
    router.context.getAttributes().put(MountPointResource.class.name, "/mountPoint")

    AgentFactoryImpl.create(commandsPath: "/commands",
                            sslEnabled: false).withRemoteAgent(serverURI) { Agent arc ->
      FileSystemImpl.createTempFileSystem() { FileSystem fs ->
        def shell = new ShellImpl(fileSystem: fs)

        // setting a shell for commands
        router.context.getAttributes().put('shellForCommands', shell)

        def shellScript = shell.fetch("./src/test/resources/shellScriptTestAgentExecShellCommand.sh")
        // let's make sure it is executable
        fs.chmod(shellScript, '+x')

        // stdout only
        checkShellExec(arc, shell, [command: [shellScript, "-1"]], 0, "this goes to stdout\n", "")

        // both stdout and stderr in their proper channel
        checkShellExec(arc, shell, [command: [shellScript, "-1", "-2"]], 0, "this goes to stdout\n", "this goes to stderr\n")

        // redirecting stderr to stdout
        checkShellExec(arc, shell, [command: [shellScript, "-1", "-2"], redirectStderr: true], 0, "this goes to stdout\nthis goes to stderr\n", "")

        // testing for failure/exit value
        checkShellExec(arc, shell, [command: [shellScript, "-1", "-e"]], 1, "this goes to stdout\n", "")

        // reading from stdin
        checkShellExec(arc, shell, [command: [shellScript, "-1", "-c"], stdin: "abc\ndef\n"], 0, "this goes to stdout\nabc\ndef\n", "")

        Chronos c = new Chronos()

        // testing interrupt 1 (interrupting before reading)
        checkShellExec(arc, shell, [command: ["sleep 10"]], null, "", "") { execResult ->

          // we wait until the command is actually started...
          shell.waitFor(timeout: '2s', heartbeat: '10') {
            agent._commandManager.findCommand(execResult.id).command.exitValueStream != null
          }
          assertTrue(arc.interruptCommand([id: execResult.id]))
          return execResult
        }

        // we make sure that the command got interrupted properly and that it did not last the
        // full 10s. We use 2s as a buffer...
        assertTrue(c.tick() < Timespan.parse("2s").durationInMilliseconds)

        FutureTaskExecution future = null

        // testing interrupt 2 (interrupting after reading)
        checkShellExec(arc, shell, [command: ["sleep 10"]], null, "", "") { execResult ->
          def async = {
            // the issue is that reading the stream is blocking (issue with restlet!)... so we do
            // it in a separate thread (the sleep is to make sure that we are "reading" when we
            // interrupt)
            Thread.sleep(Timespan.parse("1s").durationInMilliseconds)

            def expectedState = [
              scriptDefinition: [
                mountPoint: "/_/command/${execResult.id}",
                parent: '/',
                scriptFactory: [
                  'class': 'CommandGluScriptFactory'
                ],
                initParameters: [:],
              ],
              scriptState: [
                script: [:],
                stateMachine: [
                  currentState: 'stopped',
                  transitionAction: 'start',
                  transitionState: 'stopped->running'
                ]
              ]
            ]
            // we check that a glu script is running
            def fullState = arc.getFullState(mountPoint: "/_/command/${execResult.id}")

            GluGroovyTestUtils.assertEqualsIgnoreType(this,
                                                      "command glu script running not running",
                                                      expectedState,
                                                      fullState)

            // we interrupt it
            arc.interruptCommand([id: execResult.id])
          }
          future = new FutureTaskExecution(async)
          Thread.start { future.runSync() }
          return execResult
        }

        assertTrue(future.get())

        // we make sure that the command got interrupted properly and that it did not last the
        // full 10s. We use 2s as a buffer...
        assertTrue(c.tick() < Timespan.parse("2s").durationInMilliseconds)
      }
    }
  }

  private void checkShellExec(Agent agent,
                              Shell shell,
                              commands,
                              exitValue,
                              stdout,
                              stderr,
                              Closure afterExecuteShellCommand = null)
  {
    commands.command = commands.command.collect { it.toString() }.join(" ")
    if(commands.stdin)
      commands.stdin = new ByteArrayInputStream(commands.stdin.getBytes("UTF-8"))

    def execResult = agent.executeShellCommand(*: commands)

    if(afterExecuteShellCommand)
      execResult = afterExecuteShellCommand(execResult)

    def commandId = execResult.id

    def streamResults = agent.streamCommandResults(id: commandId,
                                                   exitValueStream: true,
                                                   exitValueStreamTimeout: 0,
                                                   exitErrorStream: true,
                                                   stdoutStream: true,
                                                   stderrStream: true)

    def stream = streamResults.stream

    if(!stream)
    {
      shouldFail { agent.waitForCommand(id: commandId) }
      agent.waitForCommand(id: commandId)
    }
    else
    {
      String text = stream.text

      stream = new ByteArrayInputStream(text.getBytes("UTF-8"))

      OutputStream stdoutStream = new ByteArrayOutputStream()

      OutputStream stderrStream = new ByteArrayOutputStream()

      try
      {
        try
        {
          assertEquals(exitValue, shell.demultiplexExecStream(stream, stdoutStream, stderrStream))
        }
        finally
        {
          assertEquals(stdout, new String(stdoutStream.toByteArray(), "UTF-8"))
          assertEquals(stderr, new String(stderrStream.toByteArray(), "UTF-8"))
        }

        assertEquals(exitValue, agent.waitForCommand(id: commandId))
      }
      catch(Throwable th)
      {
        System.err.println("Issue with stream?")
        System.err.println("<=================")
        System.err.println(text)
        System.err.println("=================>")
        throw th
      }
    }
  }
}

class MyScriptTestScriptResource
{
  MyScriptTestScriptResource()
  {
    TestAgentRestClient.TC.block("MyScriptTestScriptResource()")
  }

  def install = { args ->
    TestAgentRestClient.TC.block("MyScriptTestScriptResource.install")
    Assert.assertEquals('v1', params.p1)
    Assert.assertEquals('v2', args.p2)
  }

  def configure = {
    TestAgentRestClient.TC.blockWithException("MyScriptTestScriptResource.configure")
  }

  def start = { args ->
    TestAgentRestClient.TC.block("MyScriptTestScriptResource.start")
    return new ByteArrayInputStream("this is a test ${params.p1}/${args.p2}".getBytes())
  }

  def getStream = { args ->
    TestAgentRestClient.TC.block("MyScriptTestScriptResource.getStream")
    return new ByteArrayInputStream("this is a test ${params.p1}/${args.p2}".getBytes())
  }
}
