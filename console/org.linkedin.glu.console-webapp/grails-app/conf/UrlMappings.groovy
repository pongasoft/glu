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

import org.linkedin.glu.console.domain.RoleName
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UrlMappings
{
  public static final String MODULE = "org.linkedin.glu.console.conf.UrlMappings"
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private static def role = { path ->
    def userRole = ConfigurationHolder.config.console.security.roles."${path}"
    if(!userRole)
    {
      userRole = RoleName.ADMIN.toString()
      log.warn "No role defined for [${path}]: defaulting to ${userRole}"
    }
    
    if(log.isDebugEnabled())
      log.debug "${path} => ${userRole}"

    return RoleName.valueOf(RoleName.class, userRole)
  }

  static mappings = {

    /**
     * YP Note: the param __nvbe (navbarEntry) is driving which tab is selected...
     * check ConsoleTagLib.navbarEntryClass
     */
    
    /**************************************
     * USER access
     */
    // dashboard
    "/dashboard"(controller: 'dashboard', action: 'delta') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard")
    }
    "/dashboard/redelta"(controller: 'dashboard', action: 'redelta') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard/redelta")
    }
    "/dashboard/renderDelta"(controller: 'dashboard', action: 'renderDelta') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard/renderDelta")
    }
    "/dashboard/index"(controller: 'dashboard', action: 'index') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard/index")
    }
    "/dashboard/customize"(controller: 'dashboard', action: 'customize') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard/customize")
    }
    "/dashboard/saveAsNewCustomDashboard"(controller: 'dashboard', action: 'saveAsNewCustomDashboard') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard/saveAsNewCustomDashboard")
    }
    "/dashboard/plans"(controller: 'dashboard', action: 'plans') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/dashboard/plans")
    }

    // agents
    "/agents"(controller: 'agents', action: 'list') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents")
    }
    "/agents/view/$id"(controller: 'agents', action: 'view') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents/view/id")
    }
    "/agents/ps/$id"(controller: 'agents', action: 'ps') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents/ps/id")
    }
    "/agents/fullStackTrace/$id"(controller: 'agents', action: 'fullStackTrace') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents/fullStackTrace/id")
    }
    "/agents/tailLog/$id"(controller: 'agents', action: 'tailLog') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents/tailLog/id")
    }
    "/agents/fileContent/$id"(controller: 'agents', action: 'fileContent') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents/fileContent/id")
    }
    "/agents/plans/$id"(controller: 'agents', action: 'plans') {
      __nvbe = 'Agents'
      __role = UrlMappings.role("/agents/plans/id")
    }

    // plan
    "/plan/view/$id"(controller: 'plan', action: 'view') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/view/id")
    }
    "/plan/redirectView"(controller: 'plan', action: 'redirectView') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/redirectView")
    }
    "/plan/deployments/$id?"(controller: 'plan', action: 'deployments') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/deployments/id")
    }
    "/plan/renderDeploymentDetails/$id?"(controller: 'plan', action: 'renderDeploymentDetails') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/renderDeploymentDetails/id")
    }
    "/plan/renderDeployments"(controller: 'plan', action: 'renderDeployments') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/renderDeployments")
    }
    "/plan/archived/$id?"(controller: 'plan', action: 'archived') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/archived/id")
    }
    "/plan/create"(controller: 'plan', action: 'create') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role("/plan/create")
    }

    // fabric
    "/fabric/select/$id?"(controller: 'fabric', action: 'select') {
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/select/id")
    }

    // model
    "/model/list"(controller: 'model', action: 'list') {
      __nvbe = 'Model'
      __role = UrlMappings.role("/model/list")
    }
    "/model/view/$id"(controller: 'model', action: 'view') {
      __nvbe = 'Model'
      __role = UrlMappings.role("/model/view/id")
    }

    // auth
    "/auth/$action?/$id?"(controller: 'auth')

    // user credentials
    "/user/credentials"(controller: 'user', action: 'credentials') {
      __nvbe = 'User'
      __role = UrlMappings.role("/user/credentials")
    }
    "/user/updatePassword"(controller: 'user', action: 'updatePassword') {
      __nvbe = 'User'
      __role = UrlMappings.role("/user/updatePassword")
    }

    // help
    "/help"(controller: 'help', action: "index") {
      __nvbe = 'Help'
      __role = UrlMappings.role("/help")
    }
    "/help/forum"(controller: 'help', action: "forum") {
      __nvbe = 'Help'
      __role = UrlMappings.role("/help/forum")
    }

    // /
    "/"(controller: 'dashboard', action: 'delta') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/")
    }

    // home
    "/home"(controller: 'home', action: "index") { 
      __nvbe = 'User'
      __role = UrlMappings.role("/home")
    }

    /**************************************
     * RELEASE access
     */
    // agents
    "/release/agents/kill/$id/$pid"(controller: 'agents', action: 'kill') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/kill/id/pid")
    }
    "/release/agents/sync/$id"(controller: 'agents', action: 'sync') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/sync/id")
    }
    "/release/agents/clearError/$id"(controller: 'agents', action: 'clearError') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/clearError/id")
    }
    "/release/agents/uninstallScript/$id"(controller: 'agents', action: 'uninstallScript') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/uninstallScript/id")
    }
    "/release/agents/createPlan/$id"(controller: 'agents', action: 'create_plan') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/createPlan/id")
    }
    "/release/agents/interruptAction/$id"(controller: 'agents', action: 'interruptAction') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/interruptAction/id")
    }

    // plan
    "/release/plan/execute/$id"(controller: 'plan', action: 'execute') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/execute/id")
    }
    "/release/plan/filter/$id"(controller: 'plan', action: 'filter') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/filter/id")
    }
    "/release/plan/archiveAllDeployments"(controller: 'plan', action: 'archiveAllDeployments') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/archiveAllDeployments")
    }
    "/release/plan/archiveDeployment/$id"(controller: 'plan', action: 'archiveDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/archiveDeployment/id")
    }
    "/release/plan/resumeDeployment/$id"(controller: 'plan', action: 'resumeDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/resumeDeployment/id")
    }
    "/release/plan/pauseDeployment/$id"(controller: 'plan', action: 'pauseDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/pauseDeployment/id")
    }
    "/release/plan/abortDeployment/$id"(controller: 'plan', action: 'abortDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/abortDeployment/id")
    }
    "/release/plan/cancelStep/$id"(controller: 'plan', action: 'cancelStep') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role("/plan/cancelStep/id")
    }

    // model
    "/release/model/choose"(controller: 'model', action: 'choose') {
      __nvbe = 'Model'
      __role = UrlMappings.role("/model/choose")
    }
    "/release/model/load"(controller: 'model', action: 'load') {
      __nvbe = 'Model'
      __role = UrlMappings.role("/model/load")
    }
    "/release/model/upload"(controller: 'model', action: 'upload') {
      __nvbe = 'Model'
      __role = UrlMappings.role("/model/upload")
    }
    "/release/model/save"(controller: 'model', action: 'save') {
      __nvbe = 'model'
      __role = UrlMappings.role("/model/save")
    }
    "/release/model/setAsCurrent"(controller: 'model', action: 'setAsCurrent') {
      __nvbe = 'model'
      __role = UrlMappings.role("/model/setAsCurrent")
    }

    // fabric
    "/release/fabric/refresh"(controller: 'fabric', action: 'refresh') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/refresh")
    }

    /**************************************
     * ADMIN access
     */
    // admin
    "/admin"(controller: 'admin', action: 'index') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/admin")
    }

    // agents
    "/admin/agents/listVersions"(controller: 'agents', action: 'listVersions') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/agents/listVersions")
    }
    "/admin/agents/upgrade"(controller: 'agents', action: 'upgrade') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/agents/upgrade")
    }
    "/admin/agents/cleanup"(controller: 'agents', action: 'cleanup') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/agents/cleanup")
    }
    "/admin/agents/forceUninstallScript/$id"(controller: 'agents', action: 'forceUninstallScript') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role("/agents/forceUninstallScript/id")
    }
    "/admin/agent/$id/clear"(controller: 'agents', action: 'clear') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/agent/id/clear")
    }

    // fabric
    "/admin/fabric/listAgentFabrics"(controller: 'fabric', action: 'listAgentFabrics') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/listAgentFabrics")
    }
    "/admin/fabric/setAgentsFabrics"(controller: 'fabric', action: 'setAgentsFabrics') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/setAgentsFabrics")
    }
    "/admin/fabric/clearAgentFabric"(controller: 'fabric', action: 'clearAgentFabric') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/clearAgentFabric")
    }
    "/admin/fabric/list"(controller: 'fabric', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/list")
    }
    "/admin/fabric/show/$id"(controller: 'fabric', action: 'show') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/show/id")
    }
    "/admin/fabric/delete/$id?"(controller: 'fabric', action: 'delete') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/delete/id")
    }
    "/admin/fabric/edit/$id"(controller: 'fabric', action: 'edit') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/edit/id")
    }
    "/admin/fabric/update/$id?"(controller: 'fabric', action: 'update') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/update/id")
    }
    "/admin/fabric/create"(controller: 'fabric', action: 'create') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/create")
    }
    "/admin/fabric/save"(controller: 'fabric', action: 'save') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/fabric/save")
    }

    // user
    "/admin/user/index"(controller: 'user', action: 'index') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/index")
    }
    "/admin/user/list"(controller: 'user', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/list")
    }
    "/admin/user/show/$id"(controller: 'user', action: 'show') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/show/id")
    }
    "/admin/user/delete/$id"(controller: 'user', action: 'delete') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/delete/id")
    }
    "/admin/user/edit/$id"(controller: 'user', action: 'edit') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/edit/id")
    }
    "/admin/user/update/$id"(controller: 'user', action: 'update') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/update/id")
    }
    "/admin/user/create"(controller: 'user', action: 'create') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/create")
    }
    "/admin/user/save"(controller: 'user', action: 'save') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/user/save")
    }

    // audit log
    "/admin/auditLog/list"(controller: 'auditLog', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/auditLog/list")
    }

    // encryption keys
    "/admin/encryption/list"(controller: 'encryption', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/encryption/list")
    }
    "/admin/encryption/create"(controller: 'encryption', action: 'create') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/encryption/create")
    }
    "/admin/encryption/encrypt"(controller: 'encryption', action: 'encrypt') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/encryption/encrypt")
    }
    "/admin/encryption/ajaxSave"(controller: 'encryption', action: 'ajaxSave') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/encryption/ajaxSave")
    }
    "/admin/encryption/ajaxEncrypt"(controller: 'encryption', action: 'ajaxEncrypt') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/encryption/ajaxEncrypt")
    }
    "/admin/encryption/ajaxDecrypt"(controller: 'encryption', action: 'ajaxDecrypt') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role("/encryption/ajaxDecrypt")
    }

    /**************************************
     * REST Api
     */
    /***
     * plan
     */
    "/rest/v1/$fabric/plans"(controller: 'plan') {
      action = [
        GET: 'rest_list_plans',
        POST: 'rest_create_plan'
      ]
    }
    name restPlan: "/rest/v1/$fabric/plan/$id"(controller: 'plan') {
      action = [
        GET: 'rest_view_plan'
      ]
    }
    "/rest/v1/$fabric/plan/$id/executions"(controller: 'plan') {
      action = [
        GET: 'rest_list_executions'
      ]
    }
    "/rest/v1/$fabric/plan/$id/execution"(controller: 'plan') {
      action = [
        POST: 'rest_execute_plan'
      ]
    }
    name restExecution: "/rest/v1/$fabric/plan/$planId/execution/$id"(controller: 'plan') {
      action = [
        GET: 'rest_view_execution',
        HEAD: 'rest_execution_status'
      ]
    }

    /***
     * deployments
     */
    "/rest/v1/$fabric/deployments/current"(controller: 'plan') {
      action = [
        GET: 'rest_list_current_deployments',
        DELETE: 'rest_archive_all_deployments'
      ]
    }

    name restViewCurrentDeployment: "/rest/v1/$fabric/deployment/current/$id"(controller: 'plan') {
      action = [
        HEAD: 'rest_view_current_deployment',
        GET: 'rest_view_current_deployment',
        DELETE: 'rest_archive_current_deployment'
      ]
    }

    "/rest/v1/$fabric/deployments/archived"(controller: 'plan') {
      action = [
        HEAD: 'rest_count_archived_deployments',
        GET: 'rest_list_archived_deployments'
      ]
    }

    name restViewArchivedDeployment: "/rest/v1/$fabric/deployment/archived/$id"(controller: 'plan') {
      action = [
        HEAD: 'rest_view_archived_deployment',
        GET: 'rest_view_archived_deployment'
      ]
    }

    /***
     * model
     */
    name restStaticModel: "/rest/v1/$fabric/model/static"(controller: 'model') {
      action = [
        POST: 'rest_upload_model',
        GET: 'rest_get_static_model'
      ]
    }
    name restLiveModel: "/rest/v1/$fabric/model/live"(controller: 'model') {
      action = [
        GET: 'rest_get_live_model'
      ]
    }

    /***
     * delta
     */
    name restDelta: "/rest/v1/$fabric/model/delta"(controller: 'delta') {
      action = [
        GET: 'rest_get_delta'
      ]
    }

    /**
     * agents
     */
    "/rest/v1/$fabric/agents"(controller: 'agents') {
      action = [
        HEAD: 'rest_count_agents',
        GET: 'rest_list_agents'
      ]
    }

    name restViewAgent: "/rest/v1/$fabric/agent/$id"(controller: 'agents') {
      action = [
        GET: 'rest_view_agent',
        DELETE: 'rest_delete_agent'
      ]
    }

    "/rest/v1/$fabric/agents/versions"(controller: 'agents') {
      action = [
        GET: 'rest_list_agents_versions',
        POST: 'rest_upgrade_agents_versions'
      ]
    }

    /**
     * fabric
     */
    "/rest/v1/-"(controller: 'fabric') {
      action = [
        GET: 'rest_list_fabrics'
      ]
    }

    "/rest/v1/$fabric"(controller: 'fabric') {
      action = [
        GET: 'rest_view_fabric',
        PUT: 'rest_add_or_update_fabric',
        DELETE: 'rest_delete_fabric'
      ]
    }

    "/rest/v1/-/agents"(controller: 'fabric') {
      action = [
        GET: 'rest_list_agents_fabrics'
      ]
    }

    "/rest/v1/$fabric/agent/$id/fabric"(controller: 'fabric') {
      action = [
        PUT: 'rest_set_agent_fabric',
        DELETE: 'rest_clear_agent_fabric'
      ]
    }

    /**
     * DEPRECATED: kept for backward compatibility only
     */
    "/rest/v1/$fabric/system/model"(controller: 'model') {
      action = [
        POST: 'rest_upload_model',
        GET: 'rest_get_static_model'
      ]
    }
    "/rest/v1/$fabric/system/live"(controller: 'model') {
      action = [
        GET: 'rest_get_live_model'
      ]
    }

    "500"(view: '/error')
  }
}
