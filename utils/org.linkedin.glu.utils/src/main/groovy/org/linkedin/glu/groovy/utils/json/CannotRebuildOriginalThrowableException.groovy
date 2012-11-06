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

package org.linkedin.glu.groovy.utils.json

/**
 * @author yan@pongasoft.com */
public class CannotRebuildOriginalThrowableException extends Exception
{
  private final String _exceptionClassName
  private final Throwable _deserializationException

  CannotRebuildOriginalThrowableException(String message,
                                          String exceptionClassName,
                                          Throwable deserializationException)
  {
    super("${exceptionClassName}: - ${message} (could not be deserialized [${deserializationException.message}])")

    _exceptionClassName = exceptionClassName
    _deserializationException = deserializationException
  }

  String getExceptionClassName()
  {
    return _exceptionClassName
  }

  Throwable getDeserializationException()
  {
    return _deserializationException
  }
}