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
 * Base class for logic filter chain
 * @author ypujante@linkedin.com */
abstract class LogicSystemFilterChain implements SystemFilter
{
  Set<SystemFilter> filters = new LinkedHashSet<SystemFilter>()

  def toExternalRepresentation()
  {
    return [(kind): filters.collect { it.toExternalRepresentation()}];
  }

  @Override
  String toDSL()
  {
    "${kind}{${filters*.toDSL().join(';')}}".toString()
  }

  def String toString()
  {
    return "${kind}{${filters*.toString().join(';')}}";
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;
    if(!(o instanceof LogicSystemFilterChain)) return false;

    LogicSystemFilterChain that = (LogicSystemFilterChain) o;

    if(filters != that.filters) return false;

    return true;
  }

  int hashCode()
  {
    return (filters != null ? filters.hashCode() : 0);
  }
}
