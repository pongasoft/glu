/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.agent.impl.script

/**
 * Meta Object Programming methods
 */
interface MOP
{
  /**
   * Wrap the script (add properties and closures)
   *
   * @param args.script the script to wrap
   * @param args.scriptProperties the properties to add
   * @param args.scriptClosures the closures to add
   */
  def wrapScript(args)


  /**
   * Creates a state machine
   *
   * @param args.script the script to wrap
   */
  def createStateMachine(args)
}