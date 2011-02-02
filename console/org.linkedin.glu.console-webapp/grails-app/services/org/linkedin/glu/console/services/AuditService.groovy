/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.console.services

import org.linkedin.glu.console.domain.Fabric
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.glu.utils.tags.ReadOnlyTaggeable

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

    Map<String, Set<String>> agentTags

    if(expectedSystem == null)
    {
      currentSystem.each { SystemEntry currentEntry ->
        def entry = currentEntry.flatten()
        entry.state = 'UNKNOWN'
        entry.status = 'unknown'
        setTags(entry, currentEntry.tags, currentSystem.getAgentTags(currentEntry.agent).tags)
        entries << entry
      }
      agentTags =
        GroovyCollectionsUtils.collectKey(currentSystem.agentTags, new HashMap()) { String k, ReadOnlyTaggeable v ->
          v.tags
        }
    }
    else
    {
      def allKeys = (expectedSystem.findEntries().key + currentSystem.findEntries().key) as SortedSet

      agentTags =
        GroovyCollectionsUtils.collectKey(expectedSystem.agentTags, new HashMap()) { String k, ReadOnlyTaggeable v ->
          new TreeSet<String>(v.tags)
        }
      currentSystem.agentTags.each { agent, roTaggeable ->
        def tags = agentTags[agent]
        if(!tags)
        {
          tags = new TreeSet<String>()
          agentTags[agent] = tags
        }
        tags.addAll(roTaggeable.tags)
      }

      allKeys.each { key ->
        SystemEntry currentEntry = currentSystem.findEntry(key)
        def cef = currentEntry?.flatten()
        
        SystemEntry expectedEntry = expectedSystem.findEntry(key)
        def eef = expectedEntry?.flatten()

        // means that it is not deployed at all
        if(!cef)
        {
          eef.status = 'notDeployed'
          eef.state = 'ERROR'
          setTags(eef, expectedEntry.tags, agentTags[expectedEntry.agent])
          entries << eef
          return
        }

        // means that it should not be deployed at all
        if(!eef)
        {
          cef.status = 'unexpected'
          cef.state = 'ERROR'
          setTags(cef, currentEntry.tags, agentTags[currentEntry.agent])
          entries << cef
          return
        }

        // here we have both currentEntry and expectedEntry...
        currentSystem.metadata.emptyAgents?.remove(cef.agent)

        def initParameters = eef.keySet().findAll { it.startsWith("initParameters.") } +
            cef.keySet().findAll { it.startsWith("initParameters.") }

        initParameters.each { n ->
          if(eef[n] != cef[n])
          {
            cef.status = 'versionMismatch'
            cef.statusInfo = "${n}:${eef[n]} != ${n}:${cef[n]}".toString()
            cef.state = 'ERROR'
          }
        }

        if(!cef.state)
        {
          if(cef['metadata.currentState'] == 'running')
          {
            def error = cef['metadata.error']
            if(error)
            {
              cef.status = 'error'
              cef.statusInfo = error.toString()
              cef.state = 'ERROR'
            }
            else
            {
              cef.status = 'running'
              cef.state = 'RUNNING'
            }
          }
          else
          {
            cef.status = 'notRunning'
            cef.state = 'ERROR'
          }
        }

        if(cef.state != 'ERROR')
        {
          // copy all missing keys from expected into current
          eef.each { k,v ->
            if(!cef.containsKey(k))
              cef[k] = v
          }
        }
        else
        {
          // override all keys from expected into current
          eef.each { k,v ->
            cef[k] = v
          }
        }

        setTags(cef, currentEntry.tags + expectedEntry.tags, agentTags[currentEntry.agent])
        entries << cef
      }
    }

    currentSystem.metadata?.emptyAgents?.each { agent ->
      def entry= [:]
      entry.agent = agent
      entry['metadata.currentState'] = 'NA'
      entry.status = 'NA'
      entry.state = 'NA'
      setTags(entry, null, agentTags[agent])
      entries << entry
    }

    return entries
  }

  private void setTags(def entry, Collection<String> entryTags, Collection<String> agentTags)
  {
    if(entryTags)
      entry.tags = entryTags

    if(agentTags)
      entry.agentTags = agentTags
  }
}
