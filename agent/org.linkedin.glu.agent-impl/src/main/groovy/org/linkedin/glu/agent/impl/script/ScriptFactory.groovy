/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013 Yan Pujante
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
 * Encapsulates the notion of script factory so that we can recreate the scripts at will.
 *
 * @author ypujante@linkedin.com
 */
interface ScriptFactory
{
  /**
   * Creates a script
   */
  def createScript(ScriptConfig scriptConfig)

  /**
   * Called to destroy the script
   */
  void destroyScript(ScriptConfig scriptConfig)

  /**
   * @returns an external representation of the factory
   */
  def toExternalRepresentation()
}