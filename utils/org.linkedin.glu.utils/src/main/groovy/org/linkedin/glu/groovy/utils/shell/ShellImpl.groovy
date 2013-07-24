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


package org.linkedin.glu.groovy.utils.shell

import eu.medsea.mimeutil.MimeUtil
import groovy.text.GStringTemplateEngine
import org.apache.tools.ant.filters.ReplaceTokens
import org.apache.tools.ant.filters.ReplaceTokens.Token
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecutionThreadFactory
import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils
import org.linkedin.glu.utils.concurrent.OneThreadPerTaskSubmitter
import org.linkedin.glu.utils.concurrent.Submitter
import org.linkedin.glu.utils.core.Externable
import org.linkedin.groovy.util.ant.AntUtils
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.groovy.util.config.Config
import org.linkedin.groovy.util.encryption.EncryptionUtils
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.io.PathUtils
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.lang.MemorySize
import org.linkedin.util.url.QueryBuilder

import javax.management.Attribute
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.security.MessageDigest
import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * contains the utility methods for the shell
 *
 * @author ypujante@linkedin.com
 */
def class ShellImpl implements Shell
{
  public static final String MODULE = ShellImpl.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final MemorySize FILE_BUFFER_SIZE = MemorySize.parse('1m')

  static {
    MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
  }

  /**
   * Convenient call which simply creates a shell in a temporary directory
   *
   * @param closure the closure will be called with the created shell
   * @return whatever the closure returns
   */
  static <T> T createTempShell(Closure<T> closure)
  {
    return FileSystemImpl.createTempFileSystem { FileSystem fs ->
      ShellImpl shell = new ShellImpl(fileSystem: fs)
      closure(shell)
    }
  }

  /**
   * @return a shell where the file system is root (/)
   */
  static Shell createRootShell()
  {
    new ShellImpl(fileSystem: new FileSystemImpl(new File('/')))
  }

  Clock clock = SystemClock.instance()

  // will delegate all the calls to fileSystem
  @Delegate FileSystem fileSystem

  /**
   * The charset to use for reading/writing files */
  def charset = 'UTF-8'

  /**
   * Used when threads are needed
   */
  protected Submitter _submitter

  synchronized Submitter getSubmitter()
  {
    if(_submitter == null)
      _submitter = new OneThreadPerTaskSubmitter(new FutureTaskExecutionThreadFactory())
    return _submitter
  }

  void setSubmitter(Submitter submitter)
  {
    _submitter = submitter
  }

  Shell newShell(FileSystem fileSystem)
  {
    return new ShellImpl(fileSystem: fileSystem,
                         charset: charset,
                         clock: clock,
                         submitter: _submitter)
  }

  Shell newShell(def file)
  {
    newShell(newFileSystem(file))
  }

  Collection<String> getMimeTypes(file)
  {
    withInputStream(file) { InputStream is ->
      MimeUtil.getMimeTypes(is).collect { it.toString() }
    } as Collection<String>
  }

  Resource unzip(file)
  {
    return unzip(file, createTempDir())
  }

  Resource unzip(file, toDir)
  {
    file = toResource(file)
    toDir = toResource(toDir)
    ant { ant ->
      ant.unzip(src: file.file, dest: toDir.file)
    }
    return toDir
  }

  Resource untar(file)
  {
    return untar(file, createTempDir())
  }

  Resource untar(file, toDir)
  {
    file = toResource(file)
    toDir = toResource(toDir)
    def command = "tar -xf ${file.file}"

    def mimeTypes = getMimeTypes(file)

    if(mimeTypes.find { it == 'application/x-gzip'})
    {
      command = "gunzip -c ${file.file} | tar -xf -"
    }

    if(mimeTypes.find { it == 'application/x-bzip2'})
    {
      command = "bunzip2 -c ${file.file} | tar -xf -"
    }

    mkdirs(toDir)

    exec(command: command, pwd: toDir.file)

    return toDir
  }

  Resource tar(args)
  {
    Resource dir = toResource(args.dir)

    if(args.tarFile && args.tarDir)
      throw new IllegalArgumentException("tarFile and tarDir should not be used together: provide only one")

    Resource tarFile = null

    if(args.tarFile)
    {
      tarFile = toResource(args.tarFile)
    }
    else
    {
      Resource tarDir = toResource(args.tarDir ?: createTempDir())

      if(tarDir.exists() && !tarDir.isDirectory())
        throw new IllegalArgumentException("${tarDir} is not a directory")

      tarFile =
        tarDir.createRelative("${dir.filename}.${computeTarFileExtension(args.compression)}")
    }

    // make sure the folder exists
    mkdirs(tarFile.parentResource)

    boolean includeRoot = Config.getOptionalBoolean(args, "includeRoot", true)

    def basedir = (includeRoot ? dir.parentResource : dir).chroot('.')
    def includes = includeRoot ? basedir.createRelative(dir.filename) : basedir

    def executables = []
    def nonExecutables = []

    GroovyIOUtils.eachChildRecurse(includes) { Resource child ->
      if(!child.isDirectory() && Files.isExecutable(child.file.toPath()))
        executables << PathUtils.removeLeadingSlash(child.path)
      else
        nonExecutables << PathUtils.removeLeadingSlash(child.path)
    }

    // make sure the tarFile actually does not already exist
    rm(tarFile)

    ant { ant ->
      ant.tar(destfile: tarFile.file,
              longfile: 'gnu',
              compression: args.compression ?: 'none') {
        if(executables)
          tarfileset(dir: basedir.file, filemode: '755') {
            executables.each {
              include(name: it)
            }
          }
        if(nonExecutables)
          fileset(dir: basedir.file) {
            nonExecutables.each {
              include(name: it)
            }
          }
      }
    }

    return tarFile
  }

  private String computeTarFileExtension(String compression)
  {
    if(!compression)
      return 'tar'

    switch(compression)
    {
      case 'gzip':
        return 'tgz'

      case 'bzip2':
        return 'tbz2'

      default:
        throw new IllegalArgumentException("unsupported compression ${compression}")
    }
  }

  /**
   * Uncompresses the provided file
   * @return the original file or the new one
   */
  Resource gunzip(file)
  {
    file = toResource(file)

    def gunzipFilename
    if(file.filename.endsWith('.gz'))
    {
      gunzipFilename = file.filename[0..-4]
    }
    else
    {
      if(file.filename.endsWith('.tgz'))
      {
        gunzipFilename = "${file.filename[0..-5]}.tar"
      }
      else
      {
        throw new IOException("unknown suffix ${file.filename}")
      }
    }
    
    def gunzipFile = gunzip(file, file.parentResource."${URLEncoder.encode(gunzipFilename, "UTF-8")}")

    if(file != gunzipFile)
    {
      rm(file)
    }

    return gunzipFile
  }

  /**
   * Uncompresses the provided file (if compressed) into a new file
   */
  Resource gunzip(file, toFile)
  {
    file = toResource(file)
    toFile = toResource(toFile)

    if(!file.isDirectory() && getMimeTypes(file).find { it == 'application/x-gzip'})
    {
      if(!toFile.exists())
      {
        mkdirs(toFile.parentResource)
      }

      ant { ant ->
        ant.gunzip(src: file.file, dest: toFile.file)
      }

      return toFile
    }
    else
    {
      return file
    }
  }

  /**
   * Compresses the provided file (first make sure that the file is compressed)
   *
   * @return the gziped file or the original file if not zipped
   */
  Resource gzip(file)
  {
    file = toResource(file)

    def gzipFile = gzip(file, file.parentResource."${URLEncoder.encode(file.filename, "UTF-8")}.gz")

    if(file != gzipFile)
    {
      rm(file)
    }

    return gzipFile
  }

  /**
   * Compresses the provided file (first make sure that the file is compressed)
   *
   * @return the gziped file or the original file if not zipped
   */
  Resource gzip(file, toFile)
  {
    file = toResource(file)
    toFile = toResource(toFile)

    if(!file.isDirectory() && getMimeTypes(file).find { it != 'application/x-gzip'})
    {
      ant { ant ->
        ant.gzip(src: file.file, destfile: toFile.file)
      }

      return toFile
    }
    else
    {
      return file
    }
  }

  /**
   * Compresses each file in a folder
   *
   * @param fileOrFolder a file (behaves like {@link #gzip(Object)}) or a folder
   * @param recurse if <code>true</code> then recursively process all folders
   * @return a map with key is resource and value is delta in size (original - new) (note that it
   * contains only the resources that are modified!)
   */
  Map gzipFileOrFolder(fileOrFolder, boolean recurse)
  {
    def res = [:]

    fileOrFolder = toResource(fileOrFolder)

    if(fileOrFolder.isDirectory())
    {
      fileOrFolder.ls().each { file ->
        if(recurse)
        {
          res.putAll(gzipFileOrFolder(file, recurse))
        }
        else
        {
          long size = file.size()
          def gzipedFile = gzip(file)
          if(gzipedFile != file)
            res[gzipedFile] = size - gzipedFile.size()
        }
      }
    }
    else
    {
      long size = fileOrFolder.size()
      def gzipedFile = gzip(fileOrFolder)
      if(gzipedFile != fileOrFolder)
        res[gzipedFile] = size - gzipedFile.size()
    }

    return res
  }

  Resource untarAndDecrypt(file, toDir, encryptionKeys)
  {

    def tmpDir = createTempDir()
    untar(file, tmpDir)
    EncryptionUtils.decryptFiles(tmpDir.getFile(), toDir.getFile(), encryptionKeys)
    rmdirs(tmpDir)
    return toDir
  }


  /**
   * Exporting ant access to the shell 
   */
  def ant(Closure closure)
  {
    return AntUtils.withBuilder { closure(it) }
  }

  /**
   * Fetches the file pointed to by the location. The location can be <code>File</code>,
   * a <code>String</code> or <code>URI</code> and must contain a scheme. Example of locations: 
   * <code>http://locahost:8080/file.txt'</code>, <code>file:/tmp/file.txt</code>,
   * <code>ivy:/org.linkedin/util-core/1.0.0</code>.
   */
  Resource fetch(location)
  {
    return fetch(location, null)
  }

  /**
   * Fetches the file pointed to by the location. The location can be <code>File</code>,
   * a <code>String</code> or <code>URI</code> and must contain a scheme. Example of locations:
   * <code>http://locahost:8080/file.txt'</code>, <code>file:/tmp/file.txt</code>,
   * <code>ivy:/org.linkedin/util-core/1.0.0</code>. The difference with the other fetch method
   * is that it fetches the file in the provided destination rather than in the tmp space.
   */
  Resource fetch(location, destination)
  {
    URI uri = GroovyNetUtils.toURI(location)

    if(uri == null)
      return null

    Resource tempFile
    if(destination)
    {
      tempFile = toResource(destination)
      mkdirs(tempFile.parentResource)
    }
    else
    {
      tempFile = createTempDir()
    }

    if(tempFile.isDirectory())
    {
      def filename = GroovyNetUtils.guessFilename(uri)
      tempFile = tempFile.createRelative(filename)
    }

    GroovyIOUtils.fetchContent(location, tempFile.file)

    return tempFile
  }

  /**
   * Fetches the content of the location and returns it as a <code>String</code> or
   * <code>null</code> if the location is not reachable
   * 
   * @deprecated use {@link #cat(Object) instead
   */
  String fetchContent(location)
  {
    return cat(location)
  }

  /**
   * Returns the content of the location as a <code>String</code> or
   * <code>null</code> if the location is not reachable
   */
  String cat(location)
  {
    try
    {
      return GroovyIOUtils.cat(location)
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled())
        log.debug("[ignored] exception while catting content ${location}", e)
      return null
    }
  }

  /**
   * Issue a 'HEAD' request. The location should be an http or https link.
   *
   * @param location
   * @return a map with the following entries:
   * responseCode: 200, 404... {@link java.net.HttpURLConnection#getResponseCode()}
   * responseMessage: message {@link java.net.HttpURLConnection#getResponseMessage()}
   * headers: representing all the headers {@link java.net.URLConnection#getHeaderFields()}
   */
  Map httpHead(location)
  {
    Map res = [:]

    URI uri = GroovyNetUtils.toURI(location)

    URL url = uri.toURL()
    URLConnection cx = url.openConnection()
    try
    {
      if(cx instanceof HttpURLConnection)
      {
        cx.requestMethod = 'HEAD'
        cx.doInput = true
        cx.doOutput = false

        cx.connect()

        res.responseCode = cx.responseCode
        res.responseMessage = cx.responseMessage
        res.headers = cx.headerFields
      }
    }
    finally
    {
      if(cx.respondsTo('close'))
        cx.close()
    }

    return res
  }

  /**
   * Issue a 'POST' request. The location should be an http or https link. The request will be
   * made with <code>application/x-www-form-urlencoded</code> content type.
   *
   * @param location
   * @param parameters the parameters of the post as map of key value pairs (value can be a single
   * value or a collection of values)
   * @return a map with the following entries:
   * responseCode: 200, 404... {@link java.net.HttpURLConnection#getResponseCode()}
   * responseMessage: message {@link java.net.HttpURLConnection#getResponseMessage()}
   * headers: representing all the headers {@link java.net.URLConnection#getHeaderFields()}
   */
  Map httpPost(location, Map parameters)
  {
    Map res = [:]

    URI uri = GroovyNetUtils.toURI(location)

    URL url = uri.toURL()
    URLConnection cx = url.openConnection()
    try
    {
      if(cx instanceof HttpURLConnection)
      {
        cx.requestMethod = 'POST'
        cx.setRequestProperty('Content-Type', 'application/x-www-form-urlencoded')
        cx.doInput = true
        cx.doOutput = true

        cx.connect()

        QueryBuilder qb = new QueryBuilder()

        parameters?.each { k, v ->
          if(v != null)
          {
            if(v instanceof Collection)
              qb.addParameters(k.toString(), v.collect { it.toString() } as String[])
            else
              qb.addParameter(k.toString(), v.toString())
          }
        }

        cx.outputStream << qb.toString()
        cx.outputStream.close()

        res.responseCode = cx.responseCode
        res.responseMessage = cx.responseMessage
        res.headers = cx.headerFields
      }
    }
    finally
    {
      if(cx.respondsTo('close'))
        cx.close()
    }

    return res
  }

  /**
   * Similarly to the unix grep command, checks the location one line at a time and returns
   * all the lines which matches the pattern.
   */
  Collection<String> grep(location, pattern)
  {
    return (Collection<String>) grep(location, pattern, null)
  }

  /**
   * Similarly to the unix grep command, checks the location one line at a time and returns
   * all the lines which matches the pattern
   *
   * @param options options to the command:
   *   <code>out</code>: an object to output the lines which match (default to [])
   *   <code>count</code>: returns the count only (does not use out)
   *   <code>maxCount</code>: stop reading after <code>maxCount</code> matches
   */
  def grep(location, pattern, options)
  {
    options = options ?: [:]
    options = new HashMap(options)
    def out = options.out ?: []
    int count = 0

    def uri = GroovyNetUtils.toURI(location)

    if(pattern instanceof String)
    {
      pattern = Pattern.compile(pattern)
    }

    try
    {
      def idx = 1
      GroovyIOUtils.eachLine(uri.toURL()) { line ->
        if(pattern.matcher(line).find())
        {
          count++
          if(!options.count)
            out << line
          if(count == options.maxCount)
            return false
        }
        idx++
        return true
      }
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled())
        log.debug("[ignored] exception while grepping content ${location}", e)
    }

    if(options.count)
    {
      return count
    }
    else
    {
      return out
    }
  }

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   */
  String exec(executable, executableArgs)
  {
    executable = chmodPlusX(executable)
    doExec("${executable} ${toStringCommandLine(executableArgs)}".toString())
  }

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   */
  String exec(executable, Object... executableArgs)
  {
    exec(executable, executableArgs.collect { it })
  }

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   */
  String exec(String command)
  {
    return doExec(command)
  }

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   */
  String exec(Collection command)
  {
    return doExec(command)
  }

  /**
   * @{inheritDoc}
   */
  def exec(Map args)
  {
    if(args.pwd)
    {
      def pwd = toResource(args.pwd).file
      args = GluGroovyCollectionUtils.xorMap(args, ['pwd'])
      args.pwd = pwd
    }
    new ShellExec(shell: this, args: args).exec()
  }

  /**
   * @{inheritDoc}
   */
  def demultiplexExecStream(InputStream execStream,
                            OutputStream stdout,
                            OutputStream stderr)
  {
    GluGroovyIOUtils.demultiplexExecStream(execStream, stdout, stderr)
  }

  /**
   * Shortcut/More efficient implementation of the more generic {@link #chmod(Object, Object) call
   */
  Resource chmodPlusX(file)
  {
    file = toResource(file)
    file.file.setExecutable(true)
    return file
  }

  /**
   * Change the permission of the file  
   */
  Resource chmod(file, perm)
  {
    file = toResource(file)
    exec(command: ['chmod', perm, file.file], res: 'exitValue')
    return file
  }

  Resource chmodRecursive(dir, perm)
  {
    eachChildRecurse(dir) { file ->
      exec(command: ['chmod', perm, file.file], res: 'exitValue')
    }
    
    return dir
  }

  /**
   * Invokes the closure with an <code>MBeanServerConnection</code> to the jmx control running
   * on the vm started with the provided pid. The closure will be invoked with <code>null</code>
   * if cannot determine the process.
   */
  def withMBeanServerConnection(pid, Closure closure)
  {
    def serviceURL = extractJMXServiceURL(pid)
    if(serviceURL)
    {
      def connector = null
      try
      {
        connector = JMXConnectorFactory.connect(serviceURL)
      }
      catch(IOException e)
      {
        // ignored (connector remains null)
        if(log.isDebugEnabled())
        {
          log.debug("Ignored exception", e) 
        }
      }

      try
      {
        use(MBeanServerConnectionCategory) {
          closure(connector?.MBeanServerConnection)
        }
      }
      finally
      {
        connector?.close()
      }
    }
    else
    {
      closure(null)
    }
  }

  /**
   * Extract the serviceURL using sun internal implementation
   */
  private JMXServiceURL extractJMXServiceURL(pid)
  {
    if(pid == null)
      return null

    String serviceURL = null
    try
    {
      serviceURL = sun.management.ConnectorAddressLink.importFrom(pid as int)
    }
    catch (IOException ignored)
    {
      log.warn("Cannot find process ${pid}")
    }
    
    if(serviceURL == null)
      return null
    else
      return new JMXServiceURL(serviceURL)
  }

  /**
   * Waits for the condition to be <code>true</code> no longer than the timeout. <code>true</code>
   * is returned if the condition was satisfied, <code>false</code> otherwise (if you specify
   * noException)
   * @param args.timeout how long max to wait
   * @param args.heartbeat how long to wait between calling the condition
   * @param args.noException to get <code>false</code> instead of an exception
   */
  boolean waitFor(args, Closure condition)
  {
    try
    {
      GroovyConcurrentUtils.waitForCondition(clock, args.timeout, args.heartbeat, condition)
      return true
    }
    catch (TimeoutException e)
    {
      if(args.noException)
        return false
      else
        throw e
    }
  }

  /**
   * Waits for the condition to be <code>true</code> no longer than the timeout. <code>true</code>
   * is returned if the condition was satisfied, <code>false</code> otherwise
   */
  boolean waitFor(Closure condition)
  {
    return waitFor([:], condition)
  }

  /**
   * Tail the location
   *
   * @params location the location of the file to tail
   * @params maxLine the number of lines maximum to read
   *
   * @return the input stream to read the content of the tail
   */
  InputStream tail(location, long maxLine)
  {
    return tail([location: location, maxLine: maxLine])
  }

  /**
   * Tail the location
   *
   * @params args.location the location of the file to tail
   * @params args.maxLine the number of lines maximum to read (-1 for the entire file)
   * @params args.maxSize the maximum size to read (-1 for the entire file)
   *
   * @return the input stream to read the content of the tail
   */
  InputStream tail(args)
  {
    File file = toResource(args.location)?.file

    if(file && file.exists())
    {
      if(args.maxLine?.toString() == '-1' || args.maxSize?.toString() == '-1')
        return new FileInputStream(file)

      def commandLine = ['tail']
      if(args.maxLine)
        commandLine << "-${args.maxLine}"
      if(args.maxSize)
        commandLine << '-c' << MemorySize.parse(args.maxSize.toString()).sizeInBytes
      commandLine << file.canonicalPath
      return forkAndExec(commandLine)
    }
    else
      return null
  }

  /**
   * Forks a process to execute the command line provided (as a single string) and returns the
   * input stream of the process
   */
  private InputStream forkAndExec(commandLine)
  {
    exec(command: commandLine,
         redirectStderr: true,
         res: 'stdoutStream') as InputStream
  }

  /**
   * Make sure that the command line is a string.
   */
  String toStringCommandLine(commandLine)
  {
    if(commandLine instanceof GString)
      commandLine = commandLine.toString()

    if(!(commandLine instanceof String))
      commandLine = commandLine.collect { it.toString() }.join(' ')

    return commandLine
  }

  InputStream toInputStream(stream)
  {
    if(stream == null)
      return stream

    if(stream instanceof InputStream)
      return stream

    return new ByteArrayInputStream(stream.toString().getBytes(charset))
  }

  protected def doExec(commandLine)
  {
    return exec(command: commandLine,
                failOnError: true,
                res: 'all').stdout
  }

  /**
   * Converts the output into a string. Assumes that the encoding is UTF-8. Replaces all line feeds
   * by '\n' and remove the last line feed.
   */
  def toStringOutput(byte[] output)
  {
    return output ? new String(output, charset).readLines().join('\n') : ""
  }

  /**
   * Converts the output into a string with no more than maxChars characters. If there are more
   * characters, then display '...' at the end.
   */
  def toLimitedStringOutput(byte[] output, int maxChars)
  {
    def string = toStringOutput(output)
    if(string?.size() > maxChars)
    {
      string = string.substring(0, maxChars) + "..."
    }
    return string
  }

  /**
   * @return <code>true</code> if there is a socket open on the server/port combination
   */
  boolean listening(server, port)
  {
    ant { ant ->
      ant.condition(property: 'listening') {
        socket(server: server, port: port)
      }.project.getProperty('listening') ? true : false
    } as boolean
  }

  private static def REPLACE_TOKENS_UNKNOWN_TYPE_HANDLER = { o ->
    if(o instanceof Externable)
      o.toExternalRepresentation()
    else
      o
  }

  /**
   * Replaces the tokens provided in the map in the input. Token replacement is using ant token
   * replacement class so in the input, the tokens are surrounded by the '@' sign.
   * Example:
   *
   * <pre>
   * input = "abcd @myToken@"
   * assert "abcd foo" == replaceTokens(input, [myToken: 'foo'])
   * </pre>
   */
  String replaceTokens(String input, Map tokens)
  {
    if(input == null)
      return null

    tokens = GluGroovyCollectionUtils.flatten(tokens, REPLACE_TOKENS_UNKNOWN_TYPE_HANDLER)

    ReplaceTokens rt = new ReplaceTokens(new StringReader(input))
    tokens?.each { k,v ->
      if(k != null && v != null)
        rt.addConfiguredToken(new Token(key: k, value: v))
    }

    return rt.text
  }

  /**
   * Processes <code>from</code> through the replacement token mechanism and writes the result to
   * <code>to</code>
   *
   * @param from anything that can be provided to {@link FileSystem#toResource(Object)}
   * @param to anything that can be provided to {@link FileSystem#toResource(Object)}
   * @param tokens a map of token
   * @return <code>to</code> as a {@link Resource}
   * @see #replaceTokens(String, Map)
   */
  Resource replaceTokens(def from, def to, Map tokens)
  {
    to = toResource(to)

    tokens = GluGroovyCollectionUtils.flatten(tokens, REPLACE_TOKENS_UNKNOWN_TYPE_HANDLER)

    withWriter(to) { Writer writer ->
      withReader(from) { Reader reader ->
        ReplaceTokens rt = new ReplaceTokens(reader)
        tokens?.each { k,v ->
          rt.addConfiguredToken(new Token(key: k, value: v))
        }
        writer << rt
      }
    }

    return to
  }

  /**
   * @{inheritDoc}
   */
  Resource processTemplate(def template, def to, Map tokens)
  {
    Resource templateResource = toResource(template)

    try
    {
      boolean toIsDirectory = to.toString().endsWith('/')
      Resource toResource = toResource(to)
      toIsDirectory = toIsDirectory || toResource.isDirectory()

      switch(GluGroovyIOUtils.getFileExtension(templateResource))
      {
        case 'gtmpl':
          def groovyTemplate = new GStringTemplateEngine().createTemplate(cat(templateResource))
          def processedTemplate = groovyTemplate.make(tokens)

          toResource = handleTemplateExtension(templateResource, toResource, toIsDirectory, 'gtmpl')

          withWriter(toResource) { Writer writer -> processedTemplate.writeTo(writer) }
          break

        case 'ctmpl':
          def binding = new Binding([*:tokens])

          toResource = handleTemplateExtension(templateResource, toResource, toIsDirectory, 'ctmpl')

          binding.shell = tokens.shell ?: this
          binding.toResource = toResource

          def groovyShell = new GroovyShell(binding)
          withReader(templateResource) { Reader templateReader ->
            groovyShell.evaluate(templateReader, templateResource.path)
          }

          break

        case 'xtmpl':
          toResource = handleTemplateExtension(templateResource, toResource, toIsDirectory, 'xtmpl')
          replaceTokens(templateResource, toResource, tokens)
          break

        default:
          if(toIsDirectory)
            toResource = toResource.createRelative(templateResource.filename)
          cp(templateResource, toResource)
          break
      }

      if(Files.isExecutable(templateResource.file.toPath()))
        chmodPlusX(toResource)

      return toResource
    }
    catch(Throwable th)
    {
      throw new TemplateProcessingException("Exception while processing template [${templateResource.toURI()}]",
                                            th)
    }
  }

  private Resource handleTemplateExtension(Resource templateResource,
                                           Resource toResource,
                                           boolean toIsDirectory,
                                           String templateExtension)
  {
    if(toIsDirectory)
      toResource =
        toResource.createRelative(templateResource.filename - ".${templateExtension}")
    else
    {
      if(GluGroovyIOUtils.getFileExtension(toResource) == templateExtension)
        toResource =
          toResource.parentResource.createRelative(toResource.filename - ".${templateExtension}")
    }

    return toResource
  }

  /**
   * Processes the content to the token replacement method.
   *
   * @see FileSystem#saveContent(Object, String)
   */
  Resource saveContent(file, String content, Map tokens)
  {
    saveContent(file, replaceTokens(content, tokens))
  }

  /**
   * Same as <code>withInputStream</code> but wraps in a reader using the {@link #charset}
   * @param file anything that can be provided to {@link FileSystem#toResource(Object)}
   * @return whatever the closure returns
   */
  def withReader(file, Closure closure)
  {
    withInputStream(file) { InputStream is ->
      is.withReader(charset, closure)
    }
  }

  /**
   * Same as <code>withOutputStream</code> but wraps in a writer using the {@link #charset}
   * @param file anything that can be provided to {@link FileSystem#toResource(Object)}
   * @return whatever the closure returns
   */
  def withWriter(file, Closure closure)
  {
    withOutputStream(file) { OutputStream os ->
      os.withWriter(charset, closure)
    }
  }

  /**
   * Computes the sha1 of a file/resource. Returns it as an hex string (40 chars)
   *
   * @return as an hex string (40 chars)
   */
  String sha1(file)
  {
    def md = MessageDigest.getInstance('SHA1')

    withInputStream(file) { InputStream stream ->
      stream.eachByte(FILE_BUFFER_SIZE.sizeInBytes as int) { byte[] buf, int bytesRead ->
        md.update(buf, 0, bytesRead);
      }
    }

    new BigInteger(1, md.digest()).toString(16).padLeft(40, '0')
  }

  /**
   * Runs the closure in a protected block that will not throw an exception but will return
   * <code>null</code> in the case one happens
   */
  def noException(Closure closure)
  {
    GluGroovyLangUtils.noException(closure)
  }

  /**
   * Copy from to to...
   * TODO MED YP: overriding filesystem method for speed issue
   *
   * @return to as a resource
   */
  Resource cp(from, to)
  {
    copyOrMove(from, to, _copyAction)
  }

  /**
   * Move from to to... (rename if file)
   * TODO MED YP: overriding filesystem method for speed issue
   *
   * @return to as a resource
   */
  Resource mv(from, to)
  {
    copyOrMove(from, to, _moveAction)
  }

  private def _copyAction = { Resource from, Resource to ->
    exec(command: ['cp', '-R', from.file.canonicalPath, to.file.canonicalPath])
  }

  private def _moveAction = { Resource from, Resource to ->
    exec(command: ['mv', from.file.canonicalPath, to.file.canonicalPath])
  }

  /**
   * Copy or move... same code except ant action
   *
   * @return to as a resource
   */
  Resource copyOrMove(from, to, Closure action)
  {
    from = toResource(from)

    if(!from.exists())
      throw new FileNotFoundException(from.toString())

    def toIsDirectory = to.toString().endsWith('/')
    to = toResource(to)
    toIsDirectory = toIsDirectory || to.isDirectory()

    if(from.isDirectory())
    {
      // handle case when 'from' is a directory

      // to is an existing file => error
      // cp -R foo foo4
      // cp: foo4: Not a directory
      if(!toIsDirectory && to.exists())
        throw new NotDirectoryException(to.toString())
    }
    else
    {
      // handle case when 'from' is a file

      // to is a non existent directory => error
      // cp foo4 foo8/
      // cp: directory foo8 does not exist
      if(toIsDirectory && !to.exists())
        throw new FileNotFoundException(to.toString())
    }

    // to is an existent directory => copy inside directory
    if(toIsDirectory)
      to = to.createRelative(from.filename)

    mkdirs(to.parentResource)

    action(from, to)

    return to
  }
}

class MBeanServerConnectionCategory
{
  static def getAttribute(MBeanServerConnection self, objectName, String attribute)
  {
    objectName = toObjectName(objectName)

    return self.getAttribute(objectName, attribute)
  }

  static Map<String, Object> getAttributes(MBeanServerConnection self, objectName, attributes)
  {
    objectName = toObjectName(objectName)

    attributes = attributes as String[]

    Map<String, Object> res = [:]

    self.getAttributes(objectName, attributes)?.each { Attribute attribute ->
      res[attribute.name] = attribute.value
    }

    return res
  }

  static def invoke(MBeanServerConnection self, objectName, String methodName, parameters)
  {
    objectName = toObjectName(objectName)

    def info = self.getMBeanInfo(objectName)

    def operations = info.getOperations().findAll {
      it.name == methodName && it.signature.size() == parameters.size()
    }

    // if there is only one method with the given name the same number of parameters then we can call
    // it
    if(operations.size() != 1)
    {
      if(operations)
        throw new UnsupportedOperationException("cannot use this form of invoke for overloaded methods")
      else
        throw new NoSuchMethodException(methodName)
    }

    self.invoke(objectName, methodName, parameters as Object[], operations[0].signature.type as String[])
  }

  private static ObjectName toObjectName(objectName)
  {
    if(!(objectName instanceof ObjectName))
      objectName = new ObjectName(objectName.toString())

    return objectName
  }
}
