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


package org.linkedin.glu.agent.impl.script

import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.glu.agent.api.Agent

/**
 * This is the script that will auto upgrade the agent.
 *
 * Here is an example of how to run the script using the agent main:
 * <pre>
 * gluc.sh -c org.linkedin.glu.agent.impl.script.AutoUpgradeScript -m /upgrade -a "[newVersion:'2.0.0',agentTar:'file:/tmp/agent-2.0.0.tgz']"
 * gluc.sh -m /upgrade -e install
 * gluc.sh -m /upgrade -e prepare
 * gluc.sh -m /upgrade -e commit
 * gluc.sh -m /upgrade -e uninstall
 * gluc.sh -m /upgrade -u
 * </pre>
 *
 * @author ypujante@linkedin.com
 */
class AutoUpgradeScript
{
  def static stateMachine = Agent.SELF_UPGRADE_TRANSITIONS

  def currentVersion
  File agentRootDir
  def untarredAgent
  def obsoleteVersion

  /**
   * Fetches the new agent tarball, untar it in its final destination
   * (/export/content/glu/agent/${params.newVersion}).
   */
  def install = {
    log.info "Installing..."

    agentRootDir = new File(System.getProperty('user.dir')).parentFile

    currentVersion = System.getProperty('org.linkedin.app.version')

    if(!okToUpgrade())
    {
      shell.fail('Already up to date.')
    }

    def agentTar = shell.fetch(params.agentTar)
    untarredAgent = shell.untar(agentTar)

    // make sure that the bin directory contains all executable scripts
    untarredAgent.bin.ls().each {
      shell.chmodPlusX(it)
    }

    log.info "Install complete."
  }

  /**
   * Move the file in its proper location: (/export/content/glu/agent/${params.newVersion})
   * Runs the shell script (/export/content/glu/agent/bin/async_upgradectl.sh). The effect of running
   * this command will be to stop the current agent and start the new one.
   */
  def prepare = {
    log.info "Preparing..."

    if(!okToUpgrade())
    {
      shell.fail('Already up to date.')
    }

    // the location where the new agent needs to go is not available from the filesystem available
    // through the shell this is why I need to create a new one...
    def fs = new FileSystemImpl(agentRootDir)

    fs.mv(untarredAgent, params.newVersion)

    asyncUpgrade(params.newVersion)

    log.info "Prepare complete... restarting agent..."
  }


  /**
   * Committing
   */
  def commit = {
    log.info "Committing..."

    def newVersion = System.getProperty('org.linkedin.app.version')
    if(newVersion != params.newVersion)
    {
      shell.fail("Current agent version ${newVersion} is not as expected ${params.newVersion}.")
    }

    obsoleteVersion = currentVersion

    log.info "Committed."
  }

  /**
   * Rolling back: reverting to the previous version
   */
  def rollback = {
    log.info "Rolling back..."

    obsoleteVersion = params.newVersion

    def newVersion = System.getProperty('org.linkedin.app.version')
    if(newVersion != currentVersion)
    {
      asyncUpgrade(currentVersion)
      log.info "Rolling back complete... restarting agent..."
    }
    else
    {
      log.info "Rollback complete."
    }
  }

  /**
   * In the uninstall phase we delete either the new or old directory depending on commit/rollback 
   */
  def uninstall = {
    log.info "Uninstalling..."

    if(obsoleteVersion)
    {
      // the location where the new agent needs to go is not available from the filesystem available
      // through the shell this is why I need to create a new one...
      def fs = new FileSystemImpl(agentRootDir)

      fs.rmdirs(obsoleteVersion)
    }

    log.info "Uninstalled."
  }

  /**
   * Returns the upgrade log
   * // TODO HIGH YP: need to stream instead
   */
  public String getUpgradeLog()
  {
    return new FileSystemImpl(agentRootDir).readContent('data/logs/glu-agent-upgrade.log')
  }

  private asyncUpgrade(newVersion)
  {
    shell.exec("${new File(agentRootDir, 'bin/async_upgradectl.sh')} ${newVersion} &")
  }

  private boolean okToUpgrade()
  {
    return params.newVersion != currentVersion
  }
}
