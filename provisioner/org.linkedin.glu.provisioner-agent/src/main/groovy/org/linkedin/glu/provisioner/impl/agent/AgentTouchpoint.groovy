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

package org.linkedin.glu.provisioner.impl.agent

import org.linkedin.glu.provisioner.core.action.Action
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.action.SimpleAction
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.glu.agent.rest.client.EncryptionKeysProvider
import org.linkedin.glu.agent.api.Agent

/**
 * A touchpoint that interacts with the agent
 *
 * author:  Riccardo Ferretti
 * created: Aug 4, 2009
 */
public class AgentTouchpoint extends BaseAgentTouchpoint
{
  public static final String ID = "agent"

  public static final String MODULE = AgentTouchpoint.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);


  def AgentTouchpoint(AgentFactory factory, EncryptionKeysProvider keyProvider)
  {
    super(ID, factory, keyProvider)
    addActions([
               "noop": this.&getNoopAction,
               "configure" : this.&getConfigureAction,
               "start" : this.&getStartAction,
               "stop" : this.&getStopAction,
               "unconfigure" : this.&getUnconfigureAction,
               ])
  }

  private Action getNoopAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = { /* noop => nothing to do */ }
    def rollback = execute
    return new SimpleAction(ad, execute, rollback)
  }

  private Action getConfigureAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'configure', 'stopped')
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'unconfigure', 'installed')
      }
    }

    return new SimpleAction(ad, execute, rollback)
  }

  private Action getStartAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'start', 'running')
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'stop', 'stopped')
      }
    }

    return new SimpleAction(ad, execute, rollback)
  }

  private Action getStopAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'stop', 'stopped')
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'start', 'running')
      }
    }

    return new SimpleAction(ad, execute, rollback)
  }

  private Action getUnconfigureAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'unconfigure', 'installed')
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'configure', 'stopped')
      }
    }

    return new SimpleAction(ad, execute, rollback)
  }

}