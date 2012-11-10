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

import org.linkedin.groovy.util.lang.GroovyLangUtils

/**
 * @author yan@pongasoft.com */
class DestroyProcessInputStream extends InputStream
{
  private final InputStream _inputStream
  private final Process _process
  private boolean _destroyed = false

  DestroyProcessInputStream(Process process, InputStream inputStream)
  {
    _process = process
    _inputStream = inputStream
  }

  @Override
  int read(byte[] bytes)
  {
    checkNotDestroyed()
    return _inputStream.read(bytes)
  }

  @Override
  int read(byte[] bytes, int i, int i1)
  {
    checkNotDestroyed()
    return _inputStream.read(bytes, i, i1)
  }

  @Override
  long skip(long l)
  {
    checkNotDestroyed()
    return _inputStream.skip(l)
  }

  @Override
  int available()
  {
    checkNotDestroyed()
    return _inputStream.available()
  }

  @Override
  synchronized void mark(int i)
  {
    _inputStream.mark(i)
  }

  @Override
  synchronized void reset()
  {
    checkNotDestroyed()
    _inputStream.reset()
  }

  @Override
  boolean markSupported()
  {
    _inputStream.markSupported()
  }

  @Override
  int read()
  {
    checkNotDestroyed()
    _inputStream.read()
  }

  synchronized void destroy()
  {
    _destroyed = true
    _process.destroy()
  }

  private synchronized void checkNotDestroyed()
  {
    if(_destroyed)
      throw new DestroyedProcessException()
  }

  @Override
  void close()
  {
    try
    {
      super.close()
    }
    finally
    {
      GroovyLangUtils.noException {
        _process.destroy()
      }
    }
  }

}