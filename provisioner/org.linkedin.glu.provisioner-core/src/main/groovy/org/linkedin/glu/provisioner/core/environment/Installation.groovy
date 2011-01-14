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

package org.linkedin.glu.provisioner.core.environment

import org.linkedin.groovy.util.collections.GroovyCollectionsUtils

/**
 * An installation in an environment
 *
 * author:  Riccardo Ferretti
 * created: Jul 23, 2009
 */
public class Installation 
{

  /**
   * The state of the installation
   */
  final String state = 'running'

  /**
   * The transition state (<code>null</code> if not in transition)
   */
  final String transitionState

  /**
   * The host of the installation
   */
  // TODO MED YP: this needs to be renamed agentName
  final String hostname

  /**
   * The mount point of the installation
   */
  final String mount

  /**
   * The actual uri to the agent
   */
  final URI uri

  /**
   * The name of the installation
   */
  final String name

  /**
   * The URI to the glu script associated to the installation
   */
  final URI gluScript

  /**
   * The properties of this installation. They include the properties of the software
   * definition and the installation definition that generated this installation
   */
  final Map<String, String> props = [:]

  /**
   * The parent installation
   */
  final Installation parent

  /**
   * The children of this installation
   */
  private final List<Installation> _children = []

  def Installation(args)
  {
    this(args.hostname,
         args.mount,
         toURI(args.uri),
         args.name,
         toURI(args.gluScript),
         args?.props ?: [:],           // default to empty map
         args.state ? args.state : 'running',
         args.transitionState,
         args.parent)
  }

  private static URI toURI(def uri)
  {
    if(uri == null)
      return null

    if(uri instanceof URI)
      return uri

    return new URI(uri.toString())
  }

  def Installation(String hostname, String mount, URI uri, String name, URI gluScript,
                   Map props, String state, String transitionState, Installation parent)
  {
    this.hostname = hostname;
    this.mount = mount;
    this.uri = uri ?: new URI("https://${hostname}:12906") // default
    this.name = name;
    this.gluScript = gluScript;
    this.props = props;
    this.parent = parent;
    this.state = state ?: 'running'
    this.transitionState = transitionState
    if (parent) parent.addChild(this)
  }

  /**
   * Add a child to this installation (can only be called by the child at init time)
   */
  private void addChild(Installation i)
  {
    if (!_children.contains(i)) _children << i
  }

  public List<Installation> getChildren()
  {
    return Collections.unmodifiableList(_children)
  }

  /**
   * The id of the installation
   */
  String getId()
  {
    // our installations can be uniquely identified by host and mount point
    return "${hostname}:${mount}".toString()
  }


  public String toString ()
  {
    return "Installation [id=${id}, hostname=${hostname}, mount=${mount}, uri=${uri}, name=${name}," +
           " state=${state}, gluScript=${gluScript}, props=${props}, parent=${parent}]".toString()
  }


  /**
   * Note that equals doesn't take into account children, as that is not a property
   * of the installation but organization of multiple installations (basically it
   * affects the environment, but the installation itself is not affected by
   * childrens)
   */
  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!(o instanceof Installation)) return false;

    Installation that = (Installation) o;

    if (gluScript != that.gluScript) return false;
    if (hostname != that.hostname) return false;
    if (mount != that.mount) return false;
    if (uri != that.uri) return false;
    if (name != that.name) return false;
    if (parent != that.parent) return false;
    if (state != that.state) return false;
    if (!GroovyCollectionsUtils.compareIgnoreType(props, that.props)) return false;
    return true;
  }

  int hashCode()
  {
    int result;

    result = (hostname ? hostname.hashCode() : 0);
    result = 31 * result + (mount ? mount.hashCode() : 0);
    result = 31 * result + (uri ? uri.hashCode() : 0);
    result = 31 * result + (name ? name.hashCode() : 0);
    result = 31 * result + (gluScript ? gluScript.hashCode() : 0);
    result = 31 * result + (props ? props.hashCode() : 0);
    result = 31 * result + (parent ? parent.hashCode() : 0);
    return result;
  }
}
