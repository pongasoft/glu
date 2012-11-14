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

package test.utils.io

import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.util.lang.MemorySize
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import org.linkedin.glu.utils.io.DemultiplexedOutputStream
import org.linkedin.glu.utils.io.EmptyInputStream
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import org.linkedin.util.clock.Chronos
import java.util.concurrent.CancellationException

/**
 * @author yan@pongasoft.com */
public class TestMultiplexedInputStream extends GroovyTestCase
{
  public void testWith2Streams()
  {
    String s1 = "abcdefghijklmnopqrstuvwxyz"
    String s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    (15..100).each { idx ->
      def mis = new MultiplexedInputStream([new ByteArrayInputStream(s1.bytes),
                                            new ByteArrayInputStream(s2.bytes)],
                                           MemorySize.parse(idx as String))

      def text = mis.text

      def numberOfBytesWritten = 0
      // this also make sure that every single thread properly completes
      mis.futureTasks.each { FutureTask ft -> numberOfBytesWritten += ft.get(3, TimeUnit.SECONDS) }
      numberOfBytesWritten += "MISV1.0=I0=I1\n\n".getBytes("UTF-8").length
      assertEquals(text.size(), numberOfBytesWritten)

      assertEquals(["0": s1, "1": s2], demultiplex(text))

      ByteArrayOutputStream baos1 = new ByteArrayOutputStream()
      ByteArrayOutputStream baos2 = new ByteArrayOutputStream()

      def numberOfBytesRead = MultiplexedInputStream.demultiplex(new ByteArrayInputStream(text.bytes),
                                                                 [I0: baos1, I1: baos2],
                                                                 MemorySize.parse(idx as String))

      assertEquals(s1, new String(baos1.toByteArray()))
      assertEquals(s2, new String(baos2.toByteArray()))

      assertEquals(text.size(), numberOfBytesRead)

      baos1 = new ByteArrayOutputStream()
      baos2 = new ByteArrayOutputStream()

      def dmos = new DemultiplexedOutputStream([I0: baos1, I1: baos2],
                                               MemorySize.parse(idx as String))
      text.bytes.each { dmos.write((int) it) }

      assertEquals(s1, new String(baos1.toByteArray()))
      assertEquals(s2, new String(baos2.toByteArray()))
      assertEquals(text.size(), dmos.numberOfBytesWritten)

      baos1 = new ByteArrayOutputStream()
      baos2 = new ByteArrayOutputStream()

      dmos = new DemultiplexedOutputStream([I0: baos1, I1: baos2],
                                           MemorySize.parse(idx as String))
      text.bytes.each {
        def b = new byte[1]
        b[0] = it
        dmos.write(b)
      }

      assertEquals(s1, new String(baos1.toByteArray()))
      assertEquals(s2, new String(baos2.toByteArray()))
      assertEquals(text.size(), dmos.numberOfBytesWritten)

      baos1 = new ByteArrayOutputStream()
      baos2 = new ByteArrayOutputStream()

      // test when empty stream (demultiplex)
      numberOfBytesRead = MultiplexedInputStream.demultiplex(EmptyInputStream.INSTANCE,
                                                             [I0: baos1, I1: baos2],
                                                             MemorySize.parse(idx as String))

      assertEquals("", new String(baos1.toByteArray()))
      assertEquals("", new String(baos2.toByteArray()))
      assertEquals(0, numberOfBytesRead)

      assertTrue("all futures should be done", mis.futureTasks.findAll { !it.isDone() }.isEmpty())
    }
  }

  /**
   * This is to make sure that no matter how the stream terminates, the threads that were spawned
   * will terminate properly.
   */
  public void testThreadsAreClosedOnNormalTermination()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse("30s"))

    def threadControlInputStream = new InputStream() {

      boolean eof = false

      @Override
      int read()
      {
        if(eof) return -1

        int c = tc.blockWithException("bytes") as int
        if(c == -1)
          eof = true

        return c
      }

      @Override
      void close()
      {
        tc.block("close")
      }
    }

    def mis = new MultiplexedInputStream([threadControlInputStream])

    def readStream = new FutureTaskExecution({ mis.text })
    readStream.runAsync()

    tc.unblock("bytes", "a".toCharArray()[0])

    tc.unblock("bytes", -1)

    tc.unblock("close") // make sure that close is called

    def expected = """MISV1.0=I0

I0=1
a

"""
    assertEquals(expected, readStream.get())

    assertTrue("all futures should be done", mis.futureTasks.findAll { !it.isDone() }.isEmpty())
  }

  /**
   * This is to make sure that no matter how the stream terminates, the threads that were spawned
   * will terminate properly.
   */
  public void testThreadsAreClosedOnAbnormalTermination()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse("30s"))

    def threadControlInputStream = new InputStream() {

      boolean eof = false

      @Override
      int read()
      {
        if(eof) return -1

        int c = tc.blockWithException("bytes") as int
        if(c == -1)
          eof = true

        return c
      }

      @Override
      void close()
      {
        tc.unblock("bytes", -1)
      }
    }

    def mis = new MultiplexedInputStream([threadControlInputStream])

    def readStream = new FutureTaskExecution({ mis.text })
    readStream.runAsync()

    tc.unblock("bytes", "a".toCharArray()[0])

    mis.close()

    assertEquals("closed", shouldFailWithCause(IOException) { readStream.get("1s") })

    assertTrue("all futures should be done", mis.futureTasks.findAll { !it.isDone() }.isEmpty())

    mis.futureTasks.each {
      // we make sure that all futures completed properly with no error
      it.get()
    }
  }

  /**
   * This is to make sure that no matter how the stream terminates, the threads that were spawned
   * will terminate properly. When the read is blocking, the future should be cancelled after the
   * grace period
   */
  public void testThreadsFutureGetsCancelledWhenBlocking()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse("30s"))

    def threadControlInputStream = new InputStream() {

      boolean eof = false
      Throwable throwable

      @Override
      int read()
      {
        if(eof) return -1

        try
        {
          int c = tc.blockWithException("bytes") as int
          if(c == -1)
            eof = true
          return c
        }
        catch(Throwable th)
        {
          throwable = th
          throw th
        }
      }

      @Override
      void close()
      {
        // ignore => read will block until unblocked...
      }
    }

    def mis = new MultiplexedInputStream([threadControlInputStream])
    mis.gracePeriodOnClose = Timespan.parse('200')

    def readStream = new FutureTaskExecution({ mis.text })
    readStream.runAsync()

    tc.unblock("bytes", "a".toCharArray()[0])

    Chronos c = new Chronos()

    mis.close()

    // closing the stream will wait at least the grace period because read is blocking and
    // threadControlInputStream.close is doing nothing
    assertTrue(c.tick() >= 200)

    assertEquals("closed", shouldFailWithCause(IOException) { readStream.get("1s") })

    assertTrue("all futures should be done", mis.futureTasks.findAll { !it.isDone() }.isEmpty())

    mis.futureTasks.each { future ->
      // we make sure that the future has been cancelled
      shouldFail(CancellationException) { future.get() }
    }

    // we make sure that the thread was properly interrupted
    shouldFailWithCause(InterruptedException) { throw threadControlInputStream.throwable }
  }

  /**
   * Demultiplex the text: small state machine
   */
  private def demultiplex(String text)
  {
    def regex = ~/I([0-9])=([0-9]+)/
    
    def map = [:]

    def expectHeader = true
    def expectNextLineSize = null
    def expectMatchRegex = false
    def expectEmptyLine = false
    def key

    text.eachLine { line ->

      if(expectHeader)
      {
        expectHeader = false
        expectEmptyLine = true
        assertEquals("MISV1.0=I0=I1", line)
      }
      else
      {
        // should be IX=YYY
        if(expectMatchRegex)
        {
          def m = regex.matcher(line)
          assertTrue(m.matches())
          expectMatchRegex = false
          expectNextLineSize = m[0][2] as int
          key = m[0][1]
        }
        else
        {
          // expecting an empty line
          if(expectEmptyLine)
          {
            assertEquals("", line)
            expectEmptyLine = false
            expectMatchRegex = true
          }
          else
          {
            // expecting a line with YYY bytes
            assertEquals(expectNextLineSize, line.size())
            map[key] = (map[key] ?: "") + line
            expectNextLineSize = null
            expectEmptyLine = true
          }
        }
      }

    }
    return map
  }
}