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

/**
 * @author yan@pongasoft.com */
public class TestMultiplexedInputStream extends GroovyTestCase
{
  public void testWith2Streams()
  {
    String s1 = "abcdefghijklmnopqrstuvwxyz"
    String s2 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    (8..100).each { idx ->
      def mis = new MultiplexedInputStream([new ByteArrayInputStream(s1.bytes),
                                            new ByteArrayInputStream(s2.bytes)],
                                           MemorySize.parse(idx as String))

      def text = mis.text

      def numberOfBytesWritten = 0
      // this also make sure that every single thread properly completes
      mis.futureTasks.each { FutureTask ft -> numberOfBytesWritten += ft.get(3, TimeUnit.SECONDS) }
      assertEquals(text.size(), numberOfBytesWritten)

      assertEquals(["0": s1, "1": s2], demultiplex(text))
    }

  }

  /**
   * Demultiplex the text: small state machine
   */
  private def demultiplex(String text)
  {
    def regex = ~/I([0-9])=([0-9]+)/
    
    def map = [:]

    def expectNextLineSize = null
    def expectMatchRegex = true
    def expectEmptyLine = false
    def key

    text.eachLine { line ->

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
    return map
  }
}