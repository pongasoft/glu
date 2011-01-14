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

package org.linkedin.glu.agent.impl.script

/**
 * @author ypujante@linkedin.com */

class Invocation extends FutureExecutionImpl
{
  def source
  String action
  def actionArgs

  def execute()
  {
    def methods = script.metaClass.respondsTo(script, action, actionArgs)
    if(methods)
    {
      methods[0].invoke(script, actionArgs)
    }
    else
    {
      def closure = script."${action}"
      closure(actionArgs)
    }
  }

  def getScript()
  {
    source.script
  }

  def String toString()
  {
    StringBuilder sb = new StringBuilder(super.toString())

    sb << ", name=${source.name}"
    sb << ", action=${action}"
    sb << ", actionArgs=${actionArgs}"

    return sb.toString()
  }


}