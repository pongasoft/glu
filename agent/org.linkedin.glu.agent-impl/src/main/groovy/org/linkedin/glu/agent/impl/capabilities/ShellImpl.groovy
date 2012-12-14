/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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


package org.linkedin.glu.agent.impl.capabilities

import eu.medsea.mimeutil.MimeUtil
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

import java.util.concurrent.TimeoutException
import java.util.regex.Pattern
import javax.management.MBeanServerConnection
import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.remote.JMXServiceURL
import org.linkedin.glu.agent.api.ScriptFailedException
import org.linkedin.glu.agent.api.Shell
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.groovy.util.encryption.EncryptionUtils
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.util.lang.MemorySize
import org.apache.tools.ant.filters.ReplaceTokens
import org.apache.tools.ant.filters.ReplaceTokens.Token
import org.linkedin.util.io.resource.Resource
import javax.management.Attribute
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.util.url.QueryBuilder
import org.linkedin.groovy.util.rest.RestException
import org.linkedin.util.reflect.ReflectUtils
import org.linkedin.glu.agent.rest.common.AgentRestUtils
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecutionThreadFactory
import org.linkedin.glu.utils.concurrent.Submitter
import org.linkedin.glu.utils.concurrent.OneThreadPerTaskSubmitter

/**
 * contains the utility methods for the shell
 *
 * @author ypujante@linkedin.com
 */
def class ShellImpl implements Shell
{
  public static final String MODULE = ShellImpl.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private static final String CONNECTOR_ADDRESS =
    "com.sun.management.jmxremote.localConnectorAddress";

  static {
    MimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
  }

  Clock clock = SystemClock.instance()

  // will delegate all the calls to fileSystem
  @Delegate FileSystem fileSystem

  // agent properties
  AgentProperties agentProperties

  /**
   * The charset to use for reading/writing files */
  def charset = 'UTF-8'

  /**
   * Used whe threads are needed
   */
  private Submitter _submitter

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

  Shell newShell(fileSystem)
  {
    return new ShellImpl(fileSystem: fileSystem,
                         agentProperties: agentProperties,
                         charset: charset,
                         clock: clock,
                         submitter: _submitter)
  }

  Map<String, String> getEnv()
  {
    return agentProperties?.exposedProperties ?: [:]
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
    def compression = 'none'

    def mimeTypes = getMimeTypes(file)

    if(mimeTypes.find { it == 'application/x-gzip'})
    {
      compression = 'gzip'
    }

    ant { ant ->
      ant.untar(src: file.file, dest: toDir.file, compression: compression)
    }

    return toDir
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
    
    def gunzipFile = gunzip(file, file.parentResource."${URLEncoder.encode(gunzipFilename)}")

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

    def gzipFile = gzip(file, file.parentResource."${URLEncoder.encode(file.filename)}.gz")

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
    return AntHelper.withBuilder { closure(it) }
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
    AgentRestUtils.demultiplexExecStream(execStream, stdout, stderr)
  }

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  private Throwable doRebuildAgentException(RestException restException)
  {
    Throwable originalException = restException
    try
    {
      def exceptionClass = ReflectUtils.forName(restException.originalClassName)
      originalException = exceptionClass.newInstance([restException.originalMessage] as Object[])

      originalException.setStackTrace(restException.stackTrace)

      if(restException.cause)
        originalException.initCause(doRebuildAgentException(restException.cause))
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled())
      {
        log.debug("Cannot instantiate: ${restException.originalClassName}... ignored", e)
      }
    }

    return originalException
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
    catch (IOException e)
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
  protected String toStringCommandLine(commandLine)
  {
    if(commandLine instanceof GString)
      commandLine = commandLine.toString()

    if(!(commandLine instanceof String))
      commandLine = commandLine.collect { it.toString() }.join(' ')

    return commandLine
  }

  protected InputStream toInputStream(stream)
  {
    if(stream == null)
      return stream

    if(stream instanceof InputStream)
      return stream

    return new ByteArrayInputStream(stream.toString().getBytes(charset))
  }

  private def doExec(commandLine)
  {
    return exec(command: commandLine,
                failOnError: true,
                res: 'all').stdout
  }

  /**
   * Converts the output into a string. Assumes that the encoding is UTF-8. Replaces all line feeds
   * by '\n' and remove the last line feed.
   */
  protected def toStringOutput(byte[] output)
  {
    return output ? new String(output, charset).readLines().join('\n') : ""
  }

  /**
   * Converts the output into a string with no more than maxChars characters. If there are more
   * characters, then display '...' at the end.
   */
  private def toLimitedStringOutput(byte[] output, int maxChars)
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

    ReplaceTokens rt = new ReplaceTokens(new StringReader(input))
    tokens?.each { k,v ->
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
   * Runs the closure in a protected block that will not throw an exception but will return
   * <code>null</code> in the case one happens
   */
  def noException(Closure closure)
  {
    try
    {
      return closure()
    }
    catch(Throwable th)
    {
      if(log.isDebugEnabled())
      {
        log.debug("[ignored] exception", e)
      }
      return null
    }
  }

  /**
   * Calling this method will force a script failure
   */
  void fail(message)
  {
    throw new ScriptFailedException(message?.toString())
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
