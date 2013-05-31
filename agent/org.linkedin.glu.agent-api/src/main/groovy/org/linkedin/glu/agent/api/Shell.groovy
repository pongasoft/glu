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

package org.linkedin.glu.agent.api

import org.linkedin.util.io.resource.Resource

/**
 * Contains shell related methods. Accessible in any script under the variable <code>shell</code>.
 */
def interface Shell
{
  /**
   * the root of the file system used by this shell. All files created or returned by
   * any methods on this class will be under this root (except for temp files)
   */
  Resource getRoot()

  /**
   * the tmp root of the file system used by this shell. All temp files created will be under
   * this root
   */
  Resource getTmpRoot()

  /**
   * Returns a resource relative to this filesystem.
   *
   * @param file can be of type, <code>java.io.File</code>,
   *        <code>org.linkedin.util.io.resource.Resource</code>, <code>java.net.URI</code>,
   *        <code>java.lang.String</code> (which can be converted into a URI)
   */
  Resource toResource(file)

  /**
   * @param dir starting point for listing ({@see #toResource(Object)} for possible values)
   * @param closure the closure (dsl) containing include(name: '') and exclude(name: '') values
   * @return an array of {@link Resource}
   */
  def ls(dir, Closure closure)

  /**
   * list all the files under the provided directory (or root if not provided) (not recursive)
   * @param dir starting point for listing ({@see #toResource(Object)} for possible values)
   * @return an array of {@link Resource}
   */
  def ls(dir)

  /**
   * list all the files under root only (not recursive)
   * @return an array of {@link Resource}
   */
  def ls()

  /**
   * Same as the other <code>ls</code>, but starts at root
   * @param closure the closure (dsl) containing include(name: '') and exclude(name: '') values
   * @return an array of {@link Resource}
   */
  def ls(Closure closure)

  /**
   * Create the directory as well as its parents if they don't exist
   *
   * @param dir directory to create ({@see #toResource(Object)} for possible values)
   * @return the {@link Resource} representation of the dir
   */
  Resource mkdirs(dir)

  /**
   * Delete the file
   *
   * @param file ({@see #toResource(Object)} for possible values)
   */
  void rm(file)

  /**
   * Delete the directory (recursive)
   *
   * @param dir ({@see #toResource(Object)} for possible values)
   */
  void rmdirs(dir)

  /**
   * Remove all empty directories (that are children (recursively) of the provided directory).
   * @param dir ({@see #toResource(Object)} for possible values)
   */
  void rmEmptyDirs(dir)

  /**
   * creates a file and populate its content with the provided (<code>String</code>) content
   * @param file ({@see #toResource(Object)} for possible values)
   * @param content the content to store in the file
   * @return the {@link Resource} representation of the file
   */
  Resource saveContent(file, String content)

  /**
   * reads the content from the file and return it as a <code>String</code>
   * @param file ({@see #toResource(Object)} for possible values)
   * @return the content
   */
  String readContent(file)

  /**
   * Serializes (java serialization!) the <code>serializable</code> provided and store in the
   * <code>file</code>
   * @param file ({@see #toResource(Object)} for possible values)
   * @param serializable the object that needs to be serialized
   * @return the {@link Resource} representation of the file
   */
  Resource serializeToFile(file, serializable)

  /**
   * Reads the content of the file and deserializes it (java serialization). In order to be
   * deserializable, the classpath must contain the classes required.
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return whatever was deserialized
   */
  def deserializeFromFile(file)

  /**
   * Safe pattern to write to a file: you provide the <code>file</code> and the
   * <code>closure</code> gets called back with an {@link java.io.OutputStream} object as an
   * argument so you don't have to worry about closing it. Note that the implementation use
   * {@link #safeOverwrite(Object, Closure)} under the cover making it safe to overwrite previous
   * content.
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param closure the callback (takes an {@link java.io.OutputStream} as argument)
   * @return whatever the closure returns
   */
  def withOutputStream(file, closure)

  /**
   * Safe pattern to write to a file: you provide the <code>file</code> and the
   * <code>closure</code> gets called back with an {@link java.io.ObjectOutputStream} object as an
   * argument so you don't have to worry about closing it. Note that the implementation use
   * {@link #safeOverwrite(Object, Closure)} under the cover making it safe to overwrite previous
   * content.
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param closure the callback (takes an {@link java.io.ObjectOutputStream} as argument)
   * @return whatever the closure returns
   */
  def withObjectOutputStream(file, closure)

  /**
   * Safe pattern to read from a file: you provide the <code>file</code> and the
   * <code>closure</code> gets called back with an {@link java.io.InputStream} object as an
   * argument so you don't have to worry about closing it.
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param closure the callback (takes an {@link java.io.InputStream} as argument)
   * @return whatever the closure returns
   */
  def withInputStream(file, closure)

  /**
   * Safe pattern to read from a file: you provide the <code>file</code> and the
   * <code>closure</code> gets called back with an {@link java.io.ObjectInputStream} object as an
   * argument so you don't have to worry about closing it.
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param closure the callback (takes an {@link java.io.ObjectInputStream} as argument)
   * @return whatever the closure returns
   */
  def withObjectInputStream(file, closure)

  /**
   * Changes the permission of the <code>file</code>
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param perm expressed in unix fashion (ex: +x)
   * @return the {@link Resource} representation of the file
   */
  def chmod(file, perm)

  /**
   * This convenient call takes a file you want to (over)write to and a closure. The closure is
   * called back with another resource in the same folder that you can write to and then rename
   * the file to the one you wanted. The fact that it is in the same folder ensures that the
   * rename should be quick and not really require any copy thus is less likely to fail.
   * If the rename fails it throws an exception, thus ensuring that if there was an original
   * file it won't be in a partial state.
   *
   * @param file the final file where you want your output to be ({@see #toResource(Object)} for
   *             possible values)
   * @param closure takes a <code>Resource</code> as a parameter that you should use
   * @return whatever the closure returns
   * @throws IOException if cannot rename the file
   */
  def safeOverwrite(file, Closure closure) throws IOException

  /**
   * Every child resource of this resource (recursively) is being passed to the closure. If the
   * closure returns <code>true</code> then it will be part of the result.

   * @param dir where to start the recursion ({@see #toResource(Object)} for possible values)
   * @param closure will be called back for all children (recursively) as a <code>Resource</code>
   *        and should return <code>true</code> or <code>false</code>
   * @return the list of all resources where the closure returned <code>true</code>
   */
  def findAll(dir, closure)

  /**
   * Every child resource of this resource (recursively) is being passed to the closure.
   *
   * @param dir where to start the recursion ({@see #toResource(Object)} for possible values)
   * @param closure will be called back for all children (recursively) as a <code>Resource</code>
   *        and can do whatever it wants with it (return value will be ignored)
   * @return the {@link Resource} representation of the dir
   */
  Resource eachChildRecurse(dir, closure)

  /**
   * Copy from to to...
   *
   * @param from ({@see #toResource(Object)} for possible values)
   * @param to ({@see #toResource(Object)} for possible values)
   * @return the {@link Resource} representation of <code>to</code>
   */
  Resource cp(from, to)

  /**
   * Move from to to... (rename if file)
   *
   * @param from ({@see #toResource(Object)} for possible values)
   * @param to ({@see #toResource(Object)} for possible values)
   * @return the {@link Resource} representation of <code>to</code>
   */
  Resource mv(from, to)

  /**
   * Creates a temp file:
   *
   * @param args.destdir where the file should be created (optional)
   * @param args.prefix a prefix for the file (optional)
   * @param args.suffix a suffix for the file (optional)
   * @param args.deleteonexit if the temp file should be deleted on exit (default to
   * @param args.createParents if the parent directories should be created (default to
   * <code>true</code>)
   * @return a file (note that it is just a file object and that the actual file has *not* been
   *         created and the parents may have been depending on the args.createParents value)
   */
  Resource tempFile(args)

  /**
   * Creates a temp file with all default values
   */
  Resource tempFile()

  /**
   * Create a temporary directory
   */
  Resource createTempDir()

  /**
   * Create a temporary directory
   * @see #tempFile(Object) for details on the options
   */
  Resource createTempDir(args)

  /**
   * @return all the environment properties exposed by the glu agent (most are coming from its
   *         configuration, others are computed (like <code>glu.agent.pid</code> which is the
   *         pid of the agent).
   */
  Map<String, String> getEnv()

  /**
   * Try to guess the mime types of a given file
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return a list of mime types (strings)
   */
  Collection<String> getMimeTypes(file)

  /**
   * Unzips the provided file in a temporary location
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return the location of the temporary location (as a <code>Resource</code>)
   */
  Resource unzip(file)

  /**
   * Unzips the provided file in the provided location
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param toDir ({@see #toResource(Object)} for possible values)
   * @return toDir (as a <code>Resource</code>)
   */
  Resource unzip(file, toDir)

  /**
   * If neither <code>tarFile</code> nor <code>tarDir</code> is provided, then the resulting
   * tar file will be created in a temporary directory.
   *
   * @param args.dir the directory to tar ({@see #toResource(Object)} for possible values)
   * @param args.tarFile the resulting tar file name/location ({@see #toResource(Object)}
   *                     for possible values) (optional) (cannot be used with <code>tarDir</code>)
   * @param args.tarDir the directory to store the resulting tar file ({@see #toResource(Object)}
   *                    for possible values) (optional) (cannot be used with <code>tarFile</code>)
   * @param args.compression what kind of compression to use (ex: gzip, bzip2)
   * @param args.includeRoot a boolean to include the root (args.dir) in the tar file or not
   *                         (default to <code>true</code>)
   * @return the resulting tar file resource
   */
  Resource tar(args)

  /**
   * Untars the provided file in a temporary location. Note that the implementation will try
   * to detect if the file is also gziped and unzip it first (equivalent to <code>tar -zxf</code>)
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return the location of the temporary location (as a <code>Resource</code>)
   */
  Resource untar(file)

  /**
   * Untars the provided file in the provided location. Note that the implementation will try
   * to detect if the file is also gziped and unzip it first (equivalent to <code>tar -zxf</code>)
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return toDir (as a <code>Resource</code>)
   */
  Resource untar(file, toDir)

  /**
   * Gunzips the provided file in a temporary location
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return the location of the temporary location (as a <code>Resource</code>)
   */
  Resource gunzip(file)

  /**
   * Gunzips the provided file in the provided location
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param toDir ({@see #toResource(Object)} for possible values)
   * @return toDir (as a <code>Resource</code>)
   */
  Resource gunzip(file, toFile)

  /**
   * Compresses the provided file (in the same folder)
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return the gziped file or the original file if not zipped (as a <code>Resource</code>)
   */
  Resource gzip(file)

  /**
   * Compresses the provided file as <code>toFile</code>
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param toFile ({@see #toResource(Object)} for possible values)
   * @return toFile (as a <code>Resource</code>)
   */
  Resource gzip(file, toFile)

  /**
   * Compresses each file in a folder
   *
   * @param fileOrFolder a file (behaves like {@link #gzip(Object)}) or a folder
   *                     ({@see #toResource(Object)} for possible values)
   * @param recurse if <code>true</code> then recursively process all folders
   * @return a map with key is resource and value is delta in size (original - new) (note that it
   * contains only the resources that are modified!)
   */
  Map gzipFileOrFolder(fileOrFolder, boolean recurse)

  /**
   * untar + decrypt using the encryption keys (encrytion keys are automatically provided by glu
   * and are available in any glu script with <code>args.encriptionKeys</code>)
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @param toDir ({@see #toResource(Object)} for possible values)
   * @param encryptionKeys
   * @return toDir (as a <code>Resource</code>)
   */
  Resource untarAndDecrypt(file, toDir, encryptionKeys)

  /**
   * Exporting ant access to the shell to run any <code>ant</code> command.
   *
   * @return whatever the closure returns
   */
  def ant(Closure closure)

  /**
   * Fetches the file pointed to by the location. If the location is already a {@link File} then
   * simply returns it. The location can be a <code>String</code> or <code>URI</code> and must
   * contain a scheme. Example of locations: <code>http://locahost:8080/file.txt'</code>,
   * <code>file:/tmp/file.txt</code>, <code>ivy:/org.linkedin/util-core/1.0.0</code>.
   *
   * @param location the location you want to fetch (usually remote)
   * @return where the location was fetched (locally) (as a <code>Resource</code>)
   */
  Resource fetch(location)

  /**
   * Fetches the file pointed to by the location. The location can be <code>File</code>,
   * a <code>String</code> or <code>URI</code> and must contain a scheme. Example of locations:
   * <code>http://locahost:8080/file.txt'</code>, <code>file:/tmp/file.txt</code>,
   * <code>ivy:/org.linkedin/util-core/1.0.0</code>. The difference with the other fetch method
   * is that it fetches the file in the provided destination rather than in the tmp space.
   *
   * @param location the location you want to fetch (usually remote)
   * @param destination ({@see #toResource(Object)} for possible values)
   * @return where the location was fetched (locally) (as a <code>Resource</code>) (if
   *         <code>destination</code> is a file then returns <code>destination</code> as a
   *         <code>Resource</code> otherwise it will be a <code>Resource</code> inside the
   *         <code>destination</code> folder.
   */
  Resource fetch(location, destination)

  /**
   * Returns the content of the location as a <code>String</code> or
   * <code>null</code> if the location is not reachable
   * @param location the location you want to fetch (usually remote)
   * @return the content as a <code>Strint</code> or <code>null</code>
   */
  String cat(location)

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

  /**
   * Similarly to the unix grep command, checks the location one line at a time and returns
   * all the lines which matches the pattern.
   *
   * @param location (see {@link #fetch(Object)} for possible values)
   * @param pattern regular expression (see java pattern)
   * @return a list of strings
   */
  Collection<String> grep(location, pattern)

  /**
   * Similarly to the unix grep command, checks the location one line at a time and returns
   * all the lines which matches the pattern
   *
   * @param location (see {@link #fetch(Object)} for possible values)
   * @param pattern regular expression (see java pattern)
   * @param options options to the command:
   *   <code>out</code>: an object to output the lines which match (default to [])
   *   <code>count</code>: returns the count only (does not use out)
   *   <code>maxCount</code>: stop reading after <code>maxCount</code> matches
   * @return depends on <code>options</code>
   */
  def grep(location, pattern, options)

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   *
   * @param executable ({@see #toResource(Object)} for possible values)
   * @param executableArgs either a <code>String</code> or a <code>Collection<String></code>
   * @return whatever output the shell command returns
   */
  String exec(executable, executableArgs)

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   */
  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   *
   * @param executable ({@see #toResource(Object)} for possible values)
   * @param executableArgs as a vararg of executable args
   * @return whatever output the shell command returns
   */
  String exec(executable, Object... executableArgs)

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   *
   * @param command the full command (including the arguments) as a <code>String</code>
   * @return whatever output the shell command returns
   */
  String exec(String command)

  /**
   * Executes a shell command... the command will be delegated straight to shell and the output of
   * the shell command is returned.
   *
   * @param command the full command (including the arguments) as a <code>Collection</code>
   * @return whatever output the shell command returns
   */
  String exec(Collection command)

  /**
   * More generic form of the exec call which allows you to configure what you provide and what you
   * expect. This call is blocking (unless you request a stream for the result).
   *
   * - Note that if you provide a closure for <code>args.stdout</code> or
   * <code>args.stderr</code> it will be executed in a separate thread (in order to avoid
   * blocking indefinitely).
   *
   * - Note that if you request <code>stdout</code>, <code>stderr</code> or
   * <code>all</code> for the result of this call, <code>stdout</code> and <code>stderr</code> are
   * converted to a <code>String</code> using (by default) the "UTF-8" charset and all lines
   * are terminated with "\n" and the last "\n" is removed. Use <code>stdoutBytes</code> or
   * <code>stderrBytes</code> if you wish to get the bytes directly
   *
   * - Note that if you request <code>stream</code>, then the call return immediately
   * (it is non blocking in this case) and you get a single <code>InputStream</code>
   * which multiplexes stdout/stderr and exit value (see <code>MultiplexedInputStream</code> for
   * details). In this case you should make sure to read the entire stream and properly close it
   * as shown in the following code example:
   *
   * InputStream stream = shell.exec(command: 'xxx', res: 'stream')
   * try
   * {
   *   // read stream
   * }
   * finally
   * {
   *   stream.close()
   * }
   *
   * - Note that if you request <code>stdoutStream</code> or <code>stderrStream</code>, then the
   * call return immediately (it is non blocking in this case) and you get the stream you requested.
   * In this case you should make sure to read the entire stream and properly close it as
   * shown in the following code example:
   *
   * InputStream stream = shell.exec(command: 'xxx', res: 'stdoutStream')
   * try
   * {
   *   // read stream
   * }
   * finally
   * {
   *   stream.close()
   * }
   *
   *
   * @param args.command the command to execute. It will be delegated to the shell so it should
   *                     be native to the OS on which the agent runs (either a <code>String</code>
   *                     or a <code>Collection</code>) (required)
   * @param args.pwd the directory from which the command will be run (optional, will
   *                 default to the "current" directory)
   * @param args.env a map (<code>String</code>, <code>String</code>) containing environment
   *                 variables to be passed to sub-process. If the value is <code>null</code>,
   *                 it will remove it from the inherited environment variables. (optional)
   * @param args.inheritEnv a boolean to determine if the environment variables in effect in the
   *                        agent will be passed down to the sub-process (optional, default to
   *                        <code>true</code>)
   * @param args.stdin any input that can "reasonably" be converted into an
   *                   <code>InputStream</code>) to provide to the command line execution
   *                   (optional, default to no stdin)
   * @param args.stdout <code>Closure</code> (if you want to handle it yourself),
   *                    <code>OutputStream</code> (where stdout will be written to),
   *                    (optional: depends on args.res)
   * @param args.stderr <code>Closure</code> (if you want to handle it yourself),
   *                    <code>OutputStream</code> (where stdout will be written to),
   *                    (optional: depends on args.res)
   * @param args.redirectStderr <code>boolean</code> to redirect stderr into stdout
   *                            (optional, default to <code>false</code>). Note that this can also
   *                            be accomplished with the command itself with something like "2>&1"
   * @param args.failOnError do you want the command to fail (with an exception) when there is
   *                         an error (default to <code>true</code>)
   * @param args.res what do you want the call to return
   *                 <code>stdout</code>, <code>stdoutBytes</code>, <code>stdoutStream</code>
   *                 <code>stderr</code>, <code>stderrBytes</code>, <code>stderrStream</code>
   *                 <code>all</code>, <code>allBytes</code> (a map with 3 parameters, exitValue, stdout, stderr)
   *                 <code>exitValue</code>,
   *                 <code>stream</code>
   *                 (default to <code>stdout</code>)
   * @return whatever is specified in args.res
   */
  def exec(Map args)

  /**
   * Demultiplexes the exec stream as generated by {@link #exec(Map)} when <code>args.res</code> is
   * <code>stream</code>. The following is equivalent:
   *
   * OutputStream myStdout = ...
   * OutputStream myStderr = ...
   *
   * exec(command: xxx, stdout: myStdout, stderr: myStderr, res: exitValue)
   *
   * is 100% equivalent to:
   *
   * demultiplexExecStream(exec(command: xxx, res: stream), myStdout, myStderr)
   *
   * @param execStream the stream as generated by {@link #exec(Map)}
   * @param stdout the stream to write the output (optional, can be <code>null</code>)
   * @param stderr the stream to write the error (optional, can be <code>null</code>)
   * @return the value returned by the executed subprocess
   * @throws org.linkedin.glu.agent.api.ShellExecException when there was an error executing the
   *         shell script and <code>args.failOnError</code> was set to <code>true</code>
   */
  def demultiplexExecStream(InputStream execStream,
                            OutputStream stdout,
                            OutputStream stderr)

  /**
   * Shortcut/More efficient implementation of the more generic {@link #chmod(Object, Object)} call
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return file as a <code>Resource</code>
   */
  Resource chmodPlusX(file)

  /**
   * Changes the permission of the <code>dir</code> and recursively
   *
   * @param dir ({@see #toResource(Object)} for possible values)
   * @param perm expressed in unix fashion (ex: +x)
   * @return the {@link Resource} representation of the dir
   */
  Resource chmodRecursive(dir, perm)

  /**
   * Invokes the closure with an <code>MBeanServerConnection</code> to the jmx control running
   * on the vm started with the provided pid. The closure will be invoked with <code>null</code>
   * if cannot determine the process.
   *
   * @param pid the pid of the process you want to get an mbean connection
   * @param closure will be called back with a <code>MBeanServerConnection</code> (which will be
   * <code>null</code> if cannot connect)
   * @return whatever the closure returns
   */
  def withMBeanServerConnection(pid, Closure closure)

  /**
   * Waits for the condition to be <code>true</code> no longer than the timeout. <code>true</code>
   * is returned if the condition was satisfied, <code>false</code> otherwise (if you specify
   * noException)
   * @param args.timeout how long max to wait (any value convertible to a <code>Timespan</code>)
   * @param args.heartbeat how long to wait between calling the condition (any value convertible to
   *                       a <code>Timespan</code>)
   * @param args.noException to get <code>false</code> instead of an exception
   * @param condition the closure should return <code>true</code> if the condition is satisfied,
   *                  <code>false</code> otherwise
   * @return <code>true</code> if condition was satisfied within the timeout, <code>false</code>
   *         otherwise (if <code>args.noException is provided</code> otherwise an exception will
   *         be raised)
   */
  boolean waitFor(args, Closure condition)

  /**
   * Shortcut when no args
   * @see #waitFor(Object, Closure)
   */
  boolean waitFor(Closure condition)

  /**
   * Tail the file
   *
   * @params file the file to tail ({@see #toResource(Object)} for possible values)
   * @params maxLine the number of lines maximum to read
   *
   * @return the input stream to read the content of the tail
   */
  InputStream tail(file, long maxLine)

  /**
   * Tail the file
   *
   * @params args.location the location of the file to tail ({@see #toResource(Object)} for possible values)
   * @params args.maxLine the number of lines maximum to read (-1 for the entire file)
   * @params args.maxSize the maximum size to read (-1 for the entire file)
   *
   * @return the input stream to read the content of the tail
   */
  InputStream tail(args)

  /**
   * @return <code>true</code> if there is a socket open on the server/port combination
   */
  boolean listening(server, port)

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

  /**
   * Processes <code>from</code> through the replacement token mechanism and writes the result to
   * <code>to</code>
   *
   * @param from ({@see #toResource(Object)} for possible values)
   * @param to ({@see #toResource(Object)} for possible values)}
   * @param tokens a map of token
   * @return <code>to</code> as a {@link Resource}
   * @see #replaceTokens(String, Map)
   */
  Resource replaceTokens(def from, def to, Map tokens)

  /**
   * Processes the content to the token replacement method.
   *
   * @see #saveContent(Object, String)
   */
  Resource saveContent(file, String content, Map tokens)

  /**
   * Same as <code>withInputStream</code> but wraps in a reader using a configured charset (defaults
   * to UTF-8).
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return whatever the closure returns
   */
  def withReader(file, Closure closure)

  /**
   * Same as <code>withOutputStream</code> but wraps in a writer using a configured charset (defaults
   * to UTF-8).
   *
   * @param file ({@see #toResource(Object)} for possible values)
   * @return whatever the closure returns
   */
  def withWriter(file, Closure closure)

  /**
   * Runs the closure in a protected block that will not throw an exception but will return
   * <code>null</code> in the case one happens
   *
   * @return whatever the closure returns unless there is an exception in which case it will
   *         return <code>null</code> (and log the exception as a debug message)
   */
  def noException(Closure closure)

  /**
   * Calling this method will force a script failure (will throw an exception)
   */
  void fail(message)
}