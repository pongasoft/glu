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

package org.linkedin.glu.groovy.utils.lang

import org.linkedin.glu.groovy.utils.GluGroovyLangUtils

/**
 * @author yan@pongasoft.com  */
public class ProcessWithResult extends Process
{
  OutputStream outputStream
  InputStream inputStream
  InputStream errorStream
  int exitValue

  @Override
  int waitFor() throws InterruptedException
  {
    exitValue()
  }

  @Override
  int exitValue()
  {
    exitValue
  }

  @Override
  void destroy()
  {
    def cls = [outputStream, inputStream, errorStream].collect { stream ->
      def closure = {
        stream?.close()
      }
      closure
    }

    GluGroovyLangUtils.onlyOneException(cls)
  }
}