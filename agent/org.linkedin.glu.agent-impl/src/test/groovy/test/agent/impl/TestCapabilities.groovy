/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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


package test.agent.impl

import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.util.io.resource.Resource
import org.linkedin.groovy.util.net.SingletonURLStreamHandlerFactory
import org.linkedin.groovy.util.ivy.IvyURLHandler
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.glu.agent.api.ShellExecException

/**
 * Test for various capabilities.
 *
 * @author ypujante@linkedin.com
 */
class TestCapabilities extends GroovyTestCase
{
  public static final String MODULE = TestCapabilities.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  void testFetch()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      Resource tempFile = fs.tempFile()
      assertFalse(tempFile.exists())
      fs.saveContent(tempFile, "this is a test")
      assertTrue(tempFile.exists())

      // on the other end if we provide a uri it will 'fetch' it to a different location
      Resource fetchedFile = shell.fetch(tempFile)
      assertNotSame(tempFile.file.canonicalPath, fetchedFile.file.canonicalPath)

      // we make sure that the file was copied entirely
      assertEquals("this is a test", fs.readContent(fetchedFile))

      // we now remove the temp file and we make sure that fetch throws an exception
      fs.rm(tempFile)
      assertFalse(tempFile.exists())
      shouldFail(FileNotFoundException) { shell.fetch(tempFile.toURI()) }
    }
  }

  /**
   * Use a local repo to fetch a file which content is well known.
   */
  void testFetchWithIvy()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->

      def ivySettings = new File("./src/test/resources/ivysettings.xml").canonicalFile.toURI()

      def factory = new SingletonURLStreamHandlerFactory()
      factory.registerHandler('ivy') {
        return new IvyURLHandler(ivySettings)
      }
      URL.setURLStreamHandlerFactory(factory)

      def shell = new ShellImpl(fileSystem: fs)

      def file = shell.fetch('ivy:/test.agent.impl/myArtifact/1.0.0')

      assertEquals(new File("./src/test/resources/test/agent/impl/myArtifact/1.0.0/myArtifact-1.0.0.jar").getText(),
                   fs.readContent(file))

      assertEquals('myArtifact-1.0.0.jar', GroovyNetUtils.guessFilename(new URI('ivy:/test.agent.impl/myArtifact/1.0.0')))

      file = shell.fetch('ivy:/test.agent.impl/myArtifact/1.0.0/text')

      assertEquals(new File("./src/test/resources/test/agent/impl/myArtifact/1.0.0/myArtifact-1.0.0.txt").getText(),
                   fs.readContent(file))
    }
  }

  /**
   * Test that we can exec commands.
   */
  void testExec()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      // non existent script
      try
      {
        shell.exec("/non/existent/666")
        fail("should fail")
      }
      catch(ShellExecException e)
      {
        assertEquals(127, e.res)
        assertEquals('', e.stringOutput)
        String shellScriptError = 'bash: /non/existent/666: No such file or directory'
        assertEquals(shellScriptError, e.stringError)
        assertEquals('Error while executing command /non/existent/666: res=127 - output= - error='
                     + shellScriptError,
                     e.message)
      }

      def shellScript = shell.fetch("./src/test/resources/shellScriptTestCapabilities.sh")

      // we make sure that the script is not executable because exec changes the exec flag
      // automatically
      fs.chmod(shellScript, '-x')

      // we make sure that the other syntax works
      assertEquals("Hello", shell.exec(shellScript))
      assertEquals("Hello a b c", shell.exec(shellScript, "a b c"))
      assertEquals("Hello a b c", shell.exec(shellScript, ["a", "b", "c"]))
      assertEquals("Hello a b c", shell.exec(shellScript, "a", "b", "c"))
      assertEquals("Hello a b c", shell.exec(shellScript.file, ["a", "b", "c"]))

      // and we can call with resource as well
      assertEquals("Hello a b c", shell.exec("${shellScript} a b c"))

      // now we try with a file directly:
      shellScript = shellScript.file.canonicalPath
      assertEquals("Hello a b c", shell.exec("${shellScript} a b c"))
      assertEquals("       1       1       6", shell.exec("${shellScript} | wc"))

      // we make the shell script non executable
      fs.chmod(shellScript, '-x')
      try
      {
        shell.exec("${shellScript} a b c")
        fail("should fail")
      }
      catch(ShellExecException e)
      {
        assertEquals(126, e.res)
        assertEquals('', e.stringOutput)
        String shellScriptError =
          "bash: ${shellScript}: Permission denied".toString()
        assertEquals(shellScriptError, e.stringError)
        assertEquals("Error while executing command ${shellScript} a b c: " +
                     "res=126 - output= - error=${shellScriptError}",
                     e.message)
      }
    }
  }

  void testTail()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      def content = new StringBuilder()

      def f = fs.withOutputStream('testFile') { file, out ->
        (1..1000).each { lineNumber ->
          def line = "this is line ${lineNumber}\n"
          out.write(line.getBytes('UTF-8'))
          content = content << line.toString()
        }
        return file
      }

      assertEquals("""this is line 997
this is line 998
this is line 999
this is line 1000
""", shell.tail(f, 4).text)

      assertEquals(content.toString(), shell.tail(f, -1).text)

      assertNull(shell.tail(location: 'do not exists'))
    }
  }

  /**
   * Test for passing environment variables */
  void testEnv()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def env = [p1: 'v1', p2: 'v2']
      def shell = new ShellImpl(fileSystem: fs, env: env)

      assertEquals('v1', shell.env.p1)
      assertEquals('v2', shell.env['p2'])
      assertNull(shell.env.p3)

      shell = shell.newShell(fs.newFileSystem('/foo'))

      assertEquals('v1', shell.env.p1)
      assertEquals('v2', shell.env['p2'])
      assertNull(shell.env.p3)
    }
  }

  /**
   * Test that untar handles gzip properly
   */
  void testUntar()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      ["./src/test/resources/testUntar_tar", "./src/test/resources/testUntar_tgz"].each { file ->
        def fetchedFile = new File(file).canonicalFile.toURI()

        def untarred = shell.untar(shell.fetch(fetchedFile))

        assertEquals("for ${file}", ['a.txt', 'b.txt', 'c.txt'], shell.ls(untarred).filename.sort())
      }
    }
  }

  /**
   * Test the grep capability
   */
  void testGrep()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      Resource tempFile = fs.tempFile()
      fs.saveContent(tempFile, """line 1 abc
line 2 def
line 3 abcdef
""")

      assertEquals(['line 1 abc', 'line 3 abcdef'], shell.grep(tempFile, /abc/))
      assertEquals(2, shell.grep(tempFile, /abc/, [count: true]))
      assertEquals(['line 1 abc'], shell.grep(tempFile, /abc/, [maxCount: 1]))
      assertEquals(1, shell.grep(tempFile, /abc/, [count: true, maxCount: 1]))

      // test for 'out' option
      def sw = new StringWriter()
      assertEquals(sw, shell.grep(tempFile, /abc/, [out: sw]))
      assertEquals('line 1 abcline 3 abcdef', sw.toString())
    }
  }


  /**
   * Test for listening capability
   */
  void testListening()
  {
    def shell = new ShellImpl()
    assertFalse(shell.listening('localhost', 60000))

    def serverSocket = new ServerSocket(60000)

    def thread = Thread.startDaemon {
      try
      {
        while(true)
        {
          serverSocket.accept { socket ->
            socket.close()
          }
        }
      }
      catch (InterruptedIOException e)
      {
        if(log.isDebugEnabled())
          log.debug("ok because the thread is interrupted... ignored", e)
      }
    }

    assertTrue(shell.listening('localhost', 60000))
    thread.interrupt()
    thread.join(1000)
  }

  void testGzip()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      def root = shell.toResource('/root')

      shell.saveContent('/root/dir1/a.txt', 'this is a test aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')
      shell.saveContent('/root/dir1/b.txt', 'this is a test baaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')
      shell.saveContent('/root/dir1/dir2/c.txt', 'this is a test caaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')
      shell.saveContent('/root/dir1/dir2/d.txt', 'this is a test daaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')
      shell.saveContent('/root/dir1/dir2/e.txt', 'e') // this will generate a negative delta!

      def expectedResult = leavesPaths(root)
      def originalSizes = GroovyCollectionsUtils.toMapKey(expectedResult) { shell.toResource(it).size() }

      assertEquals('/root/dir1/dir2/d.txt.gz', shell.gzip('/root/dir1/dir2/d.txt').path)

      expectedResult << '/root/dir1/dir2/d.txt.gz'
      expectedResult.remove('/root/dir1/dir2/d.txt')

      assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedResult, leavesPaths(root)))
      assertEquals('this is a test daaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', shell.gunzip('/root/dir1/dir2/d.txt.gz', '/gunzip/d.txt').file.text)

      assertEquals('/root/dir1/dir2/d.txt', shell.gunzip('/root/dir1/dir2/d.txt.gz').path)
      expectedResult << '/root/dir1/dir2/d.txt'
      expectedResult.remove('/root/dir1/dir2/d.txt.gz')
      assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedResult, leavesPaths(root)))

      assertEquals('this is a test daaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', shell.readContent('/root/dir1/dir2/d.txt'))

      // not recursive => no changes
      assertEquals([:], shell.gzipFileOrFolder('/root', false))
      assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedResult, leavesPaths(root)))

      def res = shell.gzipFileOrFolder('/root', true)
      expectedResult = ['/root/dir1/a.txt.gz', '/root/dir1/b.txt.gz', '/root/dir1/dir2/c.txt.gz', '/root/dir1/dir2/d.txt.gz', '/root/dir1/dir2/e.txt.gz']
      def sizes = GroovyCollectionsUtils.toMapKey(expectedResult) { shell.toResource(it).size() }

      sizes.each { path, size ->
        assertEquals(originalSizes[path - '.gz'] - size, res[shell.toResource(path)])
      }

      assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedResult, res.keySet().path))
      assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedResult, leavesPaths(root)))
    }
  }

  void testRecurse()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      def root = shell.toResource('/root')
      shell.saveContent('/root/dir1/a.txt', 'this is a test a')
      shell.saveContent('/root/dir1/b.txt', 'this is a test b')
      shell.saveContent('/root/dir1/dir2/c.txt', 'this is a test c')
      shell.saveContent('/root/dir1/dir2/d.txt', 'this is a test d')
      shell.saveContent('/root/e.txt', 'e')

      // every file and dir under root
      def files = []
      fs.eachChildRecurse(root) { r ->
        files << r.path
      }

      def expectedFiles = ['/root/dir1', '/root/e.txt', '/root/dir1/a.txt', '/root/dir1/b.txt', '/root/dir1/dir2', '/root/dir1/dir2/c.txt', '/root/dir1/dir2/d.txt']
      assertEquals(expectedFiles, files)

      // find only dirs under root
      def dirs = fs.findAll('/root') { r ->
        if (r.isDirectory()) {
          return true
        } else {
          return false
        }
      }

      def expectedDirs = ['/root/dir1', '/root/dir1/dir2']
      assertEquals(expectedDirs, dirs.collect {it.path})

      // make everything non-writable
      shell.chmodRecursive(root, "u-w")
      def writableFiles = files.findAll { shell.toResource(it).file.canWrite() }
      assertEquals([], writableFiles)

      // make it writable now
      shell.chmodRecursive(root, "u+w")
      writableFiles = files.findAll { shell.toResource(it).file.canWrite() }
      assertEquals(files, writableFiles)
    }   
  }

  void testReplaceTokens()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      assertEquals('abc foo efg bar hij foo', 
                   shell.replaceTokens('abc @token1@ efg @token2@ hij @token1@',
                                       [token1: 'foo', token2: 'bar']))

      Resource testFile = shell.saveContent('test.txt', 'abc @token1@ efg @token2@ hij @token1@',
                                            [token1: 'foo', token2: 'bar'])

      assertEquals('abc foo efg bar hij foo', shell.fetchContent(testFile))
    }
  }

  private def leavesPaths(Resource root)
  {
    new TreeSet(GroovyIOUtils.findAll(root) { !it.isDirectory() }.collect { it.path })
  }
}
