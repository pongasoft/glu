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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class demultiplexes a stream (previously multiplexed with {@link MultiplexedInputStream})
 * and write each stream into the provided output streams
 *
 * @author yan@pongasoft.com
 */
public class OutputStreamsDemultiplexer
{
  public static final String MODULE = OutputStreamsDemultiplexer.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final MemorySize DEFAULT_BUFFER_SIZE = MemorySize.parse("4k");

  public static final String CURRENT_VERSION = "MISV1.0";

  public static final StringSplitter SS = new StringSplitter('=');

  private final InputStream _inputStream;
  private final Map<String, OutputStream> _outputStreams;
  private final Map<String, WritableByteChannel> _outputChannels;
  private final MemorySize _bufferSize;

  private final ReadableByteChannel _inputChannel;
  private ByteBuffer _buffer;
  private long _numberOfBytesRead = 0;

  // the data currently being read needs to be written there
  private WritableByteChannel _currentOutputChannel = null;
  private int _currentNumberOfBytesToRead = 0;

  private boolean _expectStreamHeader = true;
  private boolean _expectPartHeader = false;

  /**
   * Constructor
   */
  public OutputStreamsDemultiplexer(InputStream inputStream,
                                    Map<String, OutputStream> outputStreams)
  {
    this(inputStream, outputStreams, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructor
   */
  public OutputStreamsDemultiplexer(InputStream inputStream,
                                    Map<String, OutputStream> outputStreams,
                                    MemorySize bufferSize)
  {
    _inputStream = inputStream;
    _outputStreams = new LinkedHashMap<String, OutputStream>(outputStreams);
    _bufferSize = bufferSize;

    _inputChannel = Channels.newChannel(_inputStream);
    _buffer = ByteBuffer.allocate((int) _bufferSize.getSizeInBytes());
    _outputChannels = new LinkedHashMap<String, WritableByteChannel>();

    for(Map.Entry<String, OutputStream> entry : outputStreams.entrySet())
    {
      _outputChannels.put(entry.getKey(), Channels.newChannel(entry.getValue()));
    }
  }

  public InputStream getInputStream()
  {
    return _inputStream;
  }

  public Map<String, OutputStream> getOutputStreams()
  {
    return _outputStreams;
  }

  public MemorySize getBufferSize()
  {
    return _bufferSize;
  }

  public long readAll() throws IOException
  {
    _buffer.clear();

    while(_inputChannel.isOpen() && _inputChannel.read(_buffer) != -1)
    {
      if(_buffer.position() > 0)
        processBuffer();
    }

    while(_buffer.position() > 0)
      processBuffer();

    return _numberOfBytesRead;
  }

  public void close() throws IOException
  {
    _inputChannel.close();
  }

  private void processBuffer() throws IOException
  {
    _buffer.flip();

    try
    {
      if(_expectStreamHeader)
      {
        processStreamHeader();
      }
      else
      {
        if(_expectPartHeader)
        {
          processPartHeader();
        }
        else
        {
          processData();
        }
      }
    }
    finally
    {
      _buffer.compact();
    }
  }


  private void processStreamHeader() throws IOException
  {
    String line = readLine();
    if(line == null || line.equals(""))
    {
      // we need to read more... or we skip empty lines
      return;
    }

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
  }

  private void processPartHeader() throws IOException
  {
    String line = readLine();
    if(line == null || line.equals(""))
    {
      // we need to read more... or we skip empty lines
      return;
    }
    List<String> headerParts = SS.splitAsList(line);
    if(headerParts.size() != 2)
      throw new IOException("invalid part header: " + line);

    _currentOutputChannel = _outputChannels.get(headerParts.get(0));
    if(_currentOutputChannel == null)
      throw new IOException("invalid stream: mismatch stream header and part header: " + line);

    try
    {
      _currentNumberOfBytesToRead = Integer.valueOf(headerParts.get(1));
    }
    catch(NumberFormatException e)
    {
      throw new IOException("invalid stream: part header: " + line + " does not contain a valid size");
    }

    _expectPartHeader = false;
  }

  private void processData() throws IOException
  {
    int numberOfBytesInBuffer = _buffer.remaining();

    // all the bytes in the buffer belongs to the current output
    if(numberOfBytesInBuffer <= _currentNumberOfBytesToRead)
    {
      while(_buffer.hasRemaining())
      {
        int numberOfBytesRead = _currentOutputChannel.write(_buffer);
        _currentNumberOfBytesToRead -= numberOfBytesRead;
        _numberOfBytesRead += numberOfBytesRead;
      }
    }
    else
    {
      // only a portion of the bytes in the buffer belongs to the current output
      int limit = _buffer.limit();

      // we read only the portion
      _buffer.limit(_currentNumberOfBytesToRead);

      // then we read them
      while(_buffer.hasRemaining())
      {
        int numberOfBytesRead = _currentOutputChannel.write(_buffer);
        _currentNumberOfBytesToRead -= numberOfBytesRead;
        _numberOfBytesRead += numberOfBytesRead;
      }

      // we restore the limit
      _buffer.limit(limit);
    }

    if(_currentNumberOfBytesToRead == 0)
    {
      _currentOutputChannel = null;
      _expectPartHeader = true;
    }
  }

  private String readLine()
  {
    byte[] array = _buffer.array();
    int limit = _buffer.limit();

    for(int i = 0; i < limit; i++)
    {
      if(array[i] == '\n')
      {
        _buffer.position(i + 1);
        _numberOfBytesRead += i + 1;
        return new String(array, 0, i);
      }
    }
    return null;
  }
}
