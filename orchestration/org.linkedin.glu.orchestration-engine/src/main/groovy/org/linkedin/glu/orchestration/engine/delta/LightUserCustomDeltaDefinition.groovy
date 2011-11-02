/*
 * Copyright (c) 2011 Yan Pujante
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



package org.linkedin.glu.orchestration.engine.delta

import grails.persistence.Entity

/**
 * @author yan@pongasoft.com */
@Entity
class LightUserCustomDeltaDefinition
{
  static constraints = {
    name(nullable: false)
    description(nullable: true)
    username(nullable: true)
    fabric(nullable: true)
  }

  static mapping = {
    table 'user_custom_delta_definition'
  }

  /**
   * Automatically populated by gorm
   */
  Date dateCreated

  /**
   * Automatically populated by gorm
   */
  Date lastUpdated

  /**
   * Represent name from the definition (denormalized)
   */
  String name

  /**
   * Represent description from the definition (denormalized)
   */
  String description

  /**
   * Which user this object belongs to. YP note: not using a foreign key contraint on purpose: the
   * username may be <code>null</code> for 'any' user (custom deltas that belong to any user).
   */
  String username

  /**
   * The fabric to which the filter should only apply (<code>null</code> means any fabric)
   */
  String fabric

  /**
   * If a user creates a custom delta, by default other users can see it too
   */
  boolean shareable = true
}
