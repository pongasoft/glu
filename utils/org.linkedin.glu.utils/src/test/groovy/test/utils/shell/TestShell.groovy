/*
 * Copyright (c) 2013 Yan Pujante
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
package test.utils.shell

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellExec
import org.linkedin.glu.groovy.utils.shell.ShellExecException
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.glu.groovy.utils.test.GluGroovyTestUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.ivy.IvyURLHandler
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.groovy.util.net.SingletonURLStreamHandlerFactory
import org.linkedin.util.io.resource.Resource

import java.nio.file.Files

/**
 * @author yan@pongasoft.com  */
public class TestShell extends GroovyTestCase
{
  void testFetch()
  {
    ShellImpl.createTempShell { Shell shell ->

      Resource tempFile = shell.tempFile()
      assertFalse(tempFile.exists())
      shell.saveContent(tempFile, "this is a test")
      assertTrue(tempFile.exists())

      // on the other end if we provide a uri it will 'fetch' it to a different location
      Resource fetchedFile = shell.fetch(tempFile)
      assertNotSame(tempFile.file.canonicalPath, fetchedFile.file.canonicalPath)

      // we make sure that the file was copied entirely
      assertEquals("this is a test", shell.readContent(fetchedFile))

      // we now remove the temp file and we make sure that fetch throws an exception
      shell.rm(tempFile)
      assertFalse(tempFile.exists())
      shouldFail(FileNotFoundException) { shell.fetch(tempFile.toURI()) }
    }
  }

  /**
   * Using shell.fetch on a remote url (http)
   */
  void testFetchRemote()
  {
    ShellImpl.createTempShell { Shell shell ->

      String response
      Headers requestHeaders

      def handler = { HttpExchange t ->
        requestHeaders = t.requestHeaders
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
      }

      GroovyNetUtils.withHttpServer(0, ['/content': handler]) { int port ->

        File root = shell.root.file

        File tmpFile = new File(root, 'foo.txt')

        response = "abc"

        shell.fetch("http://localhost:${port}/content", tmpFile)
        assertEquals("abc", tmpFile.text)
        // no authorization header should be present!
        assertFalse(requestHeaders.containsKey('Authorization'))

        response = "def"
        shell.fetch("http://u1:p1@localhost:${port}/content", tmpFile)
        assertEquals("def", tmpFile.text)
        // authorization header should be present and properly base64ified
        assertEquals("Basic ${'u1:p1'.bytes.encodeBase64()}",
                     requestHeaders['Authorization'].iterator().next())
      }
    }
  }

  /**
   * Use a local repo to fetch a file which content is well known.
   */
  void testFetchWithIvy()
  {
    ShellImpl.createTempShell { Shell shell ->

      def ivySettings = new File("./src/test/resources/ivysettings.xml").canonicalFile.toURI()

      def factory = new SingletonURLStreamHandlerFactory()
      factory.registerHandler('ivy') {
        return new IvyURLHandler(ivySettings)
      }
      URL.setURLStreamHandlerFactory(factory)

      def file = shell.fetch('ivy:/test.agent.impl/myArtifact/1.0.0')

      assertEquals(new File("./src/test/resources/test/agent/impl/myArtifact/1.0.0/myArtifact-1.0.0.jar").getText(),
                   shell.readContent(file))

      assertEquals('myArtifact-1.0.0.jar', GroovyNetUtils.guessFilename(new URI('ivy:/test.agent.impl/myArtifact/1.0.0')))

      file = shell.fetch('ivy:/test.agent.impl/myArtifact/1.0.0/text')

      assertEquals(new File("./src/test/resources/test/agent/impl/myArtifact/1.0.0/myArtifact-1.0.0.txt").getText(),
                   shell.readContent(file))
    }
  }

  /**
   * Test that we can exec commands.
   */
  void testExec()
  {
    ShellImpl.createTempShell { Shell shell ->

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
      shell.chmod(shellScript, '-x')

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
      assertEquals("       1       1       6".trim(), shell.exec("${shellScript} | wc").trim())

      // we make the shell script non executable
      shell.chmod(shellScript, '-x')
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

  void testGenericExec()
  {
    ShellImpl.createTempShell { Shell shell ->
      def shellScript = shell.fetch("./src/test/resources/shellScriptTestShellExec.sh")
      // let's make sure it is executable
      shell.chmod(shellScript, '+x')

      // stdout only
      checkShellExec(shell, [command: [shellScript, "-1"]], 0, "this goes to stdout\n", "")

      // both stdout and stderr in their proper channel
      checkShellExec(shell, [command: [shellScript, "-1", "-2"]], 0, "this goes to stdout\n", "this goes to stderr\n")

      // redirecting stderr to stdout
      checkShellExec(shell, [command: [shellScript, "-1", "-2"], redirectStderr: true], 0, "this goes to stdout\nthis goes to stderr\n", "")

      // changing stdout
      def myStdout = new ByteArrayOutputStream()
      checkShellExec(shell, [command: [shellScript, "-1", "-2"], stdout: myStdout], 0, "", "this goes to stderr\n") {
        // implementation note: output here is not "processed" (see javadoc) so need to add the
        // final \n character
        assertEquals("this goes to stdout\n", new String(myStdout.toByteArray(), "UTF-8"))
        myStdout.reset()
      }

      // changing stderr
      def myStderr = new ByteArrayOutputStream()
      checkShellExec(shell, [command: [shellScript, "-1", "-2"], stderr: myStderr], 0, "this goes to stdout\n", "") {
        // implementation note: output here is not "processed" (see javadoc) so need to add the
        // final \n character
        assertEquals("this goes to stderr\n", new String(myStderr.toByteArray(), "UTF-8"))
        myStderr.reset()
      }

      // testing for failure/exit value
      checkShellExec(shell, [command: [shellScript, "-1", "-e"], failOnError: false], 1, "this goes to stdout\n", "")

      // test that when there is a failure, then an exception is properly generated if failOnError
      // is not defined
      def errorMsg = shouldFail(ShellExecException) {
        shell.exec(command: [shellScript, "-1", "-e"])
      }
      assertTrue(errorMsg.endsWith("res=1 - output=this goes to stdout - error="))

      // test that when there is a failure, then an exception is properly generated if failOnError
      // is set to true
      errorMsg = shouldFail(ShellExecException) {
        shell.exec(command: [shellScript, "-1", "-e"], failOnError: true)
      }
      assertTrue(errorMsg.endsWith("res=1 - output=this goes to stdout - error="))

      // reading from stdin
      checkShellExec(shell, [command: [shellScript, "-1", "-c"], stdin: "abc\ndef\n"], 0, "this goes to stdout\nabc\ndef\n", "")

      // testing for stdoutStream
      InputStream stdout = shell.exec(command: [shellScript, "-1", "-2"], res: "stdoutStream")
      assertEquals("this goes to stdout\n", stdout.text)

      // testing for stdoutStream
      InputStream stderr = shell.exec(command: [shellScript, "-1", "-2"], res: "stderrStream")
      assertEquals("this goes to stderr\n", stderr.text)

      // testing for stream
      InputStream stream = shell.exec(command: [shellScript, "-1", "-2", "-e"], failOnError: false, res: "stream")

      myStdout = new ByteArrayOutputStream()
      myStderr = new ByteArrayOutputStream()

      assertEquals(1, shell.demultiplexExecStream(stream, myStdout, myStderr))
      assertEquals("this goes to stdout\n", new String(myStdout.toByteArray(), "UTF-8"))
      assertEquals("this goes to stderr\n", new String(myStderr.toByteArray(), "UTF-8"))

      stream = shell.exec(command: [shellScript, "-1", "-2", "-e"], failOnError: true, res: "stream")

      myStdout = new ByteArrayOutputStream()
      myStderr = new ByteArrayOutputStream()

      errorMsg = shouldFail(ShellExecException) {
        shell.demultiplexExecStream(stream, myStdout, myStderr)
      }
      assertTrue(errorMsg.endsWith("res=1 - output= - error="))
      assertEquals("this goes to stdout\n", new String(myStdout.toByteArray(), "UTF-8"))
      assertEquals("this goes to stderr\n", new String(myStderr.toByteArray(), "UTF-8"))

      // testing pwd
      checkShellExec(shell, [command: ["pwd"]], 0, "${new File(".").canonicalPath}\n", "")
      def pwdDir = shell.mkdirs("/pwd")
      checkShellExec(shell, [command: ["pwd"], pwd: "/pwd"], 0, "${pwdDir.file.canonicalPath}\n", "")

      ProcessBuilder pb = new ProcessBuilder(ShellExec.buildCommandLine(['pwd']))
      pb.directory(shell.toResource("/pwdDoNotExist").file)
      String errorMessage = null
      try
      {
        pb.start()
      }
      catch(IOException e)
      {
        errorMessage = e.message
      }

      checkShellExec(shell, [command: ["pwd"], pwd: "/pwdDoNotExist", failOnError: false], 2, "", "${errorMessage}")

      // testing env
      checkShellExec(shell, [command: 'echo $HOME'], 0, "${System.getenv().HOME}\n", "")

      // changing environment variable
      def homeDir = shell.mkdirs("/home")
      checkShellExec(shell, [command: 'echo $HOME', env: [HOME: homeDir.file.canonicalPath]], 0, "${homeDir.file.canonicalPath}\n", "")

      // removing environment variable
      checkShellExec(shell, [command: 'echo $HOME', env: [HOME: null]], 0, "\n", "")

      // not inheriting
      checkShellExec(shell, [command: 'echo $HOME', inheritEnv: false], 0, "\n", "")
    }
  }

  private static void checkShellExec(shell, commands, exitValue, stdout, stderr)
  {
    checkShellExec(shell, commands, exitValue, stdout, stderr, null)
  }

  private static void checkShellExec(shell, commands, exitValue, stdout, stderr, Closure cl)
  {
    assertEquals(stdout.trim(), shell.exec(*:commands))
    if(cl) cl()
    assertEquals(exitValue, shell.exec(*:commands, res: "exitValue"))
    if(cl) cl()
    assertEquals(exitValue.toString(), shell.exec(*:commands, res: "exitValueStream").text)
    if(cl) cl()
    assertEquals(stdout.trim(), shell.exec(*:commands, res: "stdout"))
    if(cl) cl()
    assertEquals(stdout.getBytes("UTF-8"), shell.exec(*:commands, res: "stdoutBytes"))
    if(cl) cl()
    assertEquals(stderr.trim(), shell.exec(*:commands, res: "stderr"))
    if(cl) cl()
    assertEquals(stderr.getBytes("UTF-8"), shell.exec(*:commands, res: "stderrBytes"))
    if(cl) cl()
    assertEquals([exitValue: exitValue, stdout: stdout.trim(), stderr: stderr.trim()],
                 shell.exec(*:commands, res: "all"))
    if(cl) cl()
    def res = shell.exec(*:commands, res: "allBytes")
    assertEquals(exitValue, res.exitValue)
    assertEquals(stdout.getBytes("UTF8"), res.stdout)
    assertEquals(stderr.getBytes("UTF8"), res.stderr)
    if(cl) cl()
  }

  void testTail()
  {
    ShellImpl.createTempShell { Shell shell ->

      def content = new StringBuilder()

      def f = shell.withOutputStream('testFile') { file, out ->
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
   * Test that untar handles gzip properly
   */
  void testUntar()
  {
    ShellImpl.createTempShell { Shell shell ->

      ["./src/test/resources/testUntar_tar", "./src/test/resources/testUntar_tgz"].each { file ->
        def fetchedFile = new File(file).canonicalFile.toURI()

        def untarred = shell.untar(shell.fetch(fetchedFile))

        assertEquals("for ${file}", ['a.txt', 'b.txt', 'c.txt'], shell.ls(untarred).filename.sort())
      }
    }
  }

  /**
   * Test that tarring/untarring preserve executable bit
   */
  void testTar()
  {
    ShellImpl.createTempShell { Shell shell ->
      def tarFileContent = [
        '/tar/a/a.sh',
        '/tar/a/a.txt',
        '/tar/b.sh',
        '/tar/b.txt'
      ].collect {
        def f = shell.saveContent(it, "...content of ${it}...")
        if(f.filename.endsWith('.sh'))
          shell.chmodPlusX(f)
        return f
      }

      assertEquals(tarFileContent.findAll { it.filename.endsWith('.sh') },
                   tarFileContent.findAll { Files.isExecutable(it.file.toPath()) })

      def testCases = [
        // tar into a directory
        [tarDir: '/out', expectedTarFile: '/out/tar.tar'],

        // tar into a file
        [tarFile: '/out2/foo.tar', expectedTarFile: '/out2/foo.tar'],

        // tar into a directory with compression (gzip)
        [tarDir: '/out3', expectedTarFile: '/out3/tar.tgz', compression: 'gzip'],

        // tar into a file with compression (gzip)
        [tarFile: '/out4/foo.tar.gz', expectedTarFile: '/out4/foo.tar.gz', compression: 'gzip'],

        // tar into a directory with compression (bzip2)
        [tarDir: '/out5', expectedTarFile: '/out5/tar.tbz2', compression: 'bzip2'],

        // tar into a file with compression (bzip2)
        [tarFile: '/out6/foo.tar.bz2', expectedTarFile: '/out6/foo.tar.bz2', compression: 'bzip2'],
      ]

      testCases.each { tc ->
        [true, false].each { boolean includeRoot ->
          Resource tarFile = shell.tar(dir: '/tar',
                                       includeRoot: includeRoot,
                                       *:GluGroovyCollectionUtils.xorMap(tc, ['expectedTarFile']))

          // make sure that the tarFile is the proper one
          assertEquals(shell.toResource(tc.expectedTarFile), tarFile)

          def outputDir = shell.untar(tarFile)

          [
            '/tar/a/a.sh',
            '/tar/a/a.txt',
            '/tar/b.sh',
            '/tar/b.txt'
          ].each { f ->
            Resource includedFile = outputDir.createRelative(includeRoot ? f : f - '/tar')
            assertEquals("for ${[*:tc, includeRoot: includeRoot]}", "...content of ${f}...".toString(), shell.cat(includedFile))
            if(includedFile.filename.endsWith('.sh'))
              assertTrue("${includedFile} is executable", Files.isExecutable(includedFile.file.toPath()))
            else
              assertFalse(Files.isExecutable(includedFile.file.toPath()))
          }
        }
      }
    }
  }

  /**
   * Test the grep capability
   */
  void testGrep()
  {
    ShellImpl.createTempShell { Shell shell ->

      Resource tempFile = shell.tempFile()
      shell.saveContent(tempFile, """line 1 abc
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
    ShellImpl.createTempShell { Shell shell ->

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

      assertEquals(expectedResult, res.keySet().path.sort())
      assertEquals(expectedResult, leavesPaths(root).toArray().sort())
    }
  }

  void testRecurse()
  {
    ShellImpl.createTempShell { Shell shell ->

      def root = shell.toResource('/root')
      shell.saveContent('/root/dir1/a.txt', 'this is a test a')
      shell.saveContent('/root/dir1/b.txt', 'this is a test b')
      shell.saveContent('/root/dir1/dir2/c.txt', 'this is a test c')
      shell.saveContent('/root/dir1/dir2/d.txt', 'this is a test d')
      shell.saveContent('/root/e.txt', 'e')

      // every file and dir under root
      def files = []
      shell.eachChildRecurse(root) { r ->
        files << r.path
      }
      files.sort()

      def expectedFiles = ['/root/dir1', '/root/e.txt', '/root/dir1/a.txt', '/root/dir1/b.txt', '/root/dir1/dir2', '/root/dir1/dir2/c.txt', '/root/dir1/dir2/d.txt'].sort()
      assertEquals(expectedFiles, files)

      // find only dirs under root
      def dirs = shell.findAll('/root') { r ->
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
    ShellImpl.createTempShell { Shell shell ->

      assertEquals('abc foo efg bar hij foo',
                   shell.replaceTokens('abc @token1@ efg @token2@ hij @token1@',
                                       [token1: 'foo', token2: 'bar']))

      Resource testFile = shell.saveContent('test.txt', 'abc @token1@ efg @token2@ hij @token1@',
                                            [token1: 'foo', token2: 'bar'])

      assertEquals('abc foo efg bar hij foo', shell.cat(testFile))
    }
  }

  void testProcessTemplate()
  {
    ShellImpl.createTempShell { Shell shell ->

      def templates = shell.mkdirs('/templates')

      // .xtmpl
      def t1 = shell.saveContent(templates.createRelative('/foo.xtmpl'),
                                 'abc @token1@ efg @token2@ hij @token1@')
      // in a file
      def p1 = shell.processTemplate(t1, '/out1/foo', [token1: 'foo', token2: 'bar'])
      assertEquals('/out1/foo', p1.path)
      assertEquals('abc foo efg bar hij foo', shell.cat(p1))
      assertFalse(Files.isExecutable(p1.file.toPath()))

      shell.mkdirs('/out')
      // in an existing dir
      def p2 = shell.processTemplate(t1, '/out', [token1: 'foo', token2: 'bar'])
      assertEquals('/out/foo', p2.path)
      assertEquals('abc foo efg bar hij foo', shell.cat(p2))
      assertFalse(Files.isExecutable(p2.file.toPath()))

      // make it executable
      shell.chmodPlusX(t1)

      def p1x = shell.processTemplate(t1, '/out1x/foo', [token1: 'foo', token2: 'bar'])
      assertEquals('/out1x/foo', p1x.path)
      assertEquals('abc foo efg bar hij foo', shell.cat(p1x))
      assertTrue(Files.isExecutable(p1x.file.toPath()))

      // .gtmpl
      def t2 = shell.saveContent(templates.createRelative('/foo.gtmpl'),
                                 'abc ${token1} efg ${token2} hij ${token1} \\${token3}')

      // in a file
      def p3 = shell.processTemplate(t2, '/out3/foo', [token1: 'foo', token2: 'bar'])
      assertEquals('/out3/foo', p3.path)
      assertEquals('abc foo efg bar hij foo ${token3}', shell.cat(p3))

      // in an existing dir
      def p4 = shell.processTemplate(t2, '/out', [token1: 'foo', token2: 'bar'])
      assertEquals('/out/foo', p4.path)
      assertEquals('abc foo efg bar hij foo ${token3}', shell.cat(p4))

      // unknown extension => no processing... simply copy
      def t3 = shell.saveContent(templates.createRelative('/foo.xxx'),
                                 'abc @token1@ efg @token2@ hij @token1@')

      // in an existing dir
      def p5 = shell.processTemplate(t3, '/out', [token1: 'foo', token2: 'bar'])
      assertEquals('/out/foo.xxx', p5.path)
      assertEquals('abc @token1@ efg @token2@ hij @token1@', shell.cat(p5))

    }
  }

  void testCodeTemplate()
  {
    ShellImpl.createTempShell { Shell shell ->

      def templates = shell.mkdirs('/templates')

      def inputFile = shell.toResource('/in/file.bin')

      shell.withOutputStream('/in/file.bin') { OutputStream out ->
        (0..10).each { b -> out.write(b) }
      }

      def out = []

      def tokens = [token1: 'foo', token2: 'bar', inputFile: inputFile.file.path, out: out]

      def templateCode = """
out << "starting copy...\${token1}"
shell.cp(inputFile, toResource)
out << "done copy...\${token2}"
"""

      Resource templateCodeResource = shell.saveContent(templates.createRelative('foo.ctmpl'),
                                                        templateCode)

      // 1. in a file
      def p1 = shell.processTemplate(templateCodeResource, '/out1/foo', tokens)
      assertEquals('/out1/foo', p1.path)
      assertEquals(['starting copy...foo', 'done copy...bar'], out)

      shell.withInputStream(p1) { InputStream stream ->
        (0..10).each { assertEquals(it, stream.read()) }
        assertEquals(-1, stream.read())
      }

      // 2. in a directory
      out.clear()
      shell.mkdirs('/out2')
      def p2 = shell.processTemplate(templateCodeResource, '/out2/foo', tokens)
      assertEquals('/out2/foo', p2.path)
      assertEquals(['starting copy...foo', 'done copy...bar'], out)

      shell.withInputStream(p2) { InputStream stream ->
        (0..10).each { assertEquals(it, stream.read()) }
        assertEquals(-1, stream.read())
      }
    }
  }

  void testSha1()
  {
    ShellImpl.createTempShell { Shell shell ->
      assertEquals('03cfd743661f07975fa2f1220c5194cbaff48451',
                   shell.sha1(shell.saveContent('/foo.txt', 'abc\n')))
    }
  }

  /**
   * Test the tail from offset capability
   */
  public void testTailFromOffset()
  {
    ShellImpl.createTempShell { Shell shell ->

      shell.withTempFile { Resource tempFile ->
        shell.saveContent(tempFile, "0123456789")

        def res = shell.tailFromOffset(location: tempFile)
        assertEquals("0123456789", res.tailStream.text)
        assertEquals(10, res.length)
        assertEquals(10, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        res = shell.tailFromOffset(location: tempFile, offset: -3)
        assertEquals("789", res.tailStream.text)
        assertEquals(10, res.length)
        assertEquals(3, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        res = shell.tailFromOffset(location: tempFile, offset: 10)
        assertEquals("", res.tailStream.text)
        assertEquals(10, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        shell.saveContent(tempFile, "012345678901")

        res = shell.tailFromOffset(location: tempFile, offset: 10)
        assertEquals("01", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(2, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        res = shell.tailFromOffset(location: tempFile, offset: 12)
        assertEquals("", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        res = shell.tailFromOffset(location: tempFile, offset: -25)
        assertEquals("012345678901", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(12, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        res = shell.tailFromOffset(location: tempFile, offset: 34)
        assertEquals("", res.tailStream.text)
        assertEquals(12, res.length)
        assertEquals(0, res.tailStreamMaxLength)
        checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
        assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
        assertFalse(res.isSymbolicLink)

        shell.withTempFile { Resource t2 ->
          Files.createSymbolicLink(t2.file.toPath(), tempFile.file.toPath())

          res = shell.tailFromOffset(location: t2, offset: 10)

          assertEquals("01", res.tailStream.text)
          assertEquals(12, res.length)
          assertEquals(2, res.tailStreamMaxLength)
          checkTimeDifference(tempFile.file.lastModified(), res.lastModified)
          assertEquals(tempFile.file.canonicalPath, res.canonicalPath)
          assertTrue(res.isSymbolicLink)
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

  private static def leavesPaths(Resource root)
  {
    new TreeSet(GroovyIOUtils.findAll(root) { !it.isDirectory() }.collect { it.path })
  }
}