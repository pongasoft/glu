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

package org.linkedin.glu.commands.impl

/**
 * @author yan@pongasoft.com */
public abstract class AbstractCommandStreamStorage implements CommandStreamStorage
{
  CommandExecution commandExecution

  @Override
  def withStorageOutput(StreamType streamType, Closure c)
  {
    def output = findStorageOutput(streamType)
    if(output)
      output.withStream { c(it) }
    else
      c(null)
  }

  @Override
  def withStorageInput(StreamType streamType, Closure c)
  {
    def input = findStorageInput(streamType)
    if(input)
      input.withStream { c(it) }
    else
      c(null)
  }
}