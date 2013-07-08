/*
 * Copyright (c) 2013 Yan Pujante
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

package test.utils.core

import org.linkedin.glu.utils.core.DisabledFeatureProxy
import org.linkedin.glu.utils.core.Sizeable
import org.linkedin.glu.utils.exceptions.DisabledFeatureException
import org.linkedin.util.reflect.ObjectProxyBuilder

/**
 * @author yan@pongasoft.com  */
public class TestDisabledFeatureProxy extends GroovyTestCase
{
  public void testObjectMethods()
  {
    def o = ObjectProxyBuilder.createProxy(new DisabledFeatureProxy("dfp"), Sizeable)

    assertEquals("dfp", shouldFail(DisabledFeatureException) { o.getSize() })

    assertEquals("${DisabledFeatureProxy.class.name}@${Long.toHexString(o.hashCode())}".toString(),
                 o.toString())
    assertTrue(o.equals(o))
    synchronized(o)
    {
      o.notifyAll()
    }
  }
}