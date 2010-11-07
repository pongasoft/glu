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

/**
 * An installation is a software on a machine.
 * The host + the mount point can identify an installation.
 * 
 * author:  Riccardo Ferretti
 * created: Jul 23, 2009
 */
public class InstallationDefinition 
{
  /**
   * The property where to store the name of the installation
   */
  public static final String INSTALLATION_NAME = 'glu.installation.name'



  
  /**
   * The name of the installation
   */
  String name
  /**
   * The host of the installation
   */
  String hostname
  /**
   * The mount point of the installation
   */
  String mount
  /**
   * The software definition that defines this installation
   */
  SoftwareDefinition software
  /**
   * The parent installation of this installation. Could be <code>null</code>
   */
  InstallationDefinition parent
  /**
   * The properties of this installation
   */
  Map<String, String> instanceProperties = [:]


  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!o || getClass() != o.class) return false;

    InstallationDefinition that = (InstallationDefinition) o;

    if (hostname != that.hostname) return false;
    if (instanceProperties != that.instanceProperties) return false;
    if (mount != that.mount) return false;
    if (name != that.name) return false;
    if (parent != that.parent) return false;
    if (software != that.software) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (name ? name.hashCode() : 0);
    result = 31 * result + (hostname ? hostname.hashCode() : 0);
    result = 31 * result + (mount ? mount.hashCode() : 0);
    result = 31 * result + (software ? software.hashCode() : 0);
    result = 31 * result + (parent ? parent.hashCode() : 0);
    result = 31 * result + (instanceProperties ? instanceProperties.hashCode() : 0);
    return result;
  }

  /**
   * The id of the installation definition
   */
  String getId()
  {
    // our installations can be uniquely identified by host and mount point
    return "${hostname}:${mount}"
  }

  public String toString ( )
  {
    return "InstallationDefinition{" +
            "name='" + name + '\'' +
            ", hostname='" + hostname + '\'' +
            ", mount='" + mount + '\'' +
            ", software=" + software +
            ", parent=" + parent +
            ", instanceProperties=" + instanceProperties +
            '}' ;
  }

}