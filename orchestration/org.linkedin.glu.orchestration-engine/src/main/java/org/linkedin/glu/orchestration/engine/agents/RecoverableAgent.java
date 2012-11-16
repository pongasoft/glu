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

package org.linkedin.glu.orchestration.engine.agents;

import org.linkedin.glu.agent.api.Agent;
import org.linkedin.glu.agent.rest.client.RecoverableAgentException;
import org.linkedin.util.clock.Timespan;
import org.linkedin.util.reflect.ObjectProxyInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author yan@pongasoft.com
 */
public class RecoverableAgent extends ObjectProxyInvocationHandler<Agent>
{
  public static final String MODULE = RecoverableAgent.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final int _numRetries;
  private final Timespan _agentRecoveryTimeout;

  /**
   * Constructor
   */
  public RecoverableAgent(Agent agent, int numRetries, Timespan agentRecoveryTimeout)
  {
    super(agent);
    _numRetries = numRetries;
    _agentRecoveryTimeout = agentRecoveryTimeout;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
  {
    for(int i = 0; i < _numRetries; i++)
    {
      try
      {
        return super.invoke(proxy, method, args);
      }
      catch(RecoverableAgentException e)
      {
        log.warn("#" + i + ": detected recoverable error while talking to the agent [ignored]: " + e.getMessage());
        if(log.isDebugEnabled())
          log.debug("Detected recoverable error while talking to the agent [ignored]", e);

        Thread.sleep(_agentRecoveryTimeout.getDurationInMilliseconds());
      }
    }
    
    throw new TooManyRetriesAgentException("too many retries (" + _numRetries + ") for " + method.getName());
  }
}
