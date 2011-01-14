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

package org.linkedin.glu.provisioner.impl.planner

/**
 * Describes a property that has been modified
 *
 * author:  Riccardo Ferretti
 * created: Jul 27, 2009
 */
public class ModifiedProperty 
{

  /**
   * the key of the property
   */
  final String key
  /**
   * The old value of the property
   */
  final String oldValue
  /**
   * the new value of the property
   */
  final String newValue

  def ModifiedProperty(String key, String oldValue, String newValue)
  {
    this.key = key
    this.oldValue = oldValue
    this.newValue = newValue
  }

  def ModifiedProperty(args)
  {
    this (args.key, args.oldValue, args.newValue)
  }
  
  /**
   * Return if the property was not present before and needs to be added now
   */
  Boolean toBeAdded()
  {
    return oldValue == null
  }

  /**
   * Return if the property was present before but needs to be removed now
   */
  Boolean toBeRemoved()
  {
    return newValue == null
  }
  
 /**
  * Compares the given map (key->value) of properties and returns a map of all the
  * different ones, where the key is the key of the property and the value contains
  * information about the change
  */
  static Map<String, ModifiedProperty> compare(Map<String, String> oldProperties,
                                               Map<String, String> newProperties)
  {
    Map<String, ModifiedProperty> res = [:]
    // Adds all the properties that are changing and the ones that are old
    oldProperties.each { key, value ->
      if (!newProperties.containsKey(key))
      {
        res[key] = new ModifiedProperty(key: key, oldValue: value)
      }
      else
      {
        if (!newProperties[key] != value)
        {
          res[key] = new ModifiedProperty(key: key, oldValue: value, newProperties[key])
        }
      }
    }

    // Adds all the properties that are new
    newProperties.each { key, value ->
      if (!oldProperties.containsKey(key))
      {
        res[key] = new ModifiedProperty(key: key, newValue: value)
      }
    }

    return Collections.unmodifiableMap(res)
  }
  
}