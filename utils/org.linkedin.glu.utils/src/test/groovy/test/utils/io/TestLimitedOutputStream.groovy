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

import org.linkedin.glu.utils.io.LimitedOutputStream

/**
 * @author yan@pongasoft.com */
public class TestLimitedOutputStream extends GroovyTestCase
{
  public void testNoOutput()
  {
    def baos = new ByteArrayOutputStream()
    def stream = new LimitedOutputStream(baos, 0)
    stream.withStream { OutputStream os ->
      os << "this is a test"
      os << "a"
      os << "the end"
    }
    assertEquals(0, baos.toByteArray().size())
    assertEquals(22, stream.totalNumberOfBytes)
  }

  public void testShortLimit()
  {
    def baos = new ByteArrayOutputStream()
    def stream = new LimitedOutputStream(baos, 4)
    stream.withStream { OutputStream os ->
      os << "this is a test"
      os << "a"
      os << "the end"
    }
    assertEquals("this", new String(baos.toByteArray()))
    assertEquals(22, stream.totalNumberOfBytes)
  }

  public void testLongLimit()
  {
    def baos = new ByteArrayOutputStream()
    def stream = new LimitedOutputStream(baos, 100)
    stream.withStream { OutputStream os ->
      os << "this is a test"
      os << "a"
      os << "the end"
    }
    assertEquals("this is a testathe end", new String(baos.toByteArray()))
    assertEquals(22, stream.totalNumberOfBytes)
  }

  public void testSingleBytes()
  {
    def bytes = "this is it".bytes

    def baos = new ByteArrayOutputStream()
    new LimitedOutputStream(baos, 4).withStream { OutputStream os ->
      bytes.each { os.write((int) it) }
    }
    assertEquals("this", new String(baos.toByteArray()))
  }

  public void testArrayBytes()
  {
    def bytes = "thisXisXit".bytes

    def baos = new ByteArrayOutputStream()
    new LimitedOutputStream(baos, 4).withStream { OutputStream os ->
      os.write(bytes, 1, 2)
      os.write(bytes, 3, 5)
    }
    assertEquals("hisX", new String(baos.toByteArray()))
  }
}