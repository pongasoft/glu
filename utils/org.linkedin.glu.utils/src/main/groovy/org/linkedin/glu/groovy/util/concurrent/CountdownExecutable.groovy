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

package org.linkedin.glu.groovy.util.concurrent

/**
 * Count down to 0 and when reaches 0 executes the closure
 * 
 * @author yan@pongasoft.com */
public class CountdownExecutable implements Countdown
{
  private int _counter = 0

  private final Closure _closure

  CountdownExecutable(Closure closure)
  {
    _closure = closure
  }

  CountdownExecutable(Closure closure, int counter)
  {
    _closure = closure
    _counter = counter
  }

  int inc()
  {
    synchronized(this)
    {
      ++_counter
      return _counter
    }
  }

  def countDown()
  {
    boolean executeClosure

    synchronized(this)
    {
      if(_counter == 0)
        throw new IllegalStateException("already executed")

      executeClosure = (--_counter == 0)
    }

    if(executeClosure)
      _closure()
  }
}