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

package org.linkedin.glu.orchestration.engine.commands

import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.glu.commands.impl.CommandStreamStorage
import org.linkedin.glu.utils.io.LimitedOutputStream
import org.apache.tools.ant.util.TeeOutputStream
import org.linkedin.util.lang.MemorySize

/**
 * Helper class to manage stream capture
 *
 * @author yan@pongasoft.com */
public class CommandExecutionStream
{
  MemorySize commandExecutionFirstBytesSize
  StreamType streamType
  boolean captureStream
  CommandStreamStorage storage
  def streams

  private ByteArrayOutputStream _firstBytesOutputStream
  private LimitedOutputStream _limitedOutputStream
  private OutputStream _stream

  def capture(Closure c)
  {
    if(captureStream)
    {
      _firstBytesOutputStream =
        new ByteArrayOutputStream((int) commandExecutionFirstBytesSize.sizeInBytes)
      _limitedOutputStream =
        new LimitedOutputStream(_firstBytesOutputStream, commandExecutionFirstBytesSize)

      storage.withOrWithoutStorageOutput(streamType) { OutputStream stream ->

        if(stream)
          _stream = new TeeOutputStream(stream, _limitedOutputStream)
        else
          _stream = _limitedOutputStream

        streams[streamType.multiplexName] = _stream
      }
    }

    c(this)
  }

  byte[] getBytes()
  {
    _firstBytesOutputStream?.toByteArray()
  }

  Long getTotalNumberOfBytes()
  {
    _limitedOutputStream?.totalNumberOfBytes
  }
}
