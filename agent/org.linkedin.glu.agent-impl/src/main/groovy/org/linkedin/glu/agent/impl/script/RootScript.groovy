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


package org.linkedin.glu.agent.impl.script

/**
 * The root script... there can be only one of those...
 *
 * @author ypujante@linkedin.com
 */
def class RootScript
{
  def static stateMachine =
  [
          NONE: [[to: 'installed', action: 'install']],
          installed: [[to: 'NONE', action: 'uninstall']]
  ]
  
  def rootPath

  def install = {
    rootPath = mountPoint
    shell.mkdirs(rootPath)
  }

  def uninstall = {
    shell.rmdirs(rootPath)
  }

  def createChild = { args ->
    shell.mkdirs(args.mountPoint)
    return args.script
  }

  def destroyChild = { args ->
    shell.rmdirs(args.mountPoint)
  }
}
