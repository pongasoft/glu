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

package org.linkedin.glu.provisioner.core.fabric

import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.core.environment.Installation

/**
 * The definition of a fabric
 *
 * author:  Riccardo Ferretti
 * created: Jul 23, 2009
 */
public class FabricDefinition 
{
  /**
   * The name of the fabric
   */
  String name

  /**
   * The installations in the fabric
   */
  List<InstallationDefinition> installations = []


  /**
   * Creates an {@link Environment} from this fabric definition
   */
  Environment toEnvironment()
  {
    def res = [:]
    installations.each { InstallationDefinition inst ->
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
        InstallationDefinition old = stack.pop()
        def iprops = old.software.props + old.instanceProperties
        iprops[InstallationDefinition.INSTALLATION_NAME] = old.name
        Installation toAdd = new Installation(hostname: old.hostname, mount: old.mount,
                                   name: old.name, gluScript: old.software.gluScript,
                                   props: iprops,
                                   parent: res[old.parent?.id])
        assert(!res.containsKey(toAdd.id))
        res[toAdd.id] = toAdd
      }
    }

    assert (installations.size() == res.size())
    return new Environment(name: name, installations: res.values().asList())
  }

  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!o || getClass() != o.class) return false;

    FabricDefinition that = (FabricDefinition) o;

    if (installations != that.installations) return false;
    if (name != that.name) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (name ? name.hashCode() : 0);
    result = 31 * result + (installations ? installations.hashCode() : 0);
    return result;
  }
}