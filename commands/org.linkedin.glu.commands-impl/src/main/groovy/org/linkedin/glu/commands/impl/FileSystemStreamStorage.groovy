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

package org.linkedin.glu.commands.impl

import org.linkedin.util.io.resource.Resource
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils

/**
 * @author yan@pongasoft.com */
class FileSystemStreamStorage extends AbstractCommandStreamStorage
{
  FileSystemCommandExecutionIOStorage ioStorage
  Resource baseDir

  def _streams

  /**
   * Flush all streams we know about
   */
  synchronized void flush()
  {
    _streams.values().out.each { it?.flush() }
  }

  /**
   * Close all streams we know about
   */
  synchronized void close()
  {
    _streams.values().out.each { it?.close() }
  }

  synchronized def getStreams()
  {
    if(!_streams)
    {
      def keys = [StreamType.STDOUT]
      if(commandExecution.hasStdin())
        keys << StreamType.STDIN
      if(!commandExecution.redirectStderr)
        keys << StreamType.STDERR
      _streams =
        GroovyCollectionsUtils.toMapKey(keys) {
          [resource: baseDir.createRelative(ioStorage."${it.toString().toLowerCase()}StreamFileName")]
        }
    }

    _streams
  }

  synchronized OutputStream findStorageOutput(StreamType type)
  {
    def m = streams[type]
    if(m == null)
      return null

    OutputStream stream = m.out
    if(stream == null)
    {
      stream = new BufferedOutputStream(new FileOutputStream(streams[type].resource.file))
      streams[type].out = stream
    }
    return stream
  }

  synchronized InputStream findStorageInput(StreamType type)
  {
    def m = streams[type]
    if(m == null)
      return null

    Resource resource = m.resource
    if(resource.exists())
      return new BufferedInputStream(resource.inputStream)
    else
      return null
  }

  synchronized def findStorageInputWithSize(StreamType type)
  {
    def m = streams[type]
    if(m == null)
      return null

    Resource resource = m.resource
    if(resource.exists())
      return [stream: new BufferedInputStream(resource.inputStream), size: resource.length()]
    else
      return null
  }

  Resource getCommandResource()
  {
    baseDir.createRelative(ioStorage.commandFileName)
  }
}
