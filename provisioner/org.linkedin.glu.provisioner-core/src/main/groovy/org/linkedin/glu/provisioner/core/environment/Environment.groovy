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

package org.linkedin.glu.provisioner.core.environment

import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.groovy.util.state.StateMachineImpl

/**
 * An environment
 *
 * author:  Riccardo Ferretti
 * created: Jul 23, 2009
 */
public class Environment 
{
  /**
   * Default transitions for the state machine in the script (if the script does not provide its own)
   */
  def static DEFAULT_TRANSITIONS =
  [
    NONE: [[to: 'installed', action: 'install']],
    installed: [[to: 'stopped', action: 'configure'], [to: 'NONE', action: 'uninstall']],
    stopped: [[to: 'running', action: 'start'], [to: 'installed', action: 'unconfigure']],
    running: [[to: 'stopped', action: 'stop']]
  ]

  /**
   * The name of the environment
   */
  final String name

  // The installations in the environment, organized by id
  private final Map<String, Installation> _installationsById
  // The installations in the environment, organized by host
  private final Map<String, List<Installation>> _installationsByHost
  // The state machine that drives this environment
  private final StateMachine _sm

  /**
   * Creates an environment with the given name, which contains the
   * given installations and state machine
   */
  def Environment(args)
  {
    this (args.name.toString(), args.installations, args.sm)
  }


  /**
   * Creates an environment with the given name, which contains the
   * given installations and uses the {@link #DEFAULT_TRANSITIONS}
   * state machine (copied from Agent api)
   */
  def Environment(String name, List<Installation> installations)
  {
    this (name, installations, null)
  }

  /**
   * Creates an environment with the given name, which contains the
   * given installations and uses the given state machine
   */
  def Environment (String name, List<Installation> installations, StateMachine sm)
  {
    _sm = sm ?: new StateMachineImpl(transitions: DEFAULT_TRANSITIONS)
    this.name = name
    def tmpMap = [:]
    def hostTmpMap = [:]
    installations.each { inst ->
      if (tmpMap.containsKey(inst.id))
      {
        throw new RuntimeException ("Cannot have more than one installation with given id (${inst.id})")
      }
      tmpMap[inst.id] = inst
      if (!hostTmpMap.containsKey(inst.hostname))
      {
        hostTmpMap[inst.hostname] = []
      }
      hostTmpMap[inst.hostname] << inst
    }
    hostTmpMap.keys.each {key ->
      hostTmpMap[key] = Collections.unmodifiableList(hostTmpMap[key])
    }

    _installationsById = Collections.unmodifiableMap(tmpMap)
    _installationsByHost = Collections.unmodifiableMap(hostTmpMap)

    checkConsistency()
  }

  /**
   * This method checks that the environment is consistent, according to the following rules:
   * <ul>
   * <li>if an installation has a parent, the parent must be on the same host</li>
   * <li>if an installation has a parent, the parent must be in a state coherent with the child</li>
   * </ul>
   */
  private void checkConsistency()
  {
    List states = _sm.getAvailableStates()
    installations.each { it ->
      if (!states.contains(it.state))
      {
        throw new IllegalStateException("Installation ${it.id} has invalid state (${it.state}). Valid states are ${states}")
      }
      if (it.parent)
      {
        if (it.parent.hostname != it.hostname)
        {
          throw new IllegalStateException("Installation ${it.id} and its parent (${it.parent.id}) should be on the same host")
        }
        // TODO MED RF: we should not rely on the order of the states in the state machine... look at other options
        if (states.indexOf(it.parent.state) > states.indexOf(it.state))
        {
          throw new IllegalStateException("Installation ${it.id} has a state (${it.state})" +
                "that is incompatible with the one of it's parent (${it.parent.id} in state ${it.parent.state})")
        }
      }
    }
  }

  Map<String, Installation> getInstallationsById()
  {
    return _installationsById
  }


  /**
   * @return <code>null</code> if cannot find the installation given its unique id 
   */
  Installation findInstallationById(String id)
  {
    _installationsById[id]
  }

  /**
   * Return the installations on the given host.
   * Return <code>null</code> if no such host exists.
   */
  List<Installation> getInstallationsByHost(String host)
  {
    return _installationsByHost[host]
  }

  /**
   * Return all the installations in the environment
   */
  List<Installation> getInstallations()
  {
    return _installationsById.values().asList()
  }


  /**
   * Return the list of hosts present in this environment 
   */
  List<String> getHosts()
  {
    return Collections.unmodifiableList(_installationsByHost.keySet().asList())
  }

  /**
   * Return another environment which will contain only the installations
   * present on the given hosts.
   * @param environmentName the name of the new environment
   * @param hosts the hosts to filter by
   */
  Environment filterByHost(String environmentName, List<String> hosts)
  {
    filterBy(environmentName) { Installation inst -> hosts.contains(inst.hostname) }
  }

  /**
   * Return another environment which will contain only the installations
   * that have the ginve names.
   * @param environmentName the name of the new environment
   * @param appNames the names of the installations to include in the new env
   */
  Environment filterByApplicationName(String environmentName, String... appNames)
  {
    filterBy(environmentName) { inst -> [*appNames].contains(inst.name) }
  }

  Environment filterBy(String environmentName, Closure cl)
  {
    def res = [:]
    _installationsById.values().each { Installation inst ->
      if (cl(inst))
      {
        def stack = []
        def current = inst
        // put the hierarchy in a stack, until we reach the top or an element
        // that has already been processed
        while (current != null && !res.containsKey(current.id)) {
          stack.push(current)
          current = current.parent
        }
        // add the hierarchy to the list of installations, from parent to child,
        // so that we can add the reference to the parent when adding a child
        while (!stack.isEmpty()) {
          Installation old = stack.pop()
          current = new Installation(hostname: old.hostname, mount: old.mount,
                                     uri: old.uri,
                                     name: old.name, gluScript: old.gluScript,
                                     props: old.props, parent: res[old.parent?.id])
          res[current.id] = current
        }
      }
    }

    // return an environment deduplicate
    return new Environment(environmentName, res.values().asList())
  }


  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!(o instanceof Environment)) return false;

    Environment that = (Environment) o;

    if (_installationsById != that._installationsById ) return false;
    if (name != that.name) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (name ? name.hashCode() : 0);
    result = 31 * result + (_installationsById ? _installationsById.hashCode() : 0);
    result = 31 * result + (_installationsByHost ? _installationsByHost.hashCode() : 0);
    return result;
  }

  public String toString ( )
  {
    return "Environment[name='${name}' , installations=${installations}]" ;
  }
}