/*
 * Copyright (c) 2011 Ran Tavory
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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.console.domain.User
import org.apache.shiro.SecurityUtils
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.orchestration.engine.agents.AgentsService;
import org.linkedin.glu.orchestration.engine.deployment.DeploymentService;
import org.linkedin.glu.provisioner.core.model.SystemModel;

/**
 * Graph controller
 *
 * @author rantav@gmail.com 
 **/
class GraphController extends ControllerBase {

  ConsoleConfig consoleConfig
  AgentsService agentsService
  DeploymentService deploymentService
  
  /*
   Example configuration:
   console.defaults =
     [graphs: [versions: ['x-axis': 'metadata.serviceName', 
                          'y-axis': 'metadata.version', 
                           type: 'google-chart', 
                           description: 'version variation over the static model']
    ],
   */
  def index = {
    def graphs = consoleConfig.defaults.graphs
    [graphs: graphs]
  }

  def graph = {
    // Currently there's only support for a predefined set of graphs, no generic metrics, generic aggregation etc
    switch (params.graph) {
      case "versions-desired":
        return versionsDesired()
      case "versions-live":
        return versionsLive()
      case "deployments-histo":
        return deploymentsHisto()
      default:
      	flash.error = "No such graph ${params.graph}"
    }
  }

  def deploymentsHisto = {
    GregorianCalendar start = new GregorianCalendar();
    GregorianCalendar end = new GregorianCalendar();
    start.add(Calendar.DAY_OF_MONTH, -14);
    def histo = getDeploymentsHistogramByDate(start.getTime(), end.getTime())
    render(view: 'deploymentsHisto', model: [name: params.graph, histo: histo])
  }
  def versionsDesired = {
    SystemModel model = request.system
    List<MaxMinVersion> versions = extractMaxMinVersions(model)
    render(view: 'graphVersions', model: [name: params.graph, versions: versions, source: 'DESIRED'])
  }

  def versionsLive = {
    def model = agentsService.getCurrentSystemModel(request.fabric)
    model = model.filterBy(request.system.filters)
    List<MaxMinVersion> versions = extractMaxMinVersions(model)
    render(view: 'graphVersions', model: [name: params.graph, versions: versions, source: 'LIVE'])
  }

  private SortedMap<Date, SuccessFailCounts> getDeploymentsHistogramByDate(Date start, Date end) {
//    def deployments = deploymentService.getDeployments(request.fabric.name) // DO I need both getDeployments and getArchivedDeployments?
    def deployments = deploymentService.getArchivedDeployments(request.fabric.name, false, params).get('deployments')
    // Aggregate by date and success/failure
    Map<Date, SuccessFailCounts> ret = new TreeMap<Date, SuccessFailCounts>();
    deployments.each { it ->
      if (start.before(it.startDate) && end.after(it.startDate)) {
        long time = it.startDate.getTime()
        def success =  it.status == "SUCCESS"
        // aggregate by whole days, so get rid of the milli, seconds, minutes and hours
        long oneDayInMilli = 1000 * 60 * 60 * 24
        time /= oneDayInMilli
        time *= oneDayInMilli
        Date date = new Date(time)
        SuccessFailCounts counts = ret.get(date);
        if (counts == null) {
          counts = new SuccessFailCounts()
          ret.put(date, counts)
        }
        counts.add(success)
      }
    }
    return ret;
  }
    
  private def extractMaxMinVersions(SystemModel model) {
    Map<String, MaxMinVersion> moduleVersions = new HashMap<String, MaxMinVersion>();
    model.findEntries().each { entry ->
      String module = entry.metadata.get("product")
      def versionString = entry.metadata.get("version")
      if (module != null && versionString != null) {
        Number version = -1
        try {
          version = versionString as Number
        } catch (NumberFormatException e) {
        	// not an Number. will be ignored	
        }
        if (version > 0) {
          String agent = entry.getAgent()
          MaxMinVersion maxMin = moduleVersions.get(module)
          if (maxMin == null) {
            Set<String> agents = new HashSet<String>()
            agents.add(agent)
            maxMin = new MaxMinVersion(module, version, version, agents, agents)
            moduleVersions.put(module, maxMin)
          }
          maxMin.addVersion(version, agent)
        }
      }
    }
    List<MaxMinVersion> ret = new ArrayList<MaxMinVersion>(moduleVersions.values());
    return ret;
  }

  /**
  * A simple class that holds two numbers, one for success and another for failed counts.
  * It represents the number of successful v/s failed deployments during a time period.
  * @author Ran Tavory
  *
  */
 public class SuccessFailCounts {
   
   private int success, failed;
                  
   public void add(boolean success) {
     if (success) {
       ++success;
     } else {
       ++failed;
     }
   }
   
   public int getSuccess() {
     return success;
   }
                 
   public void setSuccess(int success) {
     this.success = success;
   }
     
   public int getFailed() {
     return failed;
   }
       
   public void setFailed(int failed) {
     this.failed = failed;
   }
 }
 
  class MaxMinVersion {
    Number min, max
    String module
    private Set<String> minAgents = new HashSet<String>()
    private Set<String> maxAgents = new HashSet<String>()

    public MaxMinVersion(String module, Number min, Number max, Set<String> minAgents, Set<String> maxAgents) {
      this.module = module;
      this.min = min;
      this.max = max;
      this.minAgents = new HashSet<String>(minAgents);
      this.maxAgents = new HashSet<String>(maxAgents);
    }
    
    /**
     * Adding a version means that the object checks if this version is either higher than the max or
     * lower than the min and if it is, updates max/min accordingly and sets the agents set to the
     * given agent.
     * If the version equals max/min then the agent is added to the set of already existing agents.
     * @param version
     * @param agent
     */
    def addVersion(Number version, String agent) {
      if (version == min) {
        minAgents.add(agent);
      } else if (version < min) {
        min = version;
        minAgents.clear();
        minAgents.add(agent);
      }

      if (version == max) {
        maxAgents.add(agent);
      } else if (version > max) {
        max = version;
        maxAgents.clear();
        maxAgents.add(agent);
      }
    }
    
    public String getMinAgentsHtml() {
      agentsToHtml(minAgents);
    }

    public String getMaxAgentsHtml() {
      if (min == max) {
        return "SAME";
      }
      agentsToHtml(maxAgents);
    }
    
    private String agentsToHtml(Set<String> agents) {
      StringBuffer sb = new StringBuffer();
      for (String agent : agents) {
        // http://glu/console/agents/view/anent-name
        sb.append(String.format("<a href=\"/console/agents/view/%s\">%s</a><br/>", agent, agent));
      }
      return sb.toString();
    }
    
    String toString () {
      "${module} ${min}-${max}"
    }
  }
}
