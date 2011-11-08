/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.provisioner.core.model

/**
 * Filter by key.
 *
 * @author ypujante@linkedin.com */
class SystemEntryKeyModelFilter implements SystemFilter
{
  Collection<String> keys

  def toExternalRepresentation()
  {
    return [k: keys];
  }

  def boolean filter(SystemEntry entry)
  {
    return keys.contains(entry.key)
  }

  def String getKind()
  {
    return 'k';
  }

  @Override
  String toDSL()
  {
    throw new RuntimeException("Not implemented yet")
  }

  def String toString()
  {
    return "k(${keys.join(',')})".toString();
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;
    if(!(o instanceof SystemEntryKeyModelFilter)) return false;

    SystemEntryKeyModelFilter that = (SystemEntryKeyModelFilter) o;

    if(keys != that.keys) return false;

    return true;
  }

  int hashCode()
  {
    return (keys != null ? keys.hashCode() : 0);
  }
}
