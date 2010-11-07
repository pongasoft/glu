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

package org.linkedin.glu.provisioner.impl.planner

import org.linkedin.glu.provisioner.core.environment.Installation

/**
 * Describes the delta between two installations
 *
 * author:  Riccardo Ferretti
 * created: Jul 27, 2009
 */
public class InstallationsDelta
{

  // The _first (old) installation
  private final Installation _first
  // The _second (new) installation
  private final Installation _second


  def InstallationsDelta(args)
  {
    this(args.first, args.second)
  }
  
  def InstallationsDelta(Installation first, Installation second)
  {
    _first = first;
    _second = second;
    if (!_first && !_second)
    {
      throw new IllegalArgumentException("At least one installation must differ from null")
    }
  }

  /**
   * Return the id of the two installations
   */
  final String getId()
  {
    return _first == null ? _second.id : _first.id
  }

  /**
   * Return if the host of the two installations is the same
   */
  boolean isSameHost()
  {
    return isSame('hostname')
  }

  /**
   * Return if the mount point of the two installations is the same
   */
  boolean isSameMount()
  {
    return isSame('mount')
  }

  /**
   * Return if the state of the installations is the same
   */
  boolean isSameState()
  {
    return isSame('state')
  }

  /**
   * Return if the name of the two installations is the same
   */
  boolean isSameName()
  {
    return isSame('name')
  }

  /**
   * Return if the parent of the two installations is the same
   */
  boolean isSameParent()
  {
    return isSame('parent')
  }

  /**
   * Return if the script used by the two installations is the same
   */
  boolean isSameScript()
  {
    return isSame('gluScript')
  }

  /**
   * Generic call to compare 2 fields
   */
  boolean isSame(String field)
  {
    if(field == 'parent')
    {
      if (!_first || !_second) return false
      if ((_first && _first.parent == null) &&
          (_second && _second.parent == null)) return true
      return new InstallationsDelta(_first?.parent, _second?.parent).isSameInstallation()
    }
    
    return _first?."${field}" == _second?."${field}"
  }

  /**
   * Return if the only difference between the installations is in the
   * properties (that is, they are the same installation but with different
   * properties) or in the state.
   * Also two installations are considered to be the same if the only difference
   * in their parents is the properties
   */
  boolean isSameInstallation()
  {
    isSameInstallation([])
  }

  /**
   * Return if the only difference between the installations is in the
   * properties (that is, they are the same installation but with different
   * properties) or in the state.
   * Also two installations are considered to be the same if the only difference
   * in their parents is the properties
   */
  boolean isSameInstallation(excludes)
  {
    def includes = ['hostname', 'mount', 'name', 'gluScript', 'parent'] - excludes

    !includes.any { !isSame(it) }
  }

  /**
   * Return if the properties of the two installations are the same
   */
  boolean areSameProps()
  {
    areSameProps([])
  }

  /**
   * Return if the properties of the two installations are the same
   * @param excludes an array of keys to exclude from the comparison
   */
  boolean areSameProps(excludes)
  {
    if (!_first || !_second) return false
    if (subset(_first.props.keySet(), excludes) != subset(_second.props.keySet(), excludes)) return false

    !_first.props.any { key, value ->
      if(excludes?.contains(key))
        return false
      else
        return _second.props[key] != value
    }
  }

  private def subset(set, excludes)
  {
    return new HashSet(set) - excludes
  }

  Installation getFirst()
  {
    return _first
  }

  Installation getSecond()
  {
    return _second
  }

  /**
   * Return if the delta represents a new installation
   */
  Boolean isInstall()
  {
    return _first && (!second)
  }

  /**
   * Return if the delta represents an installation to remove
   */
  Boolean isUninstall()
  {
    return (!_first) && second
  }

  /**
   * Return if the delta represents an update in an existing installation
   */
  Boolean isUpdate()
  {
    return (_first && _second)
  }
  
 /**
  *  Return the map of properties that differ between the two installations.
  *  If there is no difference (that is, {@link #areSameProps() is <code>true</code>)
  *  it will return an empty map.
  *  Otherwise it returns a map where the key is the name of the property and the value
  *  is another map, where the key is the old value and the value is the new one.
  *  
  */
  Map<String, ModifiedProperty> getModifiedProps()
  {
    if (areSameProps()) return Collections.emptyMap()

    // Compute delta in properties
    // NOTE RF: This is computed everytime the method is called, but I don't think
    // it's a big deal for now 
    ModifiedProperty.compare (_first.props, _second.props)
  }

}