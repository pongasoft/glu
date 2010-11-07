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

package org.linkedin.glu.provisioner.core.action

/**
 * Contains the informations necessary for a provisioner
 * manager to create and execute actions
 */
public class ActionDescriptor
{

  /**
   * The ID of the descriptor
   */
  String id
  
  /**
   * The name of the action
   */
  String actionName

  /**
   * The type of action
   */
  String type

  /**
   * The parameters of the action
   */
  Map actionParams

  /**
   * The properties of the descriptor
   */
  Map descriptorProperties

  /**
   * A description of the action
   */
  String description


  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!o || getClass() != o.class) return false;

    ActionDescriptor that = (ActionDescriptor) o;

    if (id != that.id) return false;
    if (actionName != that.actionName) return false;
    if (description != that.description) return false;
    if (descriptorProperties != that.descriptorProperties) return false;
    if (type != that.type) return false;
    if (actionParams != that.actionParams) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (id ? id.hashCode() : 0);
    result = 31 * result + (actionName ? actionName.hashCode() : 0);
    result = 31 * result + (type ? type.hashCode() : 0);
    result = 31 * result + (actionParams ? actionParams.hashCode() : 0);
    result = 31 * result + (descriptorProperties ? descriptorProperties.hashCode() : 0);
    result = 31 * result + (description ? description.hashCode() : 0);
    return result;
  }

  public String toString ( )
  {
    return "ActionDescriptor{" +
      "id='" + id + '\'' +
      ", actionName='" + actionName + '\'' +
      ", type='" + type + '\'' +
      ", actionParams=" + actionParams +
      ", descriptorProperties=" + descriptorProperties +
      ", description='" + description + '\'' +
      '}' ;
  }
}