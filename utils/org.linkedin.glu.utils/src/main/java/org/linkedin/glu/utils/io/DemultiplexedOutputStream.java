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
import org.linkedin.util.text.StringSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class DemultiplexedOutputStream extends OutputStream
{
  public static final String MODULE = DemultiplexedOutputStream.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final MemorySize DEFAULT_BUFFER_SIZE = MemorySize.parse("4k");

  public static final String CURRENT_VERSION = "MISV1.0";

  public static final StringSplitter SS = new StringSplitter('=');

  private final Map<String, OutputStream> _outputStreams;
  private final Map<String, WritableByteChannel> _outputChannels;
  private final MemorySize _bufferSize;

  private ByteBuffer _buffer;
  private long _numberOfBytesWritten = 0;

  // the data currently being written needs to be written there
  private WritableByteChannel _currentOutputChannel = null;
  private int _currentNumberOfBytesToWrite = 0;

  private boolean _expectStreamHeader = true;
  private boolean _expectPartHeader = false;

  private boolean _closed = false;

  /**
   * Constructor
   */
  public DemultiplexedOutputStream(Map<String, OutputStream> outputStreams)
  {
    this(outputStreams, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructor
   */
  public DemultiplexedOutputStream(Map<String, OutputStream> outputStreams,
                                   MemorySize bufferSize)
  {
    _outputStreams = new LinkedHashMap<String, OutputStream>(outputStreams);
    _bufferSize = bufferSize;

    _buffer = ByteBuffer.allocate((int) _bufferSize.getSizeInBytes());
    _outputChannels = new LinkedHashMap<String, WritableByteChannel>();

    for(Map.Entry<String, OutputStream> entry : outputStreams.entrySet())
    {
      _outputChannels.put(entry.getKey(), Channels.newChannel(entry.getValue()));
    }
  }

  @Override
  public void write(int b) throws IOException
  {
    if(_closed)
      throw new ClosedChannelException();

    if(_buffer.remaining() == 0)
      throw new IOException("invalid stream detected");

    _buffer.put((byte) b);
    
    processBuffer();
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException
  {
    if(_closed)
      throw new ClosedChannelException();

    while(len > 0)
    {
      if(_buffer.remaining() == 0)
        throw new IOException("invalid stream detected");

      int numberOfBytesToWrite = Math.min(len, _buffer.remaining());

      _buffer.put(b, off, numberOfBytesToWrite);
      processBuffer();
      len -= numberOfBytesToWrite;
      off += numberOfBytesToWrite;
    }
  }

  public long getNumberOfBytesWritten()
  {
    return _numberOfBytesWritten;
  }

  @Override
  public void flush() throws IOException
  {
    for(OutputStream outputStream : _outputStreams.values())
    {
      outputStream.flush();
    }
  }

  @Override
  public void close() throws IOException
  {
    _closed = true;
  }

  private void processBuffer() throws IOException
  {
    boolean needMoreBytes = false;

    _buffer.flip();

    try
    {
      while(_buffer.hasRemaining() && !needMoreBytes)
      {
        if(_expectStreamHeader)
        {
          needMoreBytes = processStreamHeader();
        }
        else
        {
          if(_expectPartHeader)
          {
            needMoreBytes = processPartHeader();
          }
          else
          {
            processData();
          }
        }
      }
    }
    finally
    {
      _buffer.compact();
    }
  }

  private boolean processStreamHeader() throws IOException
  {
    String line = readLine();

    // we need to read more...
    if(line == null)
      return true;

    // we skip empty lines
    if(line.equals(""))
      return false;

    // this should be the header...
    List<String> headerParts = SS.splitAsList(line);

    if(headerParts.size() == 0)
      throw new IOException("invalid stream header: " + line);

    boolean versionChecked = false;

    for(String headerPart : headerParts)
    {
      if(versionChecked)
      {
        if(!_outputChannels.containsKey(headerPart))
        {
          if(log.isDebugEnabled())
            log.debug("output stream " + headerPart + " not provided... swallowing output");
          _outputChannels.put(headerPart, Channels.newChannel(NullOutputStream.INSTANCE));
        }
      }
      else
      {
        if(!headerPart.equals(CURRENT_VERSION))
          throw new IOException("version " + headerPart + " not supported");
        versionChecked = true;
      }
    }

    _expectStreamHeader = false;
    _expectPartHeader = true;

    return false;
  }

  private boolean processPartHeader() throws IOException
  {
    String line = readLine();
    
    // we need to read more...
    if(line == null)
      return true;

    // we skip empty lines
    if(line.equals(""))
      return false;

    List<String> headerParts = SS.splitAsList(line);
    if(headerParts.size() != 2)
      throw new IOException("invalid part header: " + line);

    _currentOutputChannel = _outputChannels.get(headerParts.get(0));
    if(_currentOutputChannel == null)
      throw new IOException("invalid stream: mismatch stream header and part header: " + line);

    try
    {
      _currentNumberOfBytesToWrite = Integer.valueOf(headerParts.get(1));
    }
    catch(NumberFormatException e)
    {
      throw new IOException("invalid stream: part header: " + line + " does not contain a valid size");
    }

    _expectPartHeader = false;
    return false;
  }

  private void processData() throws IOException
  {
    int numberOfBytesInBuffer = _buffer.remaining();

    // all the bytes in the buffer belongs to the current output
    if(numberOfBytesInBuffer <= _currentNumberOfBytesToWrite)
    {
      while(_buffer.hasRemaining())
      {
        int numberOfBytesRead = _currentOutputChannel.write(_buffer);
        _currentNumberOfBytesToWrite -= numberOfBytesRead;
        _numberOfBytesWritten += numberOfBytesRead;
      }
    }
    else
    {
      // only a portion of the bytes in the buffer belongs to the current output
      int limit = _buffer.limit();

      // we read only the portion
      _buffer.limit(_currentNumberOfBytesToWrite + _buffer.position());

      // then we read them
      while(_buffer.hasRemaining())
      {
        int numberOfBytesRead = _currentOutputChannel.write(_buffer);
        _currentNumberOfBytesToWrite -= numberOfBytesRead;
        _numberOfBytesWritten += numberOfBytesRead;
      }

      // we restore the limit
      _buffer.limit(limit);
    }

    if(_currentNumberOfBytesToWrite == 0)
    {
      _currentOutputChannel = null;
      _expectPartHeader = true;
    }
  }

  private String readLine()
  {
    byte[] array = _buffer.array();
    int limit = _buffer.limit();

    int off = _buffer.position();

    for(int i = off; i < limit; i++)
    {
      if(array[i] == '\n')
      {
        _buffer.position(i + 1);
        _numberOfBytesWritten += i - off + 1;
        return new String(array, off, i - off);
      }
    }
    return null;
  }
}
