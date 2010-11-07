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


package org.linkedin.glu.agent.api

/**
 *
 *
 * @author ypujante@linkedin.com
 */
def class ScriptExecutionException extends ScriptException
{
  private static final long serialVersionUID = 1L;

  final def script
  final def action
  final def args

  ScriptExecutionException(script, action, args, Throwable cause)
  {
    // dont use args in the message, as args may contain sensitive data
    super("script=${script}, action=${action}".toString(), cause)
    this.script = script
    this.action = action
    this.args = args
  }

  ScriptExecutionException(String message, Throwable cause)
  {
    super(message, cause)
  }

  ScriptExecutionException(String message)
  {
    super(message)
  }

  boolean equals(o)
  {
    if(this.is(o)) return true

    if(!(o instanceof ScriptExecutionException)) return false

    ScriptExecutionException that = (ScriptExecutionException) o

    if(action != that.action) return false
    if(args != that.args) return false
    if(script != that.script) return false

    return true
  }
  int hashCode()
  {
    int result;

    result = script.hashCode();
    result = 31 * result + action.hashCode();
    result = 31 * result + args.hashCode();
    return result;
  }
}
