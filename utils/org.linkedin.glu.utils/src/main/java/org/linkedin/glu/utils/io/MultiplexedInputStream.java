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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * A multiplexed input stream merges any number of streams (with a given name) into one stream.
 * 
 * The stream looks like this:
 * MISV1.0=[name1]=[name2]=[name3]\n // first line contains the name of the streams
 * \n
 * [nameX]=[sizeInBytes]
 * [bytes (exactly sizeInBytes)]\n
 * \n
 * [nameY]=[sizeInBytes]
 * [bytes (exactly sizeInBytes)]\n
 * \n
 * ...
 *
 * The order in which the "blocks" appear is non deterministic and can vary from call to call.
 * The header contains the name of the streams (separated by an = sign). Since there may never be
 * anything in a stream, it is not guaranteed that it will appear as a "block". But a "block" is
 * guaranteed to have had its name defined in the header...
 *
 * @author yan@pongasoft.com
 */
public class MultiplexedInputStream extends InputStream
{
  public static final String MODULE = MultiplexedInputStream.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final MemorySize DEFAULT_BUFFER_SIZE = MemorySize.parse("4k");

  public static final String CURRENT_VERSION = "MISV1.0";

  public static final byte[] SEPARATOR;

  static
  {
    try
    {
      SEPARATOR = "\n\n".getBytes("UTF-8");
    }
    catch(UnsupportedEncodingException e)
    {
      // should not happen
      throw new RuntimeException(e);
    }
  }

  private final Map<String, InputStream> _inputStreams;

  private Collection<ChannelReaderCallable> _channelReaders;
  private Collection<FutureTask<Long>> _futureTasks;

  private final ByteBuffer _multiplexedBuffer;

  private boolean _closed = false;
  private int _endOfStream = 0;

  /**
   * Constructor
   */
  public MultiplexedInputStream(Collection<InputStream> inputStreams)
  {
    this(inputStreams, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructor
   */
  public MultiplexedInputStream(Collection<InputStream> inputStreams, MemorySize bufferSize)
  {
    this(computeNames(inputStreams), bufferSize);
  }

  private static Map<String, InputStream> computeNames(Collection<InputStream> inputStreams)
  {
    Map<String, InputStream> res = new LinkedHashMap<String, InputStream>();

    int i = 0;

    for(InputStream inputStream : inputStreams)
    {
      res.put("I" + i, inputStream);
      i++;
    }

    return res;
  }

  /**
   * Constructor
   */
  public MultiplexedInputStream(Map<String, InputStream> inputStreams)
  {
    this(inputStreams, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Constructor
   */
  public MultiplexedInputStream(Map<String, InputStream> inputStreams, MemorySize bufferSize)
  {
    _inputStreams = inputStreams;

    int bufferSizeInBytes = (int) bufferSize.getSizeInBytes();

    _multiplexedBuffer = ByteBuffer.allocate(bufferSizeInBytes);

    _futureTasks = new ArrayList<FutureTask<Long>>();
    _channelReaders = new ArrayList<ChannelReaderCallable>();

    StringBuilder header = new StringBuilder(CURRENT_VERSION);

    for(Map.Entry<String, InputStream> entry : _inputStreams.entrySet())
    {
      String name = entry.getKey();
      InputStream inputStream = entry.getValue();

      if(inputStream != null)
      {
        ChannelReaderCallable channelReader =
          new ChannelReaderCallable(name,
                                    ByteBuffer.allocate(bufferSizeInBytes),
                                    Channels.newChannel(inputStream));

        if(channelReader.getMinSize() > bufferSizeInBytes)
          throw new IllegalArgumentException("buffer size ["
                                             + bufferSizeInBytes
                                             + "] is too small and should be at least ["
                                             + channelReader.getMinSize()
                                             + "]");

        _channelReaders.add(channelReader);
        _futureTasks.add(new FutureTask<Long>(channelReader));

        header.append('=');
        header.append(name);
      }
    }

    header.append("\n\n");

    try
    {
      // first we write the header
      byte[] headerAsBytes = header.toString().getBytes("UTF-8");

      if(_multiplexedBuffer.capacity() < headerAsBytes.length)
        throw new IllegalArgumentException("buffer size ["
                                           + bufferSizeInBytes
                                           + "] is too small and should be at least ["
                                           + headerAsBytes.length
                                           + "]");

      _multiplexedBuffer.put(headerAsBytes);
    }
    catch(UnsupportedEncodingException e)
    {
      throw new RuntimeException(e);
    }

    // now that everything has been set up and initialized, we can start all the threads
    for(FutureTask<Long> futureTask : _futureTasks)
    {
      if(futureTask != null)
        new Thread(futureTask).start();
    }
  }

  /**
   * Simple getter to get a hold of the streams (for testing mostly)
   */
  public Map<String, InputStream> getInputStreams()
  {
    return _inputStreams;
  }

  /**
   * For testing purposes
   */
  public Collection<FutureTask<Long>> getFutureTasks()
  {
    return _futureTasks;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException
  {
    synchronized(_multiplexedBuffer)
    {
      try
      {
        while(_multiplexedBuffer.position() == 0 && _endOfStream != 0 && !_closed)
          _multiplexedBuffer.wait();
      }
      catch(InterruptedException e)
      {
        throw new IOException(e);
      }

      if(_closed)
        throw new IOException("closed");

      // nothing else to read... reach end of all streams!
      if(_multiplexedBuffer.position() == 0)
        return -1;

      // makes the buffer ready to read
      _multiplexedBuffer.flip();

      int numberOfBytesToRead = Math.min(len, _multiplexedBuffer.remaining());

      _multiplexedBuffer.get(b, off, numberOfBytesToRead);

      // we now compact the buffer in case not everything was read
      _multiplexedBuffer.compact();

      // let everybody know that there is more room
      _multiplexedBuffer.notifyAll();

      return numberOfBytesToRead;
    }
  }

  @Override
  public int read() throws IOException
  {
    byte[] b = new byte[1];
    int res = read(b);
    if(res == -1)
      return -1;
    else
      return b[0];
  }

  @Override
  public int available() throws IOException
  {
    synchronized(_multiplexedBuffer)
    {
      int available = 0;

      for(ChannelReaderCallable channelReader : _channelReaders)
      {
        if(channelReader != null)
          available += channelReader.available();
      }

      return available;
    }
  }

  @Override
  public void close() throws IOException
  {
    synchronized(_multiplexedBuffer)
    {
      _closed = true;
      // notify everybody that this stream is closed
      _multiplexedBuffer.notifyAll();
    }

    // this is a very "verbose" piece of code that simply calls close on each channel
    // while ensuring it gets called for all of them even if an exception is thrown and if
    // one is thrown, then the first one gets propagated
    Throwable throwable = null;

    for(ChannelReaderCallable channelReader : _channelReaders)
    {
      try
      {
        channelReader.close();
      }
      catch(Throwable e)
      {
        if(throwable != null)
          throwable = e;
        else
        {
          if(log.isDebugEnabled())
            log.debug("ignored exception", e);
        }
      }
    }

    if(throwable != null)
    {
      try
      {
        throw throwable;
      }
      catch(IOException e)
      {
        throw e;
      }
      catch(RuntimeException e)
      {
        throw e;
      }
      catch(Throwable e)
      {
        throw new IOException(e);
      }
    }
  }

  /**
   * The channel will be read in a separate thread in order not to block!
   */
  private class ChannelReaderCallable implements Callable<Long>
  {
    private final String _name;
    private final ByteBuffer _buffer;
    private final ReadableByteChannel _channel;

    private final int _minSize;
    private long _totalNumberOfBytesWritten = 0;

    private ChannelReaderCallable(String name, ByteBuffer buffer, ReadableByteChannel channel)
    {
      _name = name;
      _buffer = buffer;
      _channel = channel;

      // format is <name>=<size>\n<bytes>\n\n with the smallest message containing 1 byte
      _minSize = computeSize(1);

      _endOfStream++;
    }

    public int getMinSize()
    {
      return _minSize;
    }

    private void close() throws IOException
    {
      _channel.close();
    }

    /**
     * Should be called from a synchronized block!
     */
    private int available()
    {
      return computeSize(_buffer.capacity() - _buffer.remaining());
    }

    private int computeSize(int numberOfBytes)
    {
      if(numberOfBytes == 0)
        return 0;

      return computeHeader(numberOfBytes).length + numberOfBytes + SEPARATOR.length;
    }

    private byte[] computeHeader(int numberOfBytesToWrite)
    {
      StringBuilder sb = new StringBuilder(_name);
      sb.append('=');
      sb.append(numberOfBytesToWrite);
      sb.append('\n');

      try
      {
        return sb.toString().getBytes("UTF-8");
      }
      catch(UnsupportedEncodingException e)
      {
        // should not happen
        throw new RuntimeException(e);
      }
    }

    @Override
    public Long call() throws Exception
    {
      try
      {
        // make it ready for "write" (to the buffer)
        _buffer.clear();

        // we loop as long as there is something to read on the channel (or the channel has
        // been closed)
        while(_channel.isOpen() && (_channel.read(_buffer) != -1))
        {
          // javadoc says that the channel could potentially read 0 bytes so we need to make
          // sure there is something to actually write!
          if(_buffer.position() > 0)
            writeToMultiplexBuffer();
        }

        // after the end of the channel there may still be some data in the buffer!
        while(_buffer.position() > 0)
          writeToMultiplexBuffer();
      }
      finally
      {
        synchronized(_multiplexedBuffer)
        {
          _endOfStream--;
          _multiplexedBuffer.notifyAll();
        }
      }

      return _totalNumberOfBytesWritten;
    }

    private void writeToMultiplexBuffer()
      throws InterruptedException, ClosedChannelException
    {
      synchronized(_multiplexedBuffer)
      {
        // we need to wait until there is enough space in the buffer
        while(_multiplexedBuffer.remaining() < _minSize && !_closed)
          _multiplexedBuffer.wait();

        if(_closed)
          throw new ClosedChannelException();

        // make it ready for "read" (from the buffer)
        _buffer.flip();

        // now there is enough space to at least write 1 byte
        int numberOfBytesToWrite = computeNumberOfBytesToWrite(_buffer.remaining(),
                                                               _multiplexedBuffer.remaining());

        // saving the limit to restore it
        int limit = _buffer.limit();

        // we need to write numberOfBytesToWrite
        _buffer.limit(numberOfBytesToWrite);

        byte[] header = computeHeader(numberOfBytesToWrite);

        // number of actual bytes written
        long numberOfBytesWritten = header.length + _buffer.remaining() + SEPARATOR.length;

        // writing the data to the buffer
        _multiplexedBuffer.put(header);
        _multiplexedBuffer.put(_buffer);
        _multiplexedBuffer.put(SEPARATOR); // 2 char

        // adding to the total number of bytes written
        _totalNumberOfBytesWritten += numberOfBytesWritten;

        // reverting to the previous limit
        _buffer.limit(limit);

        // compacting the buffer in case not everything was written
        _buffer.compact();

        // we notify everybody that we have written data
        _multiplexedBuffer.notifyAll();
      }
    }

    private int computeNumberOfBytesToWrite(int numberOfBytesRead, int spaceAvailable)
    {
      int numberOfBytesToWrite = numberOfBytesRead;

      // is there enough space to write everything?
      int size = computeSize(numberOfBytesRead);

      // not enough
      if(size > spaceAvailable)
      {
        // this is an approximation (because we cannot write more than the capacity of the
        // buffer!), but we know it will fit
        numberOfBytesToWrite =
          spaceAvailable - (computeHeader(_multiplexedBuffer.capacity()).length +
                            SEPARATOR.length);

        if(numberOfBytesToWrite < 1)
          numberOfBytesToWrite = 1; // we know that there is at least enough room for 1 byte!
      }

      return numberOfBytesToWrite;
    }
  }
}
