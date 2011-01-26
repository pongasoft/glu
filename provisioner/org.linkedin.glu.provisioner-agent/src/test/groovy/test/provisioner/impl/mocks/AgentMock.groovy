/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package test.provisioner.impl.mocks

import java.util.concurrent.atomic.AtomicLong
import org.linkedin.glu.agent.api.Agent

/**
 * Mock of {@link org.linkedin.glu.agent.api.Agent} interface
 *
 * author:  Riccardo Ferretti
 * created: Sep 1, 2009
 */
public class AgentMock implements Agent
{
  private AtomicLong _actionId = new AtomicLong(0)

  private def log = []

  public void clearLog()
  {
    log = []
  }

  public def getLog()
  {
    return log
  }

  public void installScript(Object args)
  {
    log << "installScript ${args}"
  }

  public void uninstallScript(Object args)
  {
    log << "uninstallScript ${args}"
  }

  public String executeAction(Object args)
  {
    log << "executeAction ${args}"
    _actionId.incrementAndGet().toString()
  }

  public void clearError(Object args)
  {
    log << "clearError ${args}"
  }

  public executeCall(Object args)
  {
    log << "executeCall ${args}"
  }

  public getState(Object args)
  {
    log << "getState ${args}"
  }

  public getFullState(Object args)
  {
    log << "getFullState ${args}"
  }

  public boolean waitForState(Object args)
  {
    log << "waitForState ${args}"
    return true;
  }

  public boolean executeActionAndWaitForState(Object args)
  {
    executeAction(args)
    waitForState(args)
  }

  public getMountPoints()
  {
    log << "getMountPoints"
    return ['/'];
  }

  public boolean interruptAction(Object args)
  {
    log << "interruptAction"
    return true
  }

  public getHostInfo()
  {
    log << "getHostInfo"
    return [:]
  }

  public ps()
  {
    log << "ps"
    return [:]
  }

  public void kill(long pid, int signal)
  {
    log << "kill"
  }

  public void sync()
  {
    log << "sync"
  }

  public InputStream tailAgentLog(Object args)
  {
    log << "tailAgentLog(${args})"
    return null
  }

  public getFileContent(Object args)
  {
    log << "getFileContent(${args})"
    return null
  }

  def waitForAction(Object args)
  {
    log << "waitForAction(${args})"
    return null;
  }

  def executeActionAndWait(Object args)
  {
    log << "executeActionAndWait(${args})"
    return null;
  }

  @Override
  int getTagsCount()
  {
    log << "getTagsCount()"
    return 0
  }

  @Override
  boolean hasTags()
  {
    log << "hasTags()"
    return false
  }

  @Override
  Set<String> getTags()
  {
    log << "getTags()"
    return null
  }

  @Override
  boolean hasTag(String tag)
  {
    log << "hasTag(${tag})"
    return false
  }

  @Override
  boolean hasAllTags(Collection<String> tags)
  {
    log << "hasAllTags(${tags})"
    return false
  }

  @Override
  boolean hasAnyTag(Collection<String> tags)
  {
    log << "hasAnyTag(${tags})"
    return false
  }

  @Override
  boolean addTag(String tag)
  {
    log << "addTag(${tag})"
    return false
  }

  @Override
  Set<String> addTags(Collection<String> tags)
  {
    log << "addTags(${tags})"
    return null
  }

  @Override
  boolean removeTag(String tag)
  {
    log << "removeTag(${tag})"
    return false
  }

  @Override
  Set<String> removeTags(Collection<String> tags)
  {
    log << "removeTag(${tags})"
    return null
  }

  @Override
  void setTags(Collection<String> tags)
  {
    log << "setTags(${tags})"
  }
}