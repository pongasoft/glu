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

package test.utils.exceptions

import org.linkedin.glu.utils.exceptions.MultipleExceptions

/**
 * @author yan@pongasoft.com */
public class TestMultipleExceptions extends GroovyTestCase
{
  private class E1 extends Exception
  {
    E1(String s)
    {
      super(s)
    }
  }

  private class E2 extends Exception
  {
    E2(String s, Throwable throwable)
    {
      super(s, throwable)
    }
  }

  public void testMultipleExceptions()
  {
    // nothing happens
    MultipleExceptions.throwIfExceptions("myMessage", null)

    // nothing happens
    MultipleExceptions.throwIfExceptions("myMessage", [])

    shouldFail(E1) { MultipleExceptions.throwIfExceptions("myMessage", [throwE1("e1m")]) }

    try
    {
      MultipleExceptions.throwIfExceptions("myMessage", [throwE1("e1m"), throwE2("e2m", "e2m.e1m")])
      fail("should not be reached...")
    }
    catch(MultipleExceptions e)
    {
      assertEquals("myMessage - Multi[2]...", e.message)
      def more1 = e.cause
      def e1 = more1.cause
      def more2 = e1.cause
      def e2 = more2.cause
      assertEquals("...[1/2] e1m", more1.message)
      assertTrue("e1", e.causes[0].is(e1))
      assertEquals("...[2/2] e2m.e1m", more2.message)
      assertTrue("e2", e.causes[1].is(e2))
    }
  }

  private Throwable throwE1(String message)
  {
    new E1(message)
  }

  private Throwable throwE2(String e2Message, String e1Message)
  {
    try
    {
      throwE1(e1Message)
    }
    catch(Throwable t)
    {
      new E2(e2Message, t)
    }
  }

}
