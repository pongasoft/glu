/*
* Copyright (c) 2010-2010 LinkedIn, Inc
* Portions Copyright (c) 2011-2015 Yan Pujante
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class UrlMappings
{
  public static final String MODULE = "org.linkedin.glu.console.conf.UrlMappings"
  public static final Logger log = LoggerFactory.getLogger(MODULE);


  private static Closure<RoleName> role = { userRole ->
    if(!userRole)
    {
      userRole = RoleName.ADMIN.toString()
      log.warn "No role defined for [${path}]: defaulting to ${userRole}"
    }

    if(log.isDebugEnabled())
      log.debug "${path} => ${userRole}"

    return RoleName.valueOf(RoleName.class, userRole)
  }

  /**
   * @return a map where key is rest method (GET, PUT, ...) and value is the role associated
   */
  private static def restRoles = { Map allRoles, action, path ->
    def roles = [:]
    action.keySet().each { method ->
      roles[method] = UrlMappings.role(allRoles["${method}:${path}".toString()])
    }
    return roles
  }

  static mappings = { beans ->

    Map roles = beans.grailsApplication.config?.console?.security?.roles ?: [:]

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
      __role = UrlMappings.role(roles.'/dashboard')
    }
    "/dashboard/redelta"(controller: 'dashboard', action: 'redelta') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/dashboard/redelta')
    }
    "/dashboard/renderDelta"(controller: 'dashboard', action: 'renderDelta') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/dashboard/renderDelta')
    }
    "/dashboard/index"(controller: 'dashboard', action: 'index') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/dashboard/index')
    }
    "/dashboard/customize"(controller: 'dashboard', action: 'customize') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/dashboard/customize')
    }
    "/dashboard/saveAsNewCustomDashboard"(controller: 'dashboard', action: 'saveAsNewCustomDashboard') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/dashboard/saveAsNewCustomDashboard')
    }
    "/dashboard/plans"(controller: 'dashboard', action: 'plans') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/dashboard/plans')
    }

    // agents
    "/agents"(controller: 'agents', action: 'list') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents')
    }
    "/agents/view/$id"(controller: 'agents', action: 'view') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/view/$id')
    }
    "/agents/ps/$id"(controller: 'agents', action: 'ps') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/ps/$id')
    }
    "/agents/fullStackTrace/$id"(controller: 'agents', action: 'fullStackTrace') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/fullStackTrace/$id')
    }
    "/agents/tailLog/$id"(controller: 'agents', action: 'tailLog') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/tailLog/$id')
    }
    "/agents/fileContent/$id"(controller: 'agents', action: 'fileContent') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/fileContent/$id')
    }
    "/agents/plans/$id"(controller: 'agents', action: 'plans') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/plans/$id')
    }

    // plan
    "/plan/view/$id"(controller: 'plan', action: 'view') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/view/$id')
    }
    "/plan/redirectView"(controller: 'plan', action: 'redirectView') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/redirectView')
    }
    "/plan/deployments/$id?"(controller: 'plan', action: 'deployments') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/deployments/$id?')
    }
    "/plan/renderDeploymentDetails/$id?"(controller: 'plan', action: 'renderDeploymentDetails') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/renderDeploymentDetails/$id?')
    }
    "/plan/renderDeployments"(controller: 'plan', action: 'renderDeployments') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/renderDeployments')
    }
    "/plan/archived/$id?"(controller: 'plan', action: 'archived') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/archived/$id?')
    }
    "/plan/create"(controller: 'plan', action: 'create') {
      __nvbe = 'Deployments'
      __role = UrlMappings.role(roles.'/plan/create')
    }

    // fabric
    "/fabric/select/$id?"(controller: 'fabric', action: 'select') {
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/select/$id?')
    }

    // model
    "/model/list"(controller: 'model', action: 'list') {
      __nvbe = 'Model'
      __role = UrlMappings.role(roles.'/model/list')
    }
    "/model/view/$id"(controller: 'model', action: 'view') {
      __nvbe = 'Model'
      __role = UrlMappings.role(roles.'/model/view/$id')
    }

    // auth
    "/auth/$action?/$id?"(controller: 'auth')

    // user credentials
    "/user/credentials"(controller: 'user', action: 'credentials') {
      __nvbe = 'User'
      __role = UrlMappings.role(roles.'/user/credentials')
    }
    "/user/updatePassword"(controller: 'user', action: 'updatePassword') {
      __nvbe = 'User'
      __role = UrlMappings.role(roles.'/user/updatePassword')
    }
    "/user/resetPassword"(controller: 'user', action: 'resetPassword') {
      __nvbe = 'User'
      __role = UrlMappings.role(roles.'/user/resetPassword')
    }

    // help
    "/help"(controller: 'help', action: "index") {
      __nvbe = 'Help'
      __role = UrlMappings.role(roles.'/help')
    }
    "/help/forum"(controller: 'help', action: "forum") {
      __nvbe = 'Help'
      __role = UrlMappings.role(roles.'/help/forum')
    }

    // /
    "/"(controller: 'home', action: 'slash') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/')
    }

    // home
    "/home"(controller: 'home', action: "index") { 
      __nvbe = 'User'
      __role = UrlMappings.role(roles.'/home')
    }

    /**************************************
     * RELEASE access
     */
    // agents
    "/agents/kill/$id/$pid"(controller: 'agents', action: 'kill') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/kill/$id/$pid')
    }
    "/agents/sync/$id"(controller: 'agents', action: 'sync') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/sync/$id')
    }
    "/agents/clearError/$id"(controller: 'agents', action: 'clearError') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/clearError/$id')
    }
    "/agents/uninstallScript/$id"(controller: 'agents', action: 'uninstallScript') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/uninstallScript/$id')
    }
    "/agents/createPlan/$id"(controller: 'agents', action: 'create_plan') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/createPlan/$id')
    }
    "/agents/interruptAction/$id"(controller: 'agents', action: 'interruptAction') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/interruptAction/$id')
    }
    "/agents/commands/$id"(controller: 'agents', action: 'commands') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/commands/$id')
    }
    "/agents/executeCommand/$id"(controller: 'agents', action: 'executeCommand') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/agents/executeCommand/$id')
    }
    "/agents/interruptCommand/$id"(controller: 'agents', action: 'interruptCommand') {
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/interruptCommand/$id')
    }

    // commands
    "/commands/$id/streams"(controller: 'commands', action: 'streams') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/commands/$id/streams')
    }

    "/commands/renderHistory"(controller: 'commands', action: 'renderHistory') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/commands/renderHistory')
    }

    "/commands/renderCommand/$id"(controller: 'commands', action: 'renderCommand') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/commands/renderCommand/$id')
    }

    "/commands/list"(controller: 'commands', action: 'list') {
      __nvbe = 'Agents'
      __role = UrlMappings.role(roles.'/commands/list')
    }

    // plan
    "/plan/execute/$id"(controller: 'plan', action: 'execute') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/execute/$id')
    }
    "/plan/filter/$id"(controller: 'plan', action: 'filter') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/filter/$id')
    }
    "/plan/archiveAllDeployments"(controller: 'plan', action: 'archiveAllDeployments') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/archiveAllDeployments')
    }
    "/plan/archiveDeployment/$id"(controller: 'plan', action: 'archiveDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/archiveDeployment/$id')
    }
    "/plan/resumeDeployment/$id"(controller: 'plan', action: 'resumeDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/resumeDeployment/$id')
    }
    "/plan/pauseDeployment/$id"(controller: 'plan', action: 'pauseDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/pauseDeployment/$id')
    }
    "/plan/abortDeployment/$id"(controller: 'plan', action: 'abortDeployment') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/abortDeployment/$id')
    }
    "/plan/cancelStep/$id"(controller: 'plan', action: 'cancelStep') { 
      __nvbe = 'Plans'
      __role = UrlMappings.role(roles.'/plan/cancelStep/$id')
    }

    // model
    "/model/choose"(controller: 'model', action: 'choose') {
      __nvbe = 'Model'
      __role = UrlMappings.role(roles.'/model/choose')
    }
    "/model/load"(controller: 'model', action: 'load') {
      __nvbe = 'Model'
      __role = UrlMappings.role(roles.'/model/load')
    }
    "/model/upload"(controller: 'model', action: 'upload') {
      __nvbe = 'Model'
      __role = UrlMappings.role(roles.'/model/upload')
    }
    "/model/save"(controller: 'model', action: 'save') {
      __nvbe = 'model'
      __role = UrlMappings.role(roles.'/model/save')
    }
    "/model/setAsCurrent"(controller: 'model', action: 'setAsCurrent') {
      __nvbe = 'model'
      __role = UrlMappings.role(roles.'/model/setAsCurrent')
    }

    // fabric
    "/fabric/refresh"(controller: 'fabric', action: 'refresh') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/refresh')
    }

    /**************************************
     * ADMIN access
     */

    // admin
    "/admin"(controller: 'admin', action: 'index') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/admin')
    }

    // agents
    "/agents/listVersions"(controller: 'agents', action: 'listVersions') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/agents/listVersions')
    }
    "/agents/upgrade"(controller: 'agents', action: 'upgrade') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/agents/upgrade')
    }
    "/agents/cleanup"(controller: 'agents', action: 'cleanup') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/agents/cleanup')
    }
    "/agents/forceUninstallScript/$id"(controller: 'agents', action: 'forceUninstallScript') { 
      __nvbe = 'Dashboard'
      __role = UrlMappings.role(roles.'/agents/forceUninstallScript/$id')
    }
    "/agent/$id/clear"(controller: 'agents', action: 'clear') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/agent/$id/clear')
    }

    // fabric
    "/fabric/listAgentFabrics"(controller: 'fabric', action: 'listAgentFabrics') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/listAgentFabrics')
    }
    "/fabric/setAgentsFabrics"(controller: 'fabric', action: 'setAgentsFabrics') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/setAgentsFabrics')
    }
    "/fabric/clearAgentFabric"(controller: 'fabric', action: 'clearAgentFabric') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/clearAgentFabric')
    }
    "/fabric/list"(controller: 'fabric', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/list')
    }
    "/fabric/show/$id"(controller: 'fabric', action: 'show') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/show/$id')
    }
    "/fabric/delete/$id?"(controller: 'fabric', action: 'delete') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/delete/$id?')
    }
    "/fabric/edit/$id"(controller: 'fabric', action: 'edit') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/edit/$id')
    }
    "/fabric/update/$id?"(controller: 'fabric', action: 'update') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/update/$id?')
    }
    "/fabric/create"(controller: 'fabric', action: 'create') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/create')
    }
    "/fabric/save"(controller: 'fabric', action: 'save') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/fabric/save')
    }

    // user
    "/user/index"(controller: 'user', action: 'index') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/index')
    }
    "/user/list"(controller: 'user', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/list')
    }
    "/user/show/$id"(controller: 'user', action: 'show') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/show/$id')
    }
    "/user/delete/$id"(controller: 'user', action: 'delete') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/delete/$id')
    }
    "/user/edit/$id"(controller: 'user', action: 'edit') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/edit/$id')
    }
    "/user/update/$id"(controller: 'user', action: 'update') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/update/$id')
    }
    "/user/create"(controller: 'user', action: 'create') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/create')
    }
    "/user/save"(controller: 'user', action: 'save') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/user/save')
    }

    // audit log
    "/auditLog/list"(controller: 'auditLog', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/auditLog/list')
    }

    // encryption keys
    "/encryption/list"(controller: 'encryption', action: 'list') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/encryption/list')
    }
    "/encryption/create"(controller: 'encryption', action: 'create') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/encryption/create')
    }
    "/encryption/encrypt"(controller: 'encryption', action: 'encrypt') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/encryption/encrypt')
    }
    "/encryption/ajaxSave"(controller: 'encryption', action: 'ajaxSave') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/encryption/ajaxSave')
    }
    "/encryption/ajaxEncrypt"(controller: 'encryption', action: 'ajaxEncrypt') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/encryption/ajaxEncrypt')
    }
    "/encryption/ajaxDecrypt"(controller: 'encryption', action: 'ajaxDecrypt') { 
      __nvbe = 'Admin'
      __role = UrlMappings.role(roles.'/encryption/ajaxDecrypt')
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
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/plans')
    }
    name restPlan: "/rest/v1/$fabric/plan/$id"(controller: 'plan') {
      action = [
        GET: 'rest_view_plan'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/plan/$id')
    }
    "/rest/v1/$fabric/plan/$id/executions"(controller: 'plan') {
      action = [
        GET: 'rest_list_executions'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/plan/$id/executions')
    }
    "/rest/v1/$fabric/plan/$id/execution"(controller: 'plan') {
      action = [
        POST: 'rest_execute_plan'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/plan/$id/execution')
    }
    name restExecution: "/rest/v1/$fabric/plan/$planId/execution/$id"(controller: 'plan') {
      action = [
        GET: 'rest_view_execution',
        HEAD: 'rest_execution_status',
        DELETE: 'rest_abort_execution'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/plan/$planId/execution/$id')
    }

    /***
     * deployments
     */
    "/rest/v1/$fabric/deployments/current"(controller: 'plan') {
      action = [
        GET: 'rest_list_current_deployments',
        DELETE: 'rest_archive_all_deployments'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/deployments/current')
    }

    name restViewCurrentDeployment: "/rest/v1/$fabric/deployment/current/$id"(controller: 'plan') {
      action = [
        HEAD: 'rest_view_current_deployment',
        GET: 'rest_view_current_deployment',
        DELETE: 'rest_archive_current_deployment'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/deployment/current/$id')
    }

    "/rest/v1/$fabric/deployments/archived"(controller: 'plan') {
      action = [
        HEAD: 'rest_count_archived_deployments',
        GET: 'rest_list_archived_deployments'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/deployments/archived')
    }

    name restViewArchivedDeployment: "/rest/v1/$fabric/deployment/archived/$id"(controller: 'plan') {
      action = [
        HEAD: 'rest_view_archived_deployment',
        GET: 'rest_view_archived_deployment'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/deployment/archived/$id')
    }

    /***
     * model
     */
    name restStaticModel: "/rest/v1/$fabric/model/static/$id?"(controller: 'model') {
      action = [
        POST: 'rest_upload_model',
        GET: 'rest_get_static_model'
      ]
      // mismatch for role name because $id? was added afterwards
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/model/static')
    }
    name restLiveModel: "/rest/v1/$fabric/model/live"(controller: 'model') {
      action = [
        GET: 'rest_get_live_model'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/model/live')
    }
    "/rest/v1/$fabric/models/static"(controller: 'model') {
      action = [
        HEAD: 'rest_count_static_models',
        GET: 'rest_list_static_models'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/models/static')
    }

    /***
     * delta
     */
    name restDelta: "/rest/v1/$fabric/model/delta"(controller: 'delta') {
      action = [
        GET: 'rest_get_delta'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/model/delta')
    }

    /**
     * agents
     */
    "/rest/v1/$fabric/agents"(controller: 'agents') {
      action = [
        HEAD: 'rest_count_agents',
        GET: 'rest_list_agents'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/agents')
    }

    name restViewAgent: "/rest/v1/$fabric/agent/$id"(controller: 'agents') {
      action = [
        GET: 'rest_view_agent',
        DELETE: 'rest_delete_agent'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/agent/$id')
    }

    "/rest/v1/$fabric/agent/$id/commands"(controller: 'agents') {
      action = [
        POST: 'rest_execute_shell_command'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/agent/$id/commands')
    }

    "/rest/v1/$fabric/command/$id/streams"(controller: 'commands') {
      action = [
        GET: 'rest_show_command_execution_streams'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/command/$id/streams')
    }

    "/rest/v1/$fabric/agents/versions"(controller: 'agents') {
      action = [
        GET: 'rest_list_agents_versions',
        POST: 'rest_upgrade_agents_versions'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/agents/versions')
    }

    /**
     * fabric
     */
    "/rest/v1/-"(controller: 'fabric') {
      action = [
        GET: 'rest_list_fabrics'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/-')
    }

    "/rest/v1/$fabric"(controller: 'fabric') {
      action = [
        GET: 'rest_view_fabric',
        PUT: 'rest_add_or_update_fabric',
        DELETE: 'rest_delete_fabric'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric')
    }

    "/rest/v1/-/agents"(controller: 'fabric') {
      action = [
        GET: 'rest_list_agents_fabrics'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/-/agents')
    }

    "/rest/v1/$fabric/agent/$id/fabric"(controller: 'fabric') {
      action = [
        PUT: 'rest_set_agent_fabric',
        DELETE: 'rest_clear_agent_fabric'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/agent/$id/fabric')
    }


    /**
     * audit logs
     */
    "/rest/v1/-/audit/logs"(controller: 'auditLog') {
      action = [
        HEAD: 'rest_count_audit_logs',
        GET: 'rest_list_audit_logs'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/-/audit/logs')
    }

    /**
     * DEPRECATED: kept for backward compatibility only
     */
    "/rest/v1/$fabric/system/model"(controller: 'model') {
      action = [
        POST: 'rest_upload_model',
        GET: 'rest_get_static_model'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/system/model')
    }
    "/rest/v1/$fabric/system/live"(controller: 'model') {
      action = [
        GET: 'rest_get_live_model'
      ]
      __roles = UrlMappings.restRoles(roles, action, '/rest/v1/$fabric/system/live')
    }

    "500"(view: '/error')

    /**
     * Deprecated => for backward compatibility only
     */
    "/admin/**"(controller: 'auth', action: 'noAuthInURL')

    "/release/**"(controller: 'auth', action: 'noAuthInURL')
  }
}
