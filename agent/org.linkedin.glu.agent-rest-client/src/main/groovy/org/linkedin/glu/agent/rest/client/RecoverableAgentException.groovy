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

package org.linkedin.glu.agent.rest.client

import org.linkedin.glu.agent.api.AgentException
import org.restlet.data.Status

/**
 * @author yan@pongasoft.com */
public class RecoverableAgentException extends AgentException
{
  private static final long serialVersionUID = 1L;

  Status status

  RecoverableAgentException(Status status)
  {
    super(status.toString())
    if(!status.isRecoverableError())
      throw new IllegalArgumentException("${status} is not a recoverable error!")
    this.status = status
  }
}