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

package org.linkedin.glu.orchestration.engine.planner.impl;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class Transition
{
  public static final String INSTALL_SCRIPT_ACTION = "installScript";
  public static final String UNINSTALL_SCRIPT_ACTION = "uninstallScript";

  private final String _entryKey;
  private final String _action;
  private final String _to;
  private final int _distance;
  private final String _key;

  private boolean _virtual = false;

  private Set<String> _executeAfter = new HashSet<String>();
  private Set<String> _executeBefore = new HashSet<String>();

  /**
   * Constructor
   */
  public Transition(String key,
                    String entryKey,
                    String action,
                    String to,
                    int distance)
  {
    _key = key;
    _entryKey = entryKey;
    _action = action;
    _to = to;
    _distance = distance;
  }

  public String getKey()
  {
    return _key;
  }

  public String getEntryKey()
  {
    return _entryKey;
  }

  public String getAction()
  {
    return _action;
  }

  public String getTo()
  {
    return _to;
  }

  public int getDistance()
  {
    return _distance;
  }

  @Override
  public String toString()
  {
    return getKey();
  }

  public void executeAfter(Transition transition)
  {
    if(transition != null)
    {
      _executeAfter.add(transition.getKey());
      transition._executeBefore.add(_key);
    }
  }

  public Set<String> getExecuteAfter()
  {
    return _executeAfter;
  }

  public Set<String> getExecuteBefore()
  {
    return _executeBefore;
  }

  public boolean isVirtual()
  {
    return _virtual;
  }

  public void setVirtual(boolean virtual)
  {
    _virtual = virtual;
  }

  public static String computeTransitionKey(String entryKey, String action, String to, int distance)
  {
    // 'to' serves no purpose in the key...
    StringBuilder sb = new StringBuilder();
    sb.append(entryKey).append(':').append(action).append(':').append(distance);
    return sb.toString();
  }
}
