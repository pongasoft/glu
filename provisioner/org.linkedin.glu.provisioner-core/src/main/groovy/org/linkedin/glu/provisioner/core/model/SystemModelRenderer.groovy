/*
 * Copyright (c) 2012 Yan Pujante
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
 * @author yan@pongasoft.com */
public interface SystemModelRenderer
{
  /**
   * computes the system id with the idea that if 2 systems have the exact same content, they should
   * generate the same id!
   */
  String computeSystemId(SystemModel model)

  /**
   * Renders a pretty printed version of the model as a string
   */
  String prettyPrint(SystemModel model)

  /**
   * Renders a compact representation of the model as a string (contrary to pretty print, there should
   * be no space, indentation or carriage returns)
   */
  String compactPrint(SystemModel model)

  /**
   * Renders a canonical representation of the model as a string. The canonical representation
   * is a compact representation which is always the same as long as the model is the same.
   * When the format is json, then it means the keys are sorted (the compact representation
   * does not have this guarantee).
   */
  String canonicalPrint(SystemModel model)
}