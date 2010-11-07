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
 * The definition of a software
 *
 * author:  Riccardo Ferretti
 * created: Jul 23, 2009
 */
public class SoftwareDefinition 
{
  /**
   * The id of the software
   */
  String id

  /**
   * The props of the software
   */
  Map<String, String> props = [:]
  
  /**
   * The glu script associated to this software definition
   */
  URI gluScript

  void setProps(Map<String, String> value)
  {
    props = [:]
    value?.each { k,v ->
      props[k.toString()] = v?.toString()
    }
  }

  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!o || getClass() != o.class) return false;

    SoftwareDefinition that = (SoftwareDefinition) o;

    if (gluScript != that.gluScript) return false;
    if (id != that.id) return false;
    if (props != that.props) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (id ? id.hashCode() : 0);
    result = 31 * result + (props ? props.hashCode() : 0);
    result = 31 * result + (gluScript ? gluScript.hashCode() : 0);
    return result;
  }

  public String toString ( )
  {
    return "SoftwareDefinition{" +
            "id='" + id + '\'' +
            ", props=" + props +
            ", gluScript=" + gluScript +
            '}' ;
  }
}