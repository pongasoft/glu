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

package org.linkedin.glu.provisioner.core.graph

/**
 * A node in a graph
 *
 * author:  Riccardo Ferretti
 * created: Sep 15, 2009
 */
public class GraphNode <T extends Object> {

  /**
   * The value of the node
   */
  final T value

  /**
   * The properties associated to this node
   */
  final Map<String, String> props

  /**
   * Create a node of a graph that doesn't have property
   */
  def GraphNode(T value)
  {
    this(value, Collections.emptyMap())
  }

  /**
   * Create a node of the graph with the given properties.
   * The property keys are not case sensitive.
   */
  def GraphNode(T value, Map<String, String> props)
  {
    this.value = value
    this.props = [:]
    props.each { k, v ->
      this.props[k.toLowerCase()] = v
    }
  }

  def accept(Closure cl)
  {
    cl(this)
  }

  T getValue()
  {
    return value
  }

  /**
   * The property keys are not case sensitive.
   */
  String get(String key)
  {
    return props[key.toLowerCase()]
  }

  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!o || getClass() != o.class) return false;

    GraphNode graphNode = (GraphNode) o;

    if (value != graphNode.value) return false;
    if (props?.size() != graphNode.props?.size()) return false;
    props.each {k, v ->
      if (v != graphNode.props?.get(k)) return false
    }      
    return true;
  }

  int hashCode()
  {
    int result;

    result = (value?.hashCode() ?: 0);
    result = 31 * result + (props?.hashCode() ?: 0);
    return result;
  }

  String toString ()
  {
    "GraphNode{value=${value}, props=${props}}"
  }
}