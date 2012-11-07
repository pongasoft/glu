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

/**
 * @author yan@pongasoft.com */
class MemoryStreamStorage extends AbstractCommandStreamStorage<MemoryCommandExecutionIOStorage>
{
  ByteArrayOutputStream stdin
  ByteArrayOutputStream stdout = new ByteArrayOutputStream()
  ByteArrayOutputStream stderr

  @Override
  OutputStream findStorageOutput(StreamType streamType)
  {
    findByteArrayOutputStream(streamType)
  }

  @Override
  InputStream findStorageInput(StreamType type)
  {
    ByteArrayOutputStream stream = findByteArrayOutputStream(type)
    if(stream == null)
      return null
    return new ByteArrayInputStream(stream.toByteArray())
  }

  @Override
  def findStorageInputWithSize(StreamType type)
  {
    ByteArrayOutputStream stream = findByteArrayOutputStream(type)
    if(stream == null)
      return null
    def bytes = stream.toByteArray()
    return [stream: new ByteArrayInputStream(bytes), size: bytes.size()]
  }

  ByteArrayOutputStream findByteArrayOutputStream(StreamType type)
  {
    return this."${type.name().toLowerCase()}"
  }
}
