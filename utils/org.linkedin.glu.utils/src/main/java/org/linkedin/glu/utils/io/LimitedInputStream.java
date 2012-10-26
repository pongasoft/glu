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

package org.linkedin.glu.utils.io;

import org.linkedin.util.lang.MemorySize;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yan@pongasoft.com
 */
public class LimitedInputStream extends InputStream
{
  private final InputStream _inputStream;
  private final long _limit;
  private long _numberOfBytesRead = 0;

  /**
   * Constructor
   */
  public LimitedInputStream(InputStream inputStream, MemorySize limit)
  {
    this(inputStream, limit.getSizeInBytes());
  }

  /**
   * Constructor
   */
  public LimitedInputStream(InputStream inputStream, long limit)
  {
    _inputStream = inputStream;
    _limit = limit;
  }

  public long getNumberOfBytesRead()
  {
    return _numberOfBytesRead;
  }

  @Override
  public int read() throws IOException
  {
    if(_numberOfBytesRead == _limit)
      return -1;

    int res = _inputStream.read();

    if(res != -1)
      _numberOfBytesRead++;

    return res;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException
  {
    if(_numberOfBytesRead == _limit)
      return -1;

    long numberOfBytesToRead = Math.min(len, _limit - _numberOfBytesRead);

    int res = _inputStream.read(b, off, (int) numberOfBytesToRead);

    if(res != -1)
      _numberOfBytesRead += res;

    return res;
  }

  @Override
  public long skip(long n) throws IOException
  {
    long numberOfBytesToSkip = Math.min(n, _limit - _numberOfBytesRead);

    long res = _inputStream.skip(numberOfBytesToSkip);

    _numberOfBytesRead += res;

    return res;
  }

  @Override
  public int available() throws IOException
  {
    return _inputStream.available();
  }

  @Override
  public void close() throws IOException
  {
    _inputStream.close();
  }
}
