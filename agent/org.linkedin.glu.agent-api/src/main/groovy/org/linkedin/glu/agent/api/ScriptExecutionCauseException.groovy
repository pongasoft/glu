/*
 * Copyright (c) 2011 Yan Pujante
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

import java.util.regex.Pattern

/**
 * The purpose of this class is to make sure that the exception is always serializable which
 * unfortunately is not always the case. For example, uncovered with glu-27, groovy throws a
 * <code>MissingPropertyException</code> where one of the field is a <code>Class</code> object.
 * Although the <code>Class</code> object itself is serializable, it is not deserializable
 * without the proper <code>ClassLoader</code>!
 * 
 * @author yan@pongasoft.com */
public class ScriptExecutionCauseException extends ScriptException
{
  private static final long serialVersionUID = 1L;

  private static final Pattern MSG_PATTERN = Pattern.compile(/^\[([^\]]+)\]:.*$/)


  String originalClassname

  ScriptExecutionCauseException(String message)
  {
    super(message)
    if(message)
    {
      def matcher = MSG_PATTERN.matcher(message)
      if(matcher.matches())
        originalClassname = matcher[0][1]
    }
  }

  ScriptExecutionCauseException(Throwable throwable)
  {
    super("[${throwable.class.name}]: ${throwable.message}".toString())
    originalClassname = throwable.class.name
    stackTrace = throwable.stackTrace
    initCause(throwable.cause)
  }

  @Override synchronized Throwable initCause(Throwable throwable)
  {
    if(throwable == null)
      return this;

    if(!(throwable instanceof ScriptExecutionCauseException))
      throwable = new ScriptExecutionCauseException(throwable)

    return super.initCause(throwable)
  }

  static ScriptExecutionCauseException create(Throwable throwable)
  {
    if(throwable)
      return new ScriptExecutionCauseException(throwable)
    else
      return null
  }
}