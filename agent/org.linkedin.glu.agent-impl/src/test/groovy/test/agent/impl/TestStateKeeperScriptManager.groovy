/*
 * Copyright (c) 2014 Yan Pujante
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

package test.agent.impl

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.impl.capabilities.MOPImpl
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.script.AgentContext
import org.linkedin.glu.agent.impl.script.NoSharedClassLoaderScriptLoader
import org.linkedin.glu.agent.impl.script.ScriptManagerImpl
import org.linkedin.glu.agent.impl.script.StateKeeperScriptManager
import org.linkedin.glu.agent.impl.storage.RAMStorage
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.io.ram.RAMDirectory
import org.linkedin.util.io.resource.internal.RAMResourceProvider

/**
 * @author yan@pongasoft.com  */
public class TestStateKeeperScriptManager extends GroovyTestCase
{
  def ram
  def fileSystem
  def shell
  ScriptManagerImpl sm
  StateKeeperScriptManager sksm
  Map<MountPoint, Object> _invaliStates = [:]
  RAMStorage ramStorage = new RAMStorage() {
    @Override
    def invalidateState(MountPoint mountPoint)
    {
      _invaliStates[mountPoint] = loadState(mountPoint)
      super.invalidateState(mountPoint)
      return "ram:invalidStates:${mountPoint.path}"
    }
  }

  protected void setUp()
  {
    super.setUp();

    ram = new HashSet()
    RAMDirectory ramDirectory = new RAMDirectory()
    RAMResourceProvider rp = new RAMResourceProvider(ramDirectory)
    fileSystem = [
      mkdirs: { dir ->
        ram << dir
        ramDirectory.mkdirhier(dir.toString())
        return rp.createResource(dir.toString())
      },
      rmdirs: { dir ->
        ram.remove(dir)
        ramDirectory.rm(dir.toString())
      },

      getRoot: { rp.createResource('/') },

      getTmpRoot: { rp.createResource('/tmp') },

      newFileSystem: { r, t -> fileSystem }
    ] as org.linkedin.groovy.util.io.fs.FileSystem

    shell = new ShellImpl(fileSystem: fileSystem)

    def rootShell = new ShellImpl(fileSystem: new FileSystemImpl(new File("/")))

    def scriptLoader = new NoSharedClassLoaderScriptLoader()

    def agentContext = [
      getShellForScripts: {shell},
      getRootShell: { rootShell },
      getMop: {new MOPImpl()},
      getClock: { SystemClock.instance() },
      getScriptLoader: { scriptLoader }
    ] as AgentContext

    sm = new ScriptManagerImpl(agentContext: agentContext)
    sksm = new StateKeeperScriptManager(scriptManager: sm, storage: ramStorage)
  }

  /**
   * Test for when the state is invalid you get an invalid mount point  */
  public void testInvalidState()
  {
    def rootMP = MountPoint.ROOT
    ramStorage.storeState(rootMP,
                          [
                            scriptDefinition: [mountPoint: rootMP],
                            scriptState: [
                              stateMachine: [ currentState: 'installed' ],
                              script: [ rootPath: '/' ]
                            ]
                          ])

    def invalidMP = MountPoint.create('/invalid')
    def invalidState = [
      scriptDefinition: [
        mountPoint: invalidMP,
        scriptClassName: "not.exists.script"
      ]
    ]
    ramStorage.storeState(invalidMP, invalidState)

    assertTrue(_invaliStates.isEmpty())
    sksm.restoreScripts()
    assertEquals(1, _invaliStates.size())
    assertTrue(_invaliStates[invalidMP].is(invalidState))

    assertEquals("""{
  "scriptDefinition": {
    "initParameters": {
    },
    "mountPoint": "/invalid",
    "parent": "/",
    "scriptFactory": {
      "class": "org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory",
      "className": "org.linkedin.glu.agent.impl.script.InvalidStateScript"
    }
  },
  "scriptState": {
    "script": {
      "errorMessage": "Invalid state detected... check the state machine error for more details",
      "invalidState": "ram:invalidStates:/invalid"
    },
    "stateMachine": {
      "currentState": "NONE",
      "error": "org.linkedin.glu.agent.api.ScriptException: /invalid"
    }
  }
}""", JsonUtils.prettyPrint(ramStorage.loadState(invalidMP)))
  }
}