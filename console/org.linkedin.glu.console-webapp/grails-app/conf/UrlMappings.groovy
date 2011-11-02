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

class UrlMappings
{
  static mappings = {

    /**
     * YP Note: the param __nvbe (navbarEntry) is driving which tab is selected...
     * check ConsoleTagLib.navbarEntryClass
     */
    
    /**************************************
     * USER access
     */
    // dashboard
    "/dashboard"(controller: 'dashboard', action: 'delta') { __nvbe = 'Dashboard' }
    "/dashboard/renderDelta"(controller: 'dashboard', action: 'renderDelta') { __nvbe = 'Dashboard' }
    "/dashboard/index"(controller: 'dashboard', action: 'index') { __nvbe = 'Dashboard' }
    "/dashboard/customize"(controller: 'dashboard', action: 'customize') { __nvbe = 'Dashboard' }
    "/dashboard/plans"(controller: 'dashboard', action: 'plans') { __nvbe = 'Dashboard' }

    // agents
    "/agents"(controller: 'agents', action: 'list') { __nvbe = 'Agents' }
    "/agents/view/$id"(controller: 'agents', action: 'view') { __nvbe = 'Agents' }
    "/agents/ps/$id"(controller: 'agents', action: 'ps') { __nvbe = 'Agents' }
    "/agents/fullStackTrace/$id"(controller: 'agents', action: 'fullStackTrace') { __nvbe = 'Agents' }
    "/agents/tailLog/$id"(controller: 'agents', action: 'tailLog') { __nvbe = 'Agents' }
    "/agents/fileContent/$id"(controller: 'agents', action: 'fileContent') { __nvbe = 'Agents' }
    "/agents/plans/$id"(controller: 'agents', action: 'plans') { __nvbe = 'Agents' }

    // plan
    "/plan/view/$id"(controller: 'plan', action: 'view') { __nvbe = 'Deployments' }
    "/plan/redirectView"(controller: 'plan', action: 'redirectView') { __nvbe = 'Deployments' }
    "/plan/deployments/$id?"(controller: 'plan', action: 'deployments') { __nvbe = 'Deployments' }
    "/plan/renderDeploymentDetails/$id?"(controller: 'plan', action: 'renderDeploymentDetails') { __nvbe = 'Deployments' }
    "/plan/renderDeployments"(controller: 'plan', action: 'renderDeployments') { __nvbe = 'Deployments' }
    "/plan/archived/$id?"(controller: 'plan', action: 'archived') { __nvbe = 'Deployments' }
    "/plan/create"(controller: 'plan', action: 'create') { __nvbe = 'Deployments' }

    // fabric
    "/fabric/select/$id?"(controller: 'fabric', action: 'select') { __nvbe = 'Admin' }

    // model
    "/model/list"(controller: 'model', action: 'list') { __nvbe = 'Model' }
    "/model/view/$id"(controller: 'model', action: 'view') { __nvbe = 'Model' }

    // TODO HIGH YP:  added back temporarily for testing
    "/system/filter/values/$id/$value?"(controller: 'system', action: 'filter_values') { __nvbe = 'System' }

    // auth
    "/auth/$action?/$id?"(controller: 'auth')

    // user credentials
    "/user/credentials"(controller: 'user', action: 'credentials') { __nvbe = 'User' }
    "/user/updatePassword"(controller: 'user', action: 'updatePassword') { __nvbe = 'User' }

    // help
    "/help"(controller: 'help', action: "index") { __nvbe = 'Help' }
    "/help/forum"(controller: 'help', action: "forum") { __nvbe = 'Help' }

    // /
    "/"(controller: 'dashboard', action: 'delta') { __nvbe = 'Dashboard' }

    // home
    "/home"(controller: 'home', action: "index") { __nvbe = 'User' }

    /**************************************
     * RELEASE access
     */
    // agents
    "/release/agents/kill/$id/$pid"(controller: 'agents', action: 'kill') { __nvbe = 'Dashboard' }
    "/release/agents/sync/$id"(controller: 'agents', action: 'sync') { __nvbe = 'Dashboard' }
    "/release/agents/clearError/$id"(controller: 'agents', action: 'clearError') { __nvbe = 'Dashboard' }
    "/release/agents/uninstallScript/$id"(controller: 'agents', action: 'uninstallScript') { __nvbe = 'Dashboard' }
    "/release/agents/start/$id"(controller: 'agents', action: 'start') { __nvbe = 'Dashboard' }
    "/release/agents/stop/$id"(controller: 'agents', action: 'stop') { __nvbe = 'Dashboard' }
    "/release/agents/bounce/$id"(controller: 'agents', action: 'bounce') { __nvbe = 'Dashboard' }
    "/release/agents/undeploy/$id"(controller: 'agents', action: 'undeploy') { __nvbe = 'Dashboard' }
    "/release/agents/redeploy/$id"(controller: 'agents', action: 'redeploy') { __nvbe = 'Dashboard' }
    "/release/agents/interruptAction/$id"(controller: 'agents', action: 'interruptAction') { __nvbe = 'Dashboard' }

    // plan
    "/release/plan/execute/$id"(controller: 'plan', action: 'execute') { __nvbe = 'Plans' }
    "/release/plan/filter/$id"(controller: 'plan', action: 'filter') { __nvbe = 'Plans' }
    "/release/plan/archiveAllDeployments"(controller: 'plan', action: 'archiveAllDeployments') { __nvbe = 'Plans' }
    "/release/plan/archiveDeployment/$id"(controller: 'plan', action: 'archiveDeployment') { __nvbe = 'Plans' }
    "/release/plan/resumeDeployment/$id"(controller: 'plan', action: 'resumeDeployment') { __nvbe = 'Plans' }
    "/release/plan/pauseDeployment/$id"(controller: 'plan', action: 'pauseDeployment') { __nvbe = 'Plans' }
    "/release/plan/abortDeployment/$id"(controller: 'plan', action: 'abortDeployment') { __nvbe = 'Plans' }
    "/release/plan/cancelStep/$id"(controller: 'plan', action: 'cancelStep') { __nvbe = 'Plans' }

    // fabric
    "/release/fabric/refresh"(controller: 'fabric', action: 'refresh') { __nvbe = 'Admin' }

    /**************************************
     * ADMIN access
     */
    // admin
    "/admin"(controller: 'admin', action: 'index') { __nvbe = 'Admin' }

    // agents
    "/admin/agents/listVersions"(controller: 'agents', action: 'listVersions') { __nvbe = 'Admin' }
    "/admin/agents/upgrade"(controller: 'agents', action: 'upgrade') { __nvbe = 'Admin' }
    "/admin/agents/cleanup"(controller: 'agents', action: 'cleanup') { __nvbe = 'Admin' }
    "/admin/agents/forceUninstallScript/$id"(controller: 'agents', action: 'forceUninstallScript') { __nvbe = 'Dashboard' }
    "/admin/agent/$id/clear"(controller: 'agents', action: 'clear') { __nvbe = 'Admin' }

    // fabric
    "/admin/fabric/listAgentFabrics"(controller: 'fabric', action: 'listAgentFabrics') { __nvbe = 'Admin' }
    "/admin/fabric/setAgentsFabrics"(controller: 'fabric', action: 'setAgentsFabrics') { __nvbe = 'Admin' }
    "/admin/fabric/clearAgentFabric"(controller: 'fabric', action: 'clearAgentFabric') { __nvbe = 'Admin' }
    "/admin/fabric/list"(controller: 'fabric', action: 'list') { __nvbe = 'Admin' }
    "/admin/fabric/show/$id"(controller: 'fabric', action: 'show') { __nvbe = 'Admin' }
    "/admin/fabric/delete/$id?"(controller: 'fabric', action: 'delete') { __nvbe = 'Admin' }
    "/admin/fabric/edit/$id"(controller: 'fabric', action: 'edit') { __nvbe = 'Admin' }
    "/admin/fabric/update/$id?"(controller: 'fabric', action: 'update') { __nvbe = 'Admin' }
    "/admin/fabric/create"(controller: 'fabric', action: 'create') { __nvbe = 'Admin' }
    "/admin/fabric/save"(controller: 'fabric', action: 'save') { __nvbe = 'Admin' }

    // user
    "/admin/user/index"(controller: 'user', action: 'index') { __nvbe = 'Admin' }
    "/admin/user/list"(controller: 'user', action: 'list') { __nvbe = 'Admin' }
    "/admin/user/show/$id"(controller: 'user', action: 'show') { __nvbe = 'Admin' }
    "/admin/user/delete/$id"(controller: 'user', action: 'delete') { __nvbe = 'Admin' }
    "/admin/user/edit/$id"(controller: 'user', action: 'edit') { __nvbe = 'Admin' }
    "/admin/user/update/$id"(controller: 'user', action: 'update') { __nvbe = 'Admin' }
    "/admin/user/create"(controller: 'user', action: 'create') { __nvbe = 'Admin' }
    "/admin/user/save"(controller: 'user', action: 'save') { __nvbe = 'Admin' }

    // model
    "/release/model/choose"(controller: 'model', action: 'choose') { __nvbe = 'Model' }
    "/release/model/load"(controller: 'model', action: 'load') { __nvbe = 'Model' }
    "/release/model/upload"(controller: 'model', action: 'upload') { __nvbe = 'Model' }
    "/release/model/save"(controller: 'model', action: 'save') { __nvbe = 'model' }
    "/release/model/setAsCurrent"(controller: 'model', action: 'setAsCurrent') { __nvbe = 'model' }

    // audit log
    "/admin/auditLog/list"(controller: 'auditLog', action: 'list') { __nvbe = 'Admin' }

    // encryption keys
    "/admin/encryption/list"(controller: 'encryption', action: 'list') { __nvbe = 'Admin' }
    "/admin/encryption/create"(controller: 'encryption', action: 'create') { __nvbe = 'Admin' }
    "/admin/encryption/encrypt"(controller: 'encryption', action: 'encrypt') { __nvbe = 'Admin' }
    "/admin/encryption/ajaxSave"(controller: 'encryption', action: 'ajaxSave') { __nvbe = 'Admin' }
    "/admin/encryption/ajaxEncrypt"(controller: 'encryption', action: 'ajaxEncrypt') { __nvbe = 'Admin' }
    "/admin/encryption/ajaxDecrypt"(controller: 'encryption', action: 'ajaxDecrypt') { __nvbe = 'Admin' }

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
