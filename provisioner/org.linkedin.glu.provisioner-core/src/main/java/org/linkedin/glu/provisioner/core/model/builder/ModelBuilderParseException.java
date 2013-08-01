/*
 * Copyright (c) 2013 Yan Pujante
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

package org.linkedin.glu.provisioner.core.model.builder;

/**
 * @author yan@pongasoft.com
 */
public class ModelBuilderParseException extends Exception
{
  private static final long serialVersionUID = 1L;

  private final String _excerpt;

  public ModelBuilderParseException(Throwable cause, String excerpt)
  {
    super(cause.getMessage());
    initCause(cause);
    _excerpt = excerpt;
  }

  public String getExcerpt()
  {
    return _excerpt;
  }
}
