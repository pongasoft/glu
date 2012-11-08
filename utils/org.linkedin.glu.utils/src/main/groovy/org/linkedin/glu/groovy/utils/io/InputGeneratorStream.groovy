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

package org.linkedin.glu.groovy.utils.io

import org.linkedin.glu.utils.io.EmptyInputStream
import org.linkedin.glu.utils.core.Sizeable

/**
 * @author yan@pongasoft.com */
public class InputGeneratorStream extends InputStream implements Sizeable
{
  private final Closure _inputStreamFactory

  private InputStream _inputStream = null
  private volatile boolean _isClosed = false
  private volatile long _size = -1

  InputGeneratorStream(Closure inputStreamFactory)
  {
    _inputStreamFactory = inputStreamFactory
  }

  long getSize()
  {
    return _size
  }

  @Override
  int read(byte[] bytes)
  {
    return inputStream.read(bytes)
  }

  @Override
  int read(byte[] bytes, int off, int len)
  {
    return inputStream.read(bytes, off, len)
  }

  @Override
  int read()
  {
    return inputStream.read()
  }

  @Override
  long skip(long l)
  {
    return inputStream.skip(l)
  }

  @Override
  int available()
  {
    return inputStream.available()
  }

  @Override
  synchronized void mark(int i)
  {
    inputStream.mark(i)
  }

  @Override
  synchronized void reset()
  {
    inputStream.reset()
  }

  @Override
  boolean markSupported()
  {
    return inputStream.markSupported()
  }

  /**
   * Blocking call until the factory generates the proper input stream
   */
  private synchronized InputStream getInputStream() throws IOException
  {
    if(_isClosed)
      throw new IOException("closed")

    if(_inputStream == null)
    {
      try
      {
        def input = _inputStreamFactory()

        if(input == null)
        {
          _inputStream = EmptyInputStream.INSTANCE
          _size = 0
        }
        else
        {
          if(input instanceof InputStream)
          {
            _inputStream = input
            if(input instanceof Sizeable)
              _size = input.size
          }
          else
          {
            if(!(input instanceof byte[]))
              input = input.toString().getBytes("UTF-8")

            _inputStream = new ByteArrayInputStream(input)
            _size = input.size()
          }
        }
      }
      catch(IOException e)
      {
        throw e
      }
      catch(Throwable th)
      {
        throw new IOException("issue while generating the input stream", th)
      }
    }

    return _inputStream
  }

  @Override
  synchronized void close()
  {
    _isClosed = true
    _inputStream?.close()
  }
}