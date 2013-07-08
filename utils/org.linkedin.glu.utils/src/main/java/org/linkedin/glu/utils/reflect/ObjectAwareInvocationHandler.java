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

package org.linkedin.glu.utils.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author yan@pongasoft.com
 */
public abstract class ObjectAwareInvocationHandler implements InvocationHandler
{
  private static final Method OBJECT_EQUALS = getObjectMethod("equals", Object.class);

  private static Method getObjectMethod(String name, Class... types)
  {
    try
    {
      return Object.class.getMethod(name, types);
    }
    catch(NoSuchMethodException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public Object invoke(Object o, Method method, Object[] objects) throws Throwable
  {
    if(method.getDeclaringClass() == Object.class)
    {
      if(OBJECT_EQUALS.equals(method))
      {
        return o == objects[0];
      }
      else
        return method.invoke(this, objects);
    }

    return doInvoke(o, method, objects);
  }

  protected abstract Object doInvoke(Object o, Method method, Object[] objects) throws Throwable;
}
