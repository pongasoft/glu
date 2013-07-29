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

/**
 * @author yan@pongasoft.com */
public class ShellExecException extends ScriptException
{
  private static final long serialVersionUID = 1L;

  int res
  byte[] output
  byte[] error

  ShellExecException()
  {
  }

  ShellExecException(String s)
  {
    super((String) s)
  }

  ShellExecException(Throwable throwable)
  {
    super((Throwable) throwable)
  }

  ShellExecException(s, throwable)
  {
    super(s, throwable)
  }

  String getStringOutput()
  {
    return toStringOutput(output)
  }

  String getStringError()
  {
    return toStringOutput(error)
  }

  /**
   * Converts the output into a string. Assumes that the encoding is UTF-8. Replaces all line feeds
   * by '\n' and remove the last line feed.
   */
  private String toStringOutput(byte[] output)
  {
    if(output == null)
      return null

    return new String(output, "UTF-8").readLines().join('\n')
  }
}