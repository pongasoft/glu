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

package org.linkedin.glu.agent.api

/**
 * Contains shell related methods. Accessible in any script under the variable <code>shell</code>.
 */
def interface Shell
{
  /**
   * Fetches the file pointed to by the location. If the location is already a {@link File} then
   * simply returns it. The location can be a <code>String</code> or <code>URI</code> and must
   * contain a scheme. Example of locations: <code>http://locahost:8080/file.txt'</code>,
   * <code>file:/tmp/file.txt</code>, <code>ivy:/org.linkedin/util-core/1.0.0</code>.
   */
  def fetch(location)
}