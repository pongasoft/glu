/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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
import org.linkedin.glu.orchestration.engine.tags.TagsService
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.groovy.util.lang.GroovyLangUtils
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaColumnDefinition
import org.linkedin.glu.console.filters.UserPreferencesFilters

import org.linkedin.glu.provisioner.core.model.PropertySystemFilter
import org.linkedin.glu.provisioner.core.model.SystemFilter
import org.linkedin.glu.provisioner.core.model.LogicSystemFilterChain

import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.provisioner.core.model.ClosureSystemFilter
import org.linkedin.glu.provisioner.core.model.SystemEntryKeyModelFilter
import org.linkedin.glu.provisioner.core.model.FlattenSystemFilter
import org.linkedin.glu.provisioner.core.model.SystemFilterHelper
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.groovy.util.state.DefaultStateMachine
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils
import org.linkedin.util.lang.LangUtils

/**
 * Tag library for the console.
 *
 * @author ypujante  */
public class ConsoleTagLib
{
  public static final String DEFAULT_RUNNING_STATE =
    DefaultStateMachine.DEFAULT_ENTRY_STATE.toUpperCase()

  public static final String DEFAULT_NOT_RUNNING_STATE = "NOT_${DEFAULT_RUNNING_STATE}".toString()

  static namespace = 'cl'

  TagsService tagsService
  FabricService fabricService
  ConsoleConfig consoleConfig

  Clock clock = SystemClock.instance()

  SimpleDateFormat _dateFormat = new SimpleDateFormat('MM/dd/yy HH:mm:ss z')

  /**
   * "Replaces" g.link to account for fabric and make urls copy/paste friendly
   */
  def link = { args, body ->
    out << g.link(doAdjustArgs(args), body)
  }

  /**
   * "Replaces" g.createLink to account for fabric and make urls copy/paste friendly
   */
  def createLink = { args ->
    out << g.createLink(doAdjustArgs(args))
  }

  /**
   * "Replaces" g.form to account for fabric and make urls copy/paste friendly
   */
  def form = { args, body ->
    out << g.form(args) {
      def fabricName = request.fabric?.name
      if(fabricName)
        out << "<input type=\"hidden\" name=\"fabric\" value=\"${fabricName.encodeAsHTML()}\">"
      out << body()
    }
  }

  def remoteFunction = { args ->
    out << g.remoteFunction(doAdjustArgs(args))
  }

  /**
   * Split a path and link every elements in the path
   */
  def linkFilePath = { args ->
    File file = args.file as File
    File iFile = file?.parentFile

    def res = []

    while(iFile)
    {
      res << iFile
      iFile = iFile.parentFile
    }

    res.reverse().each { File d ->
      out << cl.link(title: "Go to directory [${d.name}]",
                     controller: 'agents',
                     id: args.agent,
                     action: 'fileContent',
                     params: [location: d.toURI().rawPath]) {
        out << "${d.name}/".encodeAsHTML()
      }
    }

    if(file?.parentFile)
      out << file.name.encodeAsHTML()
  }

  /**
   * Add the fabric parameter if necessary
   */
  private def doAdjustArgs(args)
  {
    def fabricName = request.fabric?.name

    if(fabricName && !args.params?.fabric)
    {
      def params = args.params ?: [:]
      params.fabric = fabricName
      args.params = params
    }

    return args
  }

  def clearFlash = {
    flash.clear()
  }

  def formatDate = { args ->
    if(args.date)
    {
      synchronized(_dateFormat) {
          out << _dateFormat.format(args.date)
      }
    }
    else
    {
      if(args.time)
      {
        synchronized(_dateFormat) {
            out << _dateFormat.format(new Date(args.time))
        }
      }
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
   * Format an computeDelta value
   * @param args.colunm a {@link CustomDeltaColumnDefinition} column definition
   * @param args.values all the values for the row (a map)
   */
  def formatDeltaValue = { args ->
    CustomDeltaColumnDefinition column = args.column
    def values = args.values

    def value = values[column.name]

    if(value == null)
      return

    if(value instanceof Map)
    {
      if(value.count != null)
      {
        if(value.count > 0)
          out << "<div class=\"count\">${value.count.encodeAsHTML()}</div>"
        return
      }
    }

    switch(column.source)
    {
      case 'tags':
        out << renderTags(tags: value,
                          linkable: column.linkable)
        return

      case 'tag':
        out << renderTags(tags: value,
                          linkable: column.linkable)
        return

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

      case 'agent':
        if(column.linkable)
        {
          if(GluGroovyLangUtils.getOptionalBoolean(consoleConfig.defaults.dashboardAgentLinksToAgent,
                                                   true))
          {
            out << cl.link(controller: 'agents', action: 'view', id: value.encodeAsHTML()) {
              out << value.encodeAsHTML()
            }
          }
          else
          {
            out << cl.linkToFilteredDashboard(systemFilter: "${column.source}='${value}'",
                                              groupBy: column.name) {
              out << value.encodeAsHTML()
            }
            out << cl.link(controller: 'agents', action: 'view', id: value) {
              out << '<i class="icon-zoom-in"> </i>'
            }
          }
        }
        else
          out << value.encodeAsHTML()
      break;

      case 'status':
        out << cl.formatDeltaStatus(status: value, statusInfo: values.statusInfo, row: args.row)
      break;

      default:
        if(column.linkable)
        {
          out << cl.linkToFilteredDashboard(systemFilter: "${column.source}='${value}'",
                                            groupBy: column.name) {
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
    def linkable = args.linkable

    if(tags)
    {
      if(tags instanceof Collection)
      {
        out << "<ul class=\"tags ${linkable ? 'with-links': 'no-links'}\">"
        tags.each { String tag ->
          out << "<li class=\"tag ${tagsService.getTagCssClass(tag) ?: ''}\">"
          if(linkable)
          {
            out << cl.linkToFilteredDashboard(systemFilter: "tags='${tag}'",
                                              groupBy: 'tags') {
              out << tag.encodeAsHTML()
            }
          }
          else
           out << tag.encodeAsHTML()
          out << "</li>"
        }
        out << "</ul>"
      }
      else
        out << tags.encodeAsHTML()
    }
  }

  /**
   * Renders the ccs section for tags
   */
  def renderTagsCss = { args ->
    out << tagsService.tagsCss
  }

  /**
   * Renders the custom css section
   */
  def renderCustomCss = { args ->
    def customCss = consoleConfig.defaults.customCss
    if(customCss)
    {
      if(customCss instanceof URI)
        out << "<link rel=\"stylesheet\" href=\"${customCss.toString()}\"/> "
      else
        out << "<style type=\"text/css\">${customCss.toString()}</style>"
    }
  }

  /**
   * Recursively displays the system filter
   */
  def renderSystemFilter = { args ->
    SystemFilter filter = args.filter
    boolean renderRemoveLink = GluGroovyLangUtils.getOptionalBoolean(args.renderRemoveLink, true)

    switch(filter)
    {
      case LogicSystemFilterChain:
        out << "<li class=\"filter-LogicSystemFilterChain\">${filter.kind.encodeAsHTML()}</li>"
        out << "<ul>"
        filter.filters.each { SystemFilter f ->
          out << "<li>"
          out << cl.renderSystemFilter(filter: f, renderRemoveLink: renderRemoveLink)
          out << "</li>"
        }
        out << "</ul>"
        break

      case ClosureSystemFilter:
      case SystemEntryKeyModelFilter:
      case FlattenSystemFilter:
        // toDSL throws an exception... currently
        out << filter.toString().encodeAsHTML()
        break

      default:
        if(filter)
        {
          out << filter.toDSL().encodeAsHTML()
          if(renderRemoveLink)
          {
            out << '['
            out << cl.link(controller: 'dashboard',
                          action: 'redelta',
                          params: [
                          'session.systemFilter': "-${filter.toDSL()}",
                          ]) {
              '<i class="icon-remove"> </i></cl:link>'
            }
            out << ']'
          }
        }
        else
          out << '-'
        break
    }
  }

  /**
   * Create a link to the dashboard (filtered)
   */
  def linkToFilteredDashboard = { args, body ->
    def systemFilter = args.systemFilter
    def groupBy = args.groupBy ?: name

    out << cl.link(controller: 'dashboard',
                   action: 'redelta',
                   params: [
                     'session.systemFilter': "+${systemFilter}",
//                    'session.groupBy': groupBy,
                   ]) {
      out << body()
    }
  }

  /**
   * Format computeDelta status
   */
  def formatDeltaStatus = { args ->
    def status = args.status

    switch(status)
    {
      case 'delta':
        def statusInfo = ConsoleHelper.toCollection(args.statusInfo)
        out << "<a href=\"#\" title=\"${statusInfo.toString().encodeAsHTML()}\" class=\"statusInfo\" onclick=\"toggleShowHide('#si-${args.row}');return false;\">DELTA</a><dt id=\"si-${args.row}\" class=\"hidden\"><ul>"
        statusInfo.each { sti ->
          out << "<li>${sti.encodeAsHTML()}</li>"
        }
        out << "</ul></dt>"
        break;

      case 'expectedState':
      case 'unexpected':
      case 'notDeployed':
      case 'parentDelta':
      case 'NA':
        def statusInfo = args.statusInfo
        if(statusInfo instanceof Collection)
        {
          statusInfo = statusInfo as Set
          if(statusInfo.size() == 1)
            statusInfo = statusInfo.iterator().next()
          else
          {
            out << "<div class=\"count\">${statusInfo.size()}</div>"
            return
          }
        }
        out << statusInfo.encodeAsHTML()
        break;

      case 'notExpectedState':
      case 'error':
        def statusInfo = ConsoleHelper.toCollection(args.statusInfo)
        out << "<a href=\"#\" title=\"${statusInfo.toString().encodeAsHTML()}\" class=\"statusInfo\" onclick=\"toggleShowHide('#si-${args.row}');return false;\">ERROR</a><dt id=\"si-${args.row}\" class=\"hidden\"><ul>"
        statusInfo.each { sti ->
          out << "<li>${sti.encodeAsHTML()}</li>"
        }
        out << "</ul></dt>"
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

    def state = mountPoint.currentState == DefaultStateMachine.DEFAULT_ENTRY_STATE ?
      DEFAULT_RUNNING_STATE :
      DEFAULT_NOT_RUNNING_STATE

    if(mountPoint.transitionState)
      state = 'TRANSITION'

    if(mountPoint.error)
      state = 'ERROR'

    out << state
  }

  def linkToPs = { args, body ->
    def agent = args.agent
    def pid = args.pid
    if(pid)
    {
      out << cl.link('class': args.class, controller: 'agents', action: 'ps', id: agent, params: [pid: pid]) {
        out << body()
      }
    }
  }

  def mapToTable = { args ->
    def map = args.map
    def clazz = args.class

    if(map)
    {
      if(clazz)
        clazz = "class=\"${clazz.encodeAsHTML()}\""
      out << "<table ${clazz}>"
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
          out << "<li class=\"m2u-${key.encodeAsHTML()}\">" << key.encodeAsHTML()
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
    if(step == null)
    {
      return
    }
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
            return cl.link(action: 'view', controller: 'agents', id: v) { v.encodeAsHTML() }

          case 'mountPoint':
            return cl.link(action: 'view', controller: 'agents', id: step.metadata.agent, fragment: v) { v.encodeAsHTML() }

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
        out << "- [ ${cl.link(controller: 'plan', action: 'cancelStep', id: deployment.id, params: [stepId: step.id]) { out << 'Cancel'}} ]"
    }
  }

  def stepExecutionError = { args ->
    def step = args.step
    def progress = args.progress ?: [:]

    def throwable = progress[step.id]?.completionStatus?.throwable

    if(throwable)
    {
      out << renderJsonException(exception: throwable)
    }
  }

  def renderStepExecution = { args ->
    try
    {
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
        if(step.metadata.agent && step.metadata.mountPoint) {
          out << cl.link(controller: 'agents', action: 'view', 'class': 'step-link', id: step.metadata.agent, fragment: step.metadata.mountPoint) {
            step.metadata.name.encodeAsHTML()
          }
        }
        else
          out << step.metadata.name.encodeAsHTML()
      }
      else
      {
        out << step.metadata.collect { k, v ->
          switch(k)
          {
            case 'agent':
              return cl.link(action: 'view', controller: 'agents', id: v) { v.encodeAsHTML() }

            case 'mountPoint':
              return cl.link(action: 'view', controller: 'agents', id: step.metadata.agent, fragment: v) { v.encodeAsHTML() }

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
    catch(Throwable th)
    {
      GroovyLangUtils.noException(args, null) {
        log.warn("Unexpected exception in renderStepExecution [ignored]", th)
        out << "<div class=\"FAILED\">Could not render step (check console log for errors)</div>"
      }
    }
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
   * Simple tag to get the fabric object: if there is no such fabric then the body is called
   */
  def withoutFabric = { args, body ->
    def var = args.var ?: 'fabric'
    def fabric = args.fabric ?: request.fabric
    if(!fabric)
    {
      out << body((var): fabric)
    }
  }

  /**
   * Renders the drop down for selecting a fabric
   */
  def renderFabricSelectDropdown = { args ->
    def fabric = args.fabric ?: request.fabric?.name

    def fabricNames = fabricService.listFabricNames().sort()
    fabricNames.remove(fabric)

    if(fabricNames.size() > 0)
    {
      out << '<li class="dropdown">'
      if(fabric)
        out << "<a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">${fabric.encodeAsHTML()}<b class=\"caret\"></b></a>"
      out << '<ul class="dropdown-menu">'
      fabricNames.each { fabricName ->
        out << "<li>"
        out << cl.link(controller: 'dashboard', action: 'delta', params: [fabric: fabricName]) { fabricName.encodeAsHTML() }
        out << "</li>"
      }
      out << '</ul>'
      out << '</li>'
    }
    else
    {
      if(fabric)
        out << "<li><a href=\"#\">${fabric.encodeAsHTML()}</a></li>"
    }
  }

  /**
   * Renders the drop down in the subtab section under the dashboard tab
   */
  def renderDashboardSelectDropdown = {
    out << "<a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">${request.userSession?.currentCustomDeltaDefinitionName?.encodeAsHTML()}<b class=\"caret\"></b></a>"
    out << "<ul class=\"dropdown-menu\">"
    out << "<li>"
    out << cl.link('class': 'btn', controller: 'dashboard', action: 'redelta', params: ['session.reset': true]) {
      "Reset"
    }
    out << "</li>"

    def otherNames = request.userSession?.customDeltaDefinitionNames
    if(otherNames)
    {
      otherNames = new TreeSet(otherNames)
      otherNames.remove(request.userSession?.currentCustomDeltaDefinitionName)
    }
    if(otherNames)
    {
      out << "<li class=\"divider\"></li>"
      otherNames.each { name ->
        out << "<li>"
        def redirectParams = [:]
        redirectParams[UserPreferencesFilters.CUSTOM_DELTA_DEFINITION_COOKIE_NAME] = name
        redirectParams['session.clear'] = true
        out << cl.link(controller: 'dashboard', action: 'redelta', params: redirectParams) {
          name.encodeAsHTML()
        }
        out << "</li>"
      }
      out << "<li class=\"divider\"></li>"
    }

    out << "<li>"
    out << '<a href="#saveAsNew" role="button" class="btn" data-toggle="modal" data-backdrop="true" data-keyboard="true">Save as new</a>'
    out << "</li>"
    out << "</ul>"
  }

  /**
   * Defined in the config file under the dashboard.shortcutFilters section
   */
  def renderDashboardShortcutFilters = {
    if(!request.system)
      return

    consoleConfig.defaults.shortcutFilters?.each { shortcutFilter ->
      def name = shortcutFilter.name
      def source = shortcutFilter.source ?: "metadata.${name}".toString()
      def headerKeys = shortcutFilter.header ?: []

      def possibleValues = request.system.metadata[name]
      if(possibleValues)
      {
        String selectedFilterDisplayName = null
        SystemFilter selectedFilter = null

        def dd = new TreeMap()

        possibleValues.each { value ->
          value = value.value
          def entry = [:]

          // filter
          entry.filter = "+${source}='${value.name}'".toString()

          // display name
          def displayName = [value.name]
          headerKeys.each { headerKey ->
            def headerValue = value[headerKey]
            if(headerValue)
              displayName << headerValue
          }
          entry.displayName = displayName.join(':').encodeAsHTML()

          PropertySystemFilter filter = new PropertySystemFilter(name: source,
                                                                 value: value.name)
          if(SystemFilterHelper.definesSubsetOrEqual(filter,
                                                     request.system.filters))
          {
            selectedFilterDisplayName = entry.displayName
            selectedFilter = filter
          }
          else
          {
            dd[value.name] = entry
          }
        }

        out << '<li class="dropdown">'
        out << "<a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">"
        if(selectedFilterDisplayName)
          out << selectedFilterDisplayName.encodeAsHTML()
        else
          out << "All [${name}]"
        out << "<b class=\"caret\"></b></a>"
        out << '<ul class="dropdown-menu">'
        dd.values().each { v ->
          out << "<li>"
          def filter = v.filter
          // YP note: there seems to be a bug (caching) with grails when using an array with
          // params and g.link... need to build by hand!
          def href = "${cl.createLink(controller: 'dashboard', action: 'delta')}&session.systemFilter=${URLEncoder.encode(filter, "UTF-8")}"
          if(selectedFilter)
          {
            href = "${href}&session.systemFilter=-${URLEncoder.encode(selectedFilter.toDSL(), "UTF-8")}"
          }
          out << "<a href=\"${href}\">${v.displayName.encodeAsHTML()}</a>"
          out << "</li>"
        }
        if(selectedFilter)
        {
          out << "<li>"
          out << cl.link(controller: 'dashboard', action: 'redelta', params: ["session.systemFilter": "-${selectedFilter.toDSL()}".toString()]) {
            'All'
          }
          out << '</li>'
        }
        out << '</ul>'
        out << '</li>'
      }
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
    if(!fabricService.isConnected(fabric?.name))
     out << body()
  }

  /**
   * Renders the system id
   */
  def renderSystemId = { args ->
    String systemId
    String name

    if(args.system)
    {
      SystemModel system = args.system
      systemId = system.id
      name = system.name
    }
    else
    {
      systemId = args.id
      name = args.name
    }
    boolean renderLinkToSystem = GluGroovyLangUtils.getOptionalBoolean(args.renderLinkToSystem,
                                                                       true)

    String text

    if(name)
    {
      text = "<span class=\"systemId\">${systemId[0..<Math.min(systemId.size(), 10)]}</span> [${name.encodeAsHTML()}]"
    }
    else
      text = "<span class=\"systemId\">${systemId}</span>"


    if(renderLinkToSystem)
      out << cl.link(controller: 'model', action: 'view', id: systemId, title: systemId) {
        text
      }
    else
      out << text
  }

  /**
   * Return the class for the given navbar entry
   */
  def navbarEntryClass = { args ->
    def entry = args.entry
    if(params.__nvbe == entry)
    {
      out << 'class="active"'
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
   * Render the log section for a given mountpoint
   */
  def mountPointLogs = { args ->
    def script = args.mountPoint.data?.scriptState?.script
    def agent = args.agent
    if(script)
    {
      Map<String, File> logs = [:]

      script.each { k,v ->
        if(k.endsWith("Log"))
        {
          logs[k - "Log"] = ConsoleHelper.toFileObject(v)
        }
      }

      // if the script has a map called 'logs', then use it as well
      if(script.logs instanceof Map)
      {
        script.logs.each { k, v -> logs[k] = ConsoleHelper.toFileObject(v) }
      }

      // determining mainLog
      File mainLog =
        ['container', 'server', 'application'].collect { logs[it] }.find { it }

      if(mainLog == null && logs)
      {
        mainLog = logs.values().find { it }
      }

      // determining logsDir
      File logsDir = ConsoleHelper.toFileObject(script.logsDir) ?: mainLog?.parentFile

      logs = logs.sort()

      if(logsDir)
      {
        // we make sure that 'more...' is the last entry!
        logs = new LinkedHashMap(logs)
        logs['more...'] = logsDir
      }

      if(logs.any { k, v -> v})
      {
        out << "<li>Logs: "

        out << logs.collect { String logType, File logFile ->
          if(logFile)
          {
            cl.link(controller: 'agents',
                    action: 'fileContent',
                    id: agent,
                    params: [(logFile == logsDir ? 'location': 'file'): logFile.path]) {
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

  def renderCommandBytes = { args ->
    def commandExecution = args.command
    def streamType = args.streamType
    def bytes = commandExecution.getFirstBytes(streamType)
    if(bytes)
    {
      out << new String(bytes, "UTF-8").encodeAsHTML()
      if(commandExecution.hasMoreBytes(streamType))
      {
        if(args.onclick)
        {
          out << "<a href=\"#\" onclick=\"${args.onclick}\" class=\"moreBytes\">[...]</a>"
        }
        else
          out << "[...]"
      }
    }
  }

  def renderJsonException = { args ->
    def exception = args.exception

    if(exception == null)
      return

    def exceptionAsJson = exception

    if(exception instanceof Throwable)
      exceptionAsJson = GluGroovyJsonUtils.extractFullStackTrace(exception)
    else
    {
      if(!(exception instanceof Collection))
        exceptionAsJson = GluGroovyJsonUtils.fromJSON(exception.toString())
    }

    if(exceptionAsJson)
    {
      def gid = "json-exception-${System.identityHashCode(exceptionAsJson)}"
      out << "<dl id=\"${gid}\" class=\"errorStackTrace\">"
      exceptionAsJson?.eachWithIndex { e, idx ->
        def id = "${gid}-${idx}"
        out << "<dt class=\"stackTraceHeader\">"
        out << "[<a href=\"#\" onclick=\"toggleShowHide('#${id}')\">+</a>]"
        out << " ${e.name?.encodeAsHTML()}: ${e.message?.encodeAsHTML()}"
        out << "</dt>"
        out << "<div id=\"${id}\" class=\"hidden\">"
        e.stackTrace.each { ste ->
          def file = ste.ln as int
          if(file >= 0)
            file = "${ste.fn}:${file}"
          else
            file = "Native Method"
          def line = "at ${ste.dc}.${ste.mn}(${file})"
          out << "<dd class=\"stackTraceBody\">${line.encodeAsHTML()}</dd>"
        }
        out << "</div>"
      }
      out << "</dl>"
    }

    if(exception instanceof Throwable)
      out << "<div class=\"hidden\">${LangUtils.getStackTrace(exception).encodeAsHTML()}</div>"

  }

  def whenFeatureEnabled = { args, body ->
    if(consoleConfig.isFeatureEnabled(args.feature))
      out << body()
  }

  def withSynchronized = { args, body ->
    def o = args.object
    def var = args.var ?: 'object'
    if(o != null)
    {
      synchronized(o)
      {
        out << body((var): o)
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