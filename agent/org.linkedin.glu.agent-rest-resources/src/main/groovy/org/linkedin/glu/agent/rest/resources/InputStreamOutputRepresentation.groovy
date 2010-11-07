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

package org.linkedin.glu.agent.rest.resources

import org.restlet.data.MediaType
import org.restlet.representation.OutputRepresentation

/**
 * Implementation of the class {@link OutputRepresentation} from an {@link InputStream}
 *
 * @author ypujante@linkedin.com */
class InputStreamOutputRepresentation extends OutputRepresentation
{
  private final InputStream _inputStream

  InputStreamOutputRepresentation(InputStream inputStream)
  {
    super(MediaType.APPLICATION_OCTET_STREAM)
    _inputStream = inputStream
  }

  public void write(OutputStream outputStream)
  {
    try
    {
      outputStream << _inputStream
    }
    finally
    {
      _inputStream.close()
    }
  }
}
