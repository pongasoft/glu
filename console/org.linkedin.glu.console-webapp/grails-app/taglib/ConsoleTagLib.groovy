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

import java.text.SimpleDateFormat
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.grails.utils.ConsoleHelper
import org.linkedin.glu.provisioner.plan.api.CompositeStep
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Timespan
import org.linkedin.util.clock.Chronos
import org.linkedin.util.io.PathUtils
import org.linkedin.glu.provisioner.services.tags.TagsService
import org.linkedin.glu.provisioner.services.fabric.FabricService

/**
 * Tag library for the console.
 *
 * @author ypujante  */
public class ConsoleTagLib
{
  static namespace = 'cl'

  TagsService tagsService
  FabricService fabricService

  Clock clock = SystemClock.instance()

  SimpleDateFormat _dateFormat = new SimpleDateFormat('MM/dd/yy HH:mm:ss z')

  def clearFlash = {
    flash.clear()
  }

  def formatDate = { args ->
    synchronized(_dateFormat) {
      if(args.date)
        out << _dateFormat.format(args.date)
    }
  }

  def formatDuration = { args ->
    def duration = args.duration
    def date = args.date

    if(date)
    {
      duration = new Timespan((clock.currentTimeMillis() - date.time) as long)
    }

    def time = args.time
    if(time)
    {
      duration = new Timespan((clock.currentTimeMillis() - time) as long)
    }

    if(duration == null)
    {
      return
    }

    if(!duration instanceof Timespan)
    {
      duration = new Timespan(duration as long)
    }

    if(duration.durationInMilliseconds == 0)
    {
      out << 0
    }
    else
    {
      if(duration.durationInMilliseconds < 1000)
      {
        out << duration.durationInMilliseconds << 'ms'
      }
      else
      {
        out << duration.filter(EnumSet.range(Timespan.TimeUnit.SECOND,
                                             Timespan.TimeUnit.WEEK)).getCanonicalString()

      }
    }
  }

  /**
   * Wrap a block with chronos to measure performance.
   */
  def chronos = { args, body ->
    def level = args.level ?: 'info'
    def var = args.var
    def message = args.message

    Chronos c = new Chronos()

    if(var)
      out << body((var): c)
    else
      out << body()

    if(message)
      log."${level}"("${message}: ${new Timespan(c.tick())}")
    else
      log."${level}"(new Timespan(c.tick()))
  }

  /**
   * Format an audit value
   * @param args.colunmName
   * @param args.detail
   */
  def formatAuditValue = { args ->
    def columnName = args.columnName
    def detail = args.detail
    def columns = args.columns

    def value = detail[columnName]

    switch(columnName)
    {
      case 'tags':
        out << renderTags(tags: value,
                          linkFilter: columns[columnName].linkFilter)
        return

      case 'tag':
        out << renderTags(tags: value,
                          linkFilter: columns[columnName].linkFilter)
        return

      default:
        if(value instanceof Collection)
        {
          if(value.size() == 1)
          {
            value = value.iterator().next()
          }
          else
          {
            out << "<div class=\"count\">${value.size()}</div>"
            return
          }
        }
    }

    if(value == null)
      return

    switch(columnName)
    {
      case 'metadata.modifiedTime':
        out << cl.formatDate(date: new Date(value))
        break;

      case 'initParameters.wars':
        def versions = ConsoleHelper.computeUrisVersion(value)
        if(versions?.size() == 1)
        {
          out << versions[0].encodeAsHTML()
        }
        else
        {
          out << "<ul class=\"wars\">\n"
          versions?.each { version ->
            out << "<li class=\"wars\">${version.encodeAsHTML()}</li>\n"
          }
          out << "</ul>\n"
        }
        break;

      case 'initParameters.config':
      case 'initParameters.skeleton':
      case 'script':
        def versions = ConsoleHelper.computeUrisVersion(value)
        out << versions[0].encodeAsHTML()
        break;

      case 'agent':
        out << g.link(controller: 'agents', action: 'view', id: value.encodeAsHTML()) {
          out << value.encodeAsHTML()
      }
      break;

      case 'status':
        out << cl.formatAuditStatus(status: value, statusInfo: detail.statusInfo, row: args.row)
        break;

      default:
        if(columns[columnName].linkFilter)
        {
          out << cl.linkToSystemFilter(name: columnName,
                                       value: value,
                                       displayName: columns[columnName].name) {
            out << value.encodeAsHTML()
          }
        }
        else
        {
          out << value.encodeAsHTML()
        }
    }
  }

  /**
   * Tag rendering.
   */
  def renderTags = { args ->
    def tags = args.tags
    def linkFilter = args.linkFilter

    if(tags)
    {
      if(!(tags instanceof Collection))
        tags = [tags]
      
      out << "<ul class=\"tags ${linkFilter ? 'with-links': 'no-links'}\">"
      tags.each { String tag ->
        out << "<li class=\"tag ${tagsService.getTagCssClass(tag) ?: ''}\">"
        if(linkFilter)
        {
          out << cl.linkToSystemFilter(name: 'tags',
                                       groupBy: 'tag',
                                       value: tag,
                                       displayName: 'tags') {
            out << tag
          }
        }
        else
         out << tag
        out << "</li>"
      }
      out << "</ul>"
    }
  }

  /**
   * Renders the ccs section for tags
   */
  def renderTagsCss = { args ->
    out << tagsService.tagsCss
  }

  /**
   * Create a link to the system filter page
   */
  def linkToSystemFilter = { args, body ->
    def name = args.name
    def value = args.value
    def groupBy = args.groupBy ?: name
    def title = args.title ?: "${args.displayName ?: name} [${value}]"

    out << g.link(controller: 'system',
                  action: 'filter',
                  params: [
                    systemFilter: "${name}='${value}'",
                    title: title,
                    groupBy: groupBy,
                    ]) {
      out << body()
    }
  }

  /**
   * Format audit status
   */
  def formatAuditStatus = { args ->
    def status = args.status

    switch(status)
    {
      case 'running':
        out << 'running'
        break;

      case 'notRunning':
        out << 'NOT running'
        break;

      case 'versionMismatch':
        if(args.statusInfo)
        {
          out << "<a href=\"#\" title=\"${args.statusInfo.encodeAsHTML()}\" class=\"statusInfo\" onclick=\"toggleShowHide('si-${args.row}');return false;\">version MISMATCH</a><dt id=\"si-${args.row}\" class=\"hidden\">${args.statusInfo.encodeAsHTML()}</dt>"
        }
        else
        {
          out << "version MISMATCH"
        }
        break;

      case 'unexpected':
        out << 'should NOT be deployed'
        break;

      case 'notDeployed':
        out << 'NOT deployed'
        break;

      case 'NA':
        out << 'nothing deployed'
        break;

      case 'error':
        out << "<a href=\"#\" title=\"${args.statusInfo.encodeAsHTML()}\" class=\"statusInfo\" onclick=\"toggleShowHide('si-${args.row}');return false;\">ERROR</a><dt id=\"si-${args.row}\" class=\"hidden\">${args.statusInfo.encodeAsHTML()}</dt>"
        break;

      case 'unknown':
        out << 'Undetermined (no system defined)'
        break;
    }
  }

  /**
   * Create an html attribute with proper encoding and quotes
   */
  def htmlAttribute = { args ->
    if(args.name && args.value != null)
      out << "${args.name}=\"${args.value.encodeAsHTML()}\""
  }

  def truncate = { args, body ->
    def text = args.text?.toString()
    def size = (args.size ?: 100) as int
    def var = args.var ?: 'var'

    if(text)
    {
      if(text.size() > size)
      {
        def truncatedText = text[0..size-1]

        if(body)
        {
          out << body((var): truncatedText)
        }
        else
        {
          out << truncatedText.encodeAsHTML()
        }
      }
      else
      {
        if(body)
        {
          out << body()
        }
        else
        {
          out << text.encodeAsHTML()
        }
      }
    }
  }

  def mountPointState = { args ->
    def mountPoint = args.mountPoint

    def state = mountPoint.currentState == 'running' ? 'RUNNING' : 'NOT_RUNNING'

    if(mountPoint.transitionState)
      state = 'TRANSITION'

    if(mountPoint.error)
      state = 'ERROR'

    out << state
  }

  def serviceVersion = { args ->
    def mountPoint = args.mountPoint

    out << ConsoleHelper.computeVersion(mountPoint?.data?.scriptDefinition?.initParameters?.release,
                                        mountPoint?.data?.scriptDefinition?.initParameters?.wars)
  }

  def linkToPs = { args, body ->
    def agent = args.agent
    def pid = args.pid
    if(pid)
    {
      out << g.link(controller: 'agents', action: 'ps', id: agent, params: [pid: pid]) {
        out << body()
      }
    }
  }

  def mapToTable = { args ->
    def map = args.map

    if(map)
    {
      out << '<table>'
      map.keySet().sort().each { key ->
        def value = map[key]
        out << '<tr>'
        if(value instanceof Map)
        {
          out << '<td>' << key.encodeAsHTML() << '</td>'
          out << '<td>'
          out << cl.mapToTable(map: value)
          out << '</td>'
        }
        else
        {
          out << '<td>' << key.encodeAsHTML() << '</td>'
          out << '<td>'
          out << value.encodeAsHTML()
          out << '</td>'
        }
        out << '</tr>'
      }
      out << '</table>'
    }
    else
    {
      out << '{}'
    }

  }

  def mapToUL = { args, body ->
    def var = args.var ?: 'var'
    def map = args.map
    def specialKeys = args.specialKeys ?: []

    if(map)
    {
      out << '<ul>'
      map.keySet().sort().each { key ->
        def value = map[key]

        if(!specialKeys.contains(key))
        {
          out << "<li class=\"${key.encodeAsHTML()}\">" << key.encodeAsHTML()
          if(value instanceof Map && value)
          {
            args.map = value
            out << cl.mapToUL(args) { nestedArgs ->
            if(nestedArgs)
            out << body((var): nestedArgs[var])
            else
            out << body()
            }
          }
          else
          {
            out << ': ' << value.encodeAsHTML()
          }
          out << '</li>'
        }
        else
        {
          out << body((var): [key: key, value: value])
        }
      }
      out << '</ul>'
    }
  }

  def renderStep = { args ->
    def step = args.step
    def stepIdx = args.stepIdx == null ? [:] : args.stepIdx

    out << "<dl class=\"${step.type} step\">"

    out << "<dt>"

    def inputClass = 'stepCheckBox'

    if(step instanceof CompositeStep)
    {
      if(step.steps?.find { it instanceof LeafStep})
      {
        inputClass = 'quickSelect'
      }
    }

    if(step.metadata)
      out << g.checkBox(name: 'stepId',
                        value: step.id,
                        id: step.id,
                        checked: true, 
                        'class': inputClass)

    if(step instanceof LeafStep)
    {
      stepIdx.idx = (stepIdx.idx ?: 0) + 1
      out << "${stepIdx.idx}. "
    }

    if(step.metadata.name)
    {
      out << step.metadata.name.encodeAsHTML()
    }
    else
    {
      out << step.metadata.collect { k, v ->
        switch(k)
        {
          case 'agent':
            return g.link(action: 'view', controller: 'agents', id: v) { v }

          case 'mountPoint':
            return g.link(action: 'view', controller: 'agents', id: step.metadata.agent, fragment: v) { v.encodeAsHTML() }

          default:
            return "${k.encodeAsHTML()}=${v.encodeAsHTML()}"
        }
      }.join(' - ')
    }
    
    if(step instanceof CompositeStep)
    {
      step.steps.each { child ->
        if(step.type == IStep.Type.PARALLEL)
        {
          stepIdx = [:]
        }
        out << "<dd>" << cl.renderStep(step: child, stepIdx: stepIdx) << "</dd>"
      }
    }

    out << "</dt>"

    out << "</dl>"
  }

  def stepVisit = { args, body ->
    def step = args.step
    def var = args.var ?: 'var'

    step.steps.each {
      out << body((var): it)
    }
  }

  def stepExecutionStatus = { args ->
    def step = args.step
    def progress = args.progress ?: [:]

    def status = progress[step.id]?.completionStatus?.status

    if(!status)
    {
      if(progress[step.id])
        status = "RUNNING"
      else
        status = "NONE"
    }

    out << status
  }

  def stepExecutionDuration = { args ->
    def step = args.step
    def progress = args.progress ?: [:]

    def duration = progress[step.id]?.completionStatus?.duration

    if(!duration)
    {
      if(progress[step.id]?.startTime)
        duration = "running [${cl.formatDuration(time: progress[step.id].startTime)}]"
      else
        duration = "not started"
    }
    else
    {
      duration = cl.formatDuration(duration: duration)
    }

    out << duration
  }

  def stepExecutionActions = { args ->
    def deployment = args.deployment
    def step = args.step
    def progress = args.progress ?: [:]

    def stepExecution = progress[step.id]

    if(stepExecution)
    {
      if(!stepExecution.isCompleted())
        out << "- [ ${g.link(controller: 'plan', action: 'cancelStep', id: deployment.id, params: [stepId: step.id]) { out << 'Cancel'}} ]"
    }
  }

  def stepExecutionError = { args ->
    def step = args.step
    def progress = args.progress ?: [:]

    def throwable = progress[step.id]?.completionStatus?.throwable

    if(throwable)
    {
      out << '<div class="throwable">'
      while(throwable)
      {
        out << '<div class="stackTrace">'
        out << '<div class="stackTraceMessage">* <span class="stackTraceExceptionClass">'
        out << throwable.class.name.encodeAsHTML()
        out << ': </span>' << throwable.message.encodeAsHTML()
        out << "</div>"
        out << '<div class="stackTraceBody">'
        out << throwable.stackTrace.join('\n').encodeAsHTML()
        out << "</div>"
        out << "</div>"
        throwable = throwable.cause
      }
      out << '</div>'
    }
  }

  def renderStepExecution = { args ->
    def deployment = args.deployment
    def step = args.step ?: deployment.planExecution.plan.step
    def progress = args.progress
    def dlClass = args.dlClass ?: step.type

    // duration
    def duration = cl.stepExecutionDuration(step: step, progress: progress)?.toString()

    // status
    def status = cl.stepExecutionStatus(step: step, progress: progress)?.toString()

    out << "<dl class=\"${dlClass}\">"

    out << "<dt class=\"${status}\">"

    if(step.metadata.name)
    {
      out << step.metadata.name.encodeAsHTML()
    }
    else
    {
      out << step.metadata.collect { k, v ->
        switch(k)
        {
          case 'agent':
            return g.link(action: 'view', controller: 'agents', id: v) { v }

          case 'mountPoint':
            return g.link(action: 'view', controller: 'agents', id: step.metadata.agent, fragment: v) { v.encodeAsHTML() }

          default:
            return "${k.encodeAsHTML()}=${v.encodeAsHTML()}"
        }
      }.join(' - ')
    }

    out << " - ${duration}"

    if(step instanceof CompositeStep)
    {
      step.steps.each { child ->
        out << "<dd>" << cl.renderStepExecution(deployment: deployment, step: child, progress: progress) << "</dd>"
      }
    }
    else
    {
      out << cl.stepExecutionActions(deployment: deployment, step: step, progress: progress)
      out << cl.stepExecutionError(step: step, progress: progress)
    }

    out << "</dt>"

    out << "</dl>"
  }

  /**
   * Simple tag to get the fabric object: if there is no such fabric then the body is not called
   */
  def withFabric = { args, body ->
    def var = args.var ?: 'fabric'
    def fabric = args.fabric ?: request.fabric
    if(fabric)
    {
      out << body((var): fabric)
    }
  }

  /**
   * Simple tag to get the fabric object: if fabric is not present then simply call with
   * <code>null</code>
   */
  def withOrWithoutFabric = { args, body ->
    def var = args.var ?: 'fabric'
    def fabric = args.fabric ?: request.fabric
    out << body((var): fabric)
  }

  /**
   * Simple tag to get the system object: if there is no such system, then the body is not called
   */
  def withSystem = { args, body ->
    def var = args.var ?: 'system'
    def system = args.system ?: request.system
    if(system)
      out << body((var): system)
  }

  /**
   * Simple tag to get the system object. If system is not present then simply call with
   * <code>null</code>
   */
  def withOrWithoutSystem = { args, body ->
    def var = args.var ?: 'system'
    def system = args.system ?: request.system
    out << body((var): system)
  }

  /**
   * Body executed when the fabric is not connected  */
  def whenDisconnectedFabric = { args, body ->
    def fabric = args.fabric ?: request.fabric
    if(!fabricService.isConnected(fabric.name))
     out << body()
  }

  /**
   * Display a link to the bom ref
   */
  def linkToBom = { args ->
    def bom = args.bom
    def ref = args.ref

    if(ref)
    {
      def entry = bom?.bom?.find(ref)

      if(entry)
      {
        def artifact = extractArtifact(entry.coordinates) ?: ''
        def version = extractVersion(entry.coordinates)

        if(ref.name == artifact)
        {
          out << g.link(controller: 'dbBom', action: 'show', id: bom.id, fragment: ref.name, 'class': 'bom-ref', title: "${entry.coordinates}".encodeAsHTML()) {
            out << "<span class=\"bom-detail\">${artifact?.encodeAsHTML()} - ${version?.encodeAsHTML()}</span>"
          }
        }
        else
        {
          out << g.link(controller: 'dbBom', action: 'show', id: bom.id, fragment: ref.name, 'class': 'bom-ref', title: "${entry.coordinates}".encodeAsHTML()) {
          out << ref.name.encodeAsHTML()
          }
          out << " <span class=\"bom-detail\">[${artifact?.encodeAsHTML()} - ${version?.encodeAsHTML()}]</span>"
        }
      }
      else
      {
        out << "${ref.name} [invalid]"
      }
    }
  }


  /**
   * Return the class for the given navbar entry 
   */
  def navbarEntryClass = { args ->
    def entry = args.entry
    if(params.__nvbe == entry)
    {
      out << 'class="selected"'
    }
  }

  /**
   * Defines a checkbox that gets initialized from the params value (as a value from the params)
   */
  def checkBoxInitFromParams = { args ->
    def name = args.name
    def paramName = args.paramName ?: name
    def checkedByDefault = args.checkedByDefault != null ? args.checkedByDefault : true
    def checked = checkedByDefault
    if(params[paramName] == 'false')
    {
      checked = false
    }
    if(params[paramName] == 'true')
    {
      checked = true
    }
    args.value = checked
    out << g.checkBox(args)
  }

  def extractVersion(uri)
  {
    if(uri instanceof String)
      uri = new URI(uri as String)

    if(uri.scheme == 'ivy')
    {
      return extractURIPathPart(uri, 2)
    }
    else
    {
      // the last entry in the URI
      return extractURIPathPart(uri, -1)
    }
  }

  /**
   * Render the audit javascript: since it uses g.remoteFunction it cannot be put in javascript itself... 
   */
  def renderAuditJS = { args ->
    def filter = args.filter
    def columns = args.columns ?: ConsoleConfig.getInstance().defaults.dashboard
    out << 'function render(groupBy) {\n'
    out << "var p = computeRenderParams(groupBy, ['${columns.keySet().join('\',\'')}']);\n"
    if(filter)
    {
      def encodedFilter = filter.encodeAsURL()
      out << "p = p + '&systemFilter=${encodedFilter}';\n"
    }
    out << "${g.remoteFunction(controller: 'dashboard', action: 'renderAudit', params: "p", before: 'showSpinner()', update:[success: '__audit', failure: '__audit_content'])}\n"
    out << "}\n"

    out << """function renderSame() {
    render(null);
}"""
  }

  /**
   * Render the log section for a given mountpoint
   */
  def mountPointLogs = { args ->
    def script = args.mountPoint.data?.scriptState?.script
    def agent = args.agent
    if(script)
    {
      File gcLog = ConsoleHelper.toFileObject(script.gcLog)
      File mainLog = ['containerLog', 'serverLog', 'applicationLog'].
        collect { ConsoleHelper.toFileObject(script?.getAt(it))}.find { it }
      File logsDir = ConsoleHelper.toFileObject(script.logsDir) ?: mainLog?.parentFile

      def logs = [main: mainLog, gc: gcLog, 'more...': logsDir]

      if(logs.any { k, v -> v})
      {
        out << "<li>Logs: "

        out << logs.collect { String logType, File logFile ->
          if(logFile)
          {
            g.link(controller: 'agents',
                   action: 'fileContent',
                   id: agent,
                   params: [location: logFile.path, maxLine: 500]) {
              out << logType
            }
          }
          else
           return null
        }.findAll { it }.join(' | ')

        out << "</li>"
      }
    }
  }

  def extractArtifact(uri)
  {
    if(uri instanceof String)
      uri = new URI(uri as String)

    if(uri.scheme == 'ivy')
    {
      return extractURIPathPart(uri, 1)
    }
    else
    {
      return null
    }
  }

  private String extractURIPathPart(URI uri, idx)
  {
    if(uri == null)
      return null

    def coordinates = PathUtils.removeLeadingSlash(uri.path).split('/') ?: ['unknown']
    if(coordinates.size() > idx)
      return coordinates[idx] as String

    return null
  }
}