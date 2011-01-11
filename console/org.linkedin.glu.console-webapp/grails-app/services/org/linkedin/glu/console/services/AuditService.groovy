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

package org.linkedin.glu.console.services

import org.linkedin.glu.console.domain.Fabric
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry

class AuditService
{
  AgentsService agentsService

  def audit(Fabric fabric, SystemModel expectedSystem)
  {
    if(expectedSystem && expectedSystem.fabric != fabric.name)
      throw new IllegalArgumentException("fabric mismatch")

    SystemModel currentSystem = agentsService.getCurrentSystemModel(fabric)

    [
        audit: audit(currentSystem, expectedSystem),
        accuracy: currentSystem.metadata.accuracy
    ]
  }

  def audit(SystemModel currentSystem, SystemModel expectedSystem)
  {
    (expectedSystem, currentSystem) = SystemModel.filter(expectedSystem, currentSystem)

    def entries = []
    
    if(expectedSystem == null)
    {
      currentSystem.each { SystemEntry currentEntry ->
        def entry = currentEntry.flatten()
        entry.state = 'UNKNOWN'
        entry.status = 'unknown'
        entries << entry
      }
    }
    else
    {
      def allKeys = (expectedSystem.findEntries().key + currentSystem.findEntries().key) as SortedSet

      allKeys.each { key ->
        def currentEntry = currentSystem.findEntry(key)?.flatten()
        def expectedEntry = expectedSystem.findEntry(key)?.flatten()

        // means that it is not deployed at all
        if(!currentEntry)
        {
          expectedEntry.status = 'notDeployed'
          expectedEntry.state = 'ERROR'
          entries << expectedEntry
          return
        }

        // means that it should not be deployed at all
        if(!expectedEntry)
        {
          currentEntry.status = 'unexpected'
          currentEntry.state = 'ERROR'
          entries << currentEntry
          return
        }

        // here we have both currentEntry and expectedEntry...
        currentSystem.metadata.emptyAgents?.remove(currentEntry.agent)

        def initParameters = expectedEntry.keySet().findAll { it.startsWith("initParameters.") } +
            currentEntry.keySet().findAll { it.startsWith("initParameters.") }

        initParameters.each { n ->
          if(expectedEntry[n] != currentEntry[n])
          {
            currentEntry.status = 'versionMismatch'
            currentEntry.statusInfo = "${n}:${expectedEntry[n]} != ${n}:${currentEntry[n]}".toString()
            currentEntry.state = 'ERROR'
          }
        }

        if(!currentEntry.state)
        {
          if(currentEntry['metadata.currentState'] == 'running')
          {
            def error = currentEntry['metadata.error']
            if(error)
            {
              currentEntry.status = 'error'
              currentEntry.statusInfo = error.toString()
              currentEntry.state = 'ERROR'
            }
            else
            {
              currentEntry.status = 'running'
              currentEntry.state = 'RUNNING'
            }
          }
          else
          {
            currentEntry.status = 'notRunning'
            currentEntry.state = 'ERROR'
          }
        }

        if(currentEntry.state != 'ERROR')
        {
          // copy all missing keys from expected into current
          expectedEntry.each { k,v ->
            if(!currentEntry.containsKey(k))
              currentEntry[k] = v
          }
        }
        else
        {
          // override all keys from expected into current
          expectedEntry.each { k,v ->
            currentEntry[k] = v
          }
        }

        entries << currentEntry
      }
    }

    currentSystem.metadata?.emptyAgents?.each { agent ->
      def entry= [:]
      entry.agent = agent
      entry['metadata.currentState'] = 'NA'
      entry.status = 'NA'
      entry.state = 'NA'
      entries << entry
    }

    return entries
  }
}
