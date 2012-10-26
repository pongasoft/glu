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

import org.linkedin.glu.utils.io.LimitedInputStream

/**
 * @author yan@pongasoft.com */
public class TestLimitedInputStream extends GroovyTestCase
{
  public void testNoInput()
  {
    def baos = new ByteArrayOutputStream()
    baos.withStream { OutputStream os ->
      def stream = new LimitedInputStream(new ByteArrayInputStream("this is a test".bytes), 0)
      stream.withStream { InputStream is ->
        os << is
      }
      assertEquals(0, baos.toByteArray().size())
      assertEquals(0, stream.numberOfBytesRead)
    }
  }

  public void testShortLimit()
  {
    def baos = new ByteArrayOutputStream()
    baos.withStream { OutputStream os ->
      def stream = new LimitedInputStream(new ByteArrayInputStream("this is a test".bytes), 4)
      stream.withStream { InputStream is ->
        os << is
      }
      assertEquals("this", new String(baos.toByteArray()))
      assertEquals(4, stream.numberOfBytesRead)
    }
  }

  public void testLongLimit()
  {
    def baos = new ByteArrayOutputStream()
    baos.withStream { OutputStream os ->
      def stream = new LimitedInputStream(new ByteArrayInputStream("this is a test".bytes), 100)
      stream.withStream { InputStream is ->
        os << is
      }
      assertEquals("this is a test", new String(baos.toByteArray()))
      assertEquals(14, stream.numberOfBytesRead)
    }
  }

  public void testSingleBytes()
  {
    def baos = new ByteArrayOutputStream()
    baos.withStream { OutputStream os ->
      def stream = new LimitedInputStream(new ByteArrayInputStream("this is a test".bytes), 4)
      stream.withStream { InputStream is ->
        int i
        while((i = is.read()) != -1)
          os.write(i)
      }
      assertEquals("this", new String(baos.toByteArray()))
      assertEquals(4, stream.numberOfBytesRead)
    }
  }

  public void testArrayBytes()
  {
    def baos = new ByteArrayOutputStream()
    baos.withStream { OutputStream os ->
      def stream = new LimitedInputStream(new ByteArrayInputStream("this is a test".bytes), 4)
      stream.withStream { InputStream is ->
        byte[] b = new byte[3]
        assertEquals(3, is.read(b))
        os.write(b)
        assertEquals(1, is.read(b))
        os.write(b, 0, 1)
        assertEquals(-1, is.read(b))
      }
      assertEquals("this", new String(baos.toByteArray()))
      assertEquals(4, stream.numberOfBytesRead)
    }
  }
}