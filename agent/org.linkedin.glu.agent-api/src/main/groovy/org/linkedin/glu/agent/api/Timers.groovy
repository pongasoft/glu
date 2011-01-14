/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.agent.api

/**
 * This interface is available from any GLU script using <code>timers</code> property
 *
 * <pre>
 * class MyScript
 * {
 *   def timer1 = {
 *     log.info "hello world"
 *   }
 *
 *   def install = {
 *     timers.scedule(timer: timer1, repeatFrequency: '1m') // property
 *     // timers.sceduleTimer(timer: 'timer1', repeatFrequency: '1m') // also valid (name of the property)
 *     // timers.sceduleTimer(timer: { println 'hello world'}, repeatFrequency: '1m') // invalid (anonymous closure)
 *   }
 *
 *   def uninstall = {
 *     timers.cancel(timer: timer1)
 *   }
 * }
 * </pre>
 * 
 * @author ypujante@linkedin.com */
interface Timers
{
  /**
   * Schedule a timer. Because of the fact that a timer needs to survive an agent restart, it needs
   * to be defined as a property of the GLU script and not an anonymous <code>Closure</code>.
   *
   * @param args.timer there can only be one timer with a given name (note that the timer needs
   * to be a pointer to a <code>Closure</code>, so it can either be the name of the property
   * holding the <code>Closure</code> or directly the property itself)
   * @param args.initialFrequency how long to wait the first time (<code>optional</code>)
   * @param args.repeatFrequency how long to wait after the first time
   * @return a future execution (asynchronous execution, cancellable...)
   */
  FutureExecution schedule(args)

  /**
   * @param args.timer timer
   * @return <code>false</code> if the execution could not be cancelled, typically because it has already completed
   * normally; <code>true</code> otherwise
   */
  boolean cancel(args)
}
