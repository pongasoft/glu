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
 * @author ypujante@linkedin.com */
class ClosureSystemFilter implements SystemFilter
{
  String name = 'anonymous'
  Closure closure

  ClosureSystemFilter(String name, Closure closure)
  {
    this.name = name
    this.closure = closure
  }

  ClosureSystemFilter(Closure closure)
  {
    this.closure = closure
  }

  String getKind()
  {
    return 'c';
  }

  def toExternalRepresentation()
  {
    return [(kind): name]
  }

  @Override
  String toDSL()
  {
    throw new RuntimeException("Not implemented yet")
  }

  def boolean filter(SystemEntry entry)
  {
    return closure(entry);
  }

  def String toString()
  {
    return "c(${name})".toString()
  }
}
