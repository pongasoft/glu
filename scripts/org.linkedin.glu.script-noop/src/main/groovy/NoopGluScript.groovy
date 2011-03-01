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

/**
 * This script does nothing. Note that technically the content of the script could be entirely
 * empty since the engine simply doesn't do anything when a closure is missing. The point of this
 * script is to show the names of all the possible closures (for the standard state machine).
 */
class NoopGluScript
{
  def version = '@script.version@'

  def install = {
    // nothing
  }

  def configure = {
    // nothing
  }

  def start = {
    // nothing
  }

  def stop = {
    // nothing
  }

  def unconfigure = {
    // nothing
  }

  def uninstall = {
    // nothing
  }
}