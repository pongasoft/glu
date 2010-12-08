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
    "/dashboard"(controller: 'dashboard', action: 'audit') { __nvbe = 'Dashboard' }
    "/dashboard/renderAudit"(controller: 'dashboard', action: 'renderAudit') { __nvbe = 'Dashboard' }
    "/dashboard/index"(controller: 'dashboard', action: 'index') { __nvbe = 'Dashboard' }

    // agents
    "/agents"(controller: 'agents', action: 'list') { __nvbe = 'Dashboard' }
    "/agents/list"(controller: 'agents', action: 'list') { __nvbe = 'Dashboard' }
    "/agents/view/$id"(controller: 'agents', action: 'view') { __nvbe = 'System' }
    "/agents/ps/$id"(controller: 'agents', action: 'ps') { __nvbe = 'System' }
    "/agents/fullStackTrace/$id"(controller: 'agents', action: 'fullStackTrace') { __nvbe = 'System' }
    "/agents/tailLog/$id"(controller: 'agents', action: 'tailLog') { __nvbe = 'System' }
    "/agents/fileContent/$id"(controller: 'agents', action: 'fileContent') { __nvbe = 'System' }

    // plan
    "/plan/view/$id"(controller: 'plan', action: 'view') { __nvbe = 'Plans' }
    "/plan/redirectView"(controller: 'plan', action: 'redirectView') { __nvbe = 'Plans' }
    "/plan/deployments/$id?"(controller: 'plan', action: 'deployments') { __nvbe = 'Plans' }
    "/plan/renderDeploymentDetails/$id?"(controller: 'plan', action: 'renderDeploymentDetails') { __nvbe = 'Plans' }
    "/plan/renderDeployments"(controller: 'plan', action: 'renderDeployments') { __nvbe = 'Plans' }
    "/plan/archived/$id?"(controller: 'plan', action: 'archived') { __nvbe = 'Plans' }
    "/plan/renderDelta/$id?"(controller: 'plan', action: 'renderDelta') { __nvbe = 'Plans' }

    // fabric
    "/fabric/select/$id?"(controller: 'fabric', action: 'select') { __nvbe = 'Admin' }

    // system
    "/system"(controller: 'system', action: 'delta') { __nvbe = 'System' }
    "/system/list"(controller: 'system', action: 'list') { __nvbe = 'System' }
    "/system/view/$id"(controller: 'system', action: 'view') { __nvbe = 'System' }
    "/system/filter"(controller: 'system', action: 'filter') { __nvbe = 'System' }
    "/system/filter/values/$id/$value?"(controller: 'system', action: 'filter_values') { __nvbe = 'System' }

    // applications
    "/applications/show/$id"(controller: 'applications', action: 'show') { __nvbe = 'Dashboard' }

    // cluster
    "/cluster/show/$id"(controller: 'cluster', action: 'show') { __nvbe = 'Dashboard' }

    // metrics
    "/metrics/bomHistory/$id"(controller: 'metrics', action: 'bomHistory') { __nvbe = 'System' }

    // auth
    "/auth/$action?/$id?"(controller: 'auth')

    // user credentials
    "/user/credentials"(controller: 'user', action: 'credentials') { __nvbe = 'User' }
    "/user/updatePassword"(controller: 'user', action: 'updatePassword') { __nvbe = 'User' }

    // /
    "/"(controller: 'dashboard', action: 'audit') { __nvbe = 'Dashboard' }

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

    // fabric
    "/admin/fabric/listAgentFabrics"(controller: 'fabric', action: 'listAgentFabrics') { __nvbe = 'Admin' }
    "/admin/fabric/setAgentsFabrics"(controller: 'fabric', action: 'setAgentsFabrics') { __nvbe = 'Admin' }
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
    "/admin/model/choose"(controller: 'model', action: 'choose') { __nvbe = 'Model' }
    "/admin/model/load"(controller: 'model', action: 'load') { __nvbe = 'Model' }
    "/admin/model/upload"(controller: 'model', action: 'upload') { __nvbe = 'Model' }

    // system
    "/admin/system/save"(controller: 'system', action: 'save') { __nvbe = 'System' }
    "/admin/system/addEntry"(controller: 'system', action: 'addEntry') { __nvbe = 'System' }

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
    "/rest/v1/$fabric/plans"(controller: 'plan') {
      action = [GET: 'rest_list_plans', POST: 'rest_create_plan']
    }
    name restPlan: "/rest/v1/$fabric/plan/$id"(controller: 'plan') {
      action = [GET: 'rest_view_plan']
    }
    "/rest/v1/$fabric/plan/$id/execution"(controller: 'plan') {
      action = [POST: 'rest_execute_plan']
    }
    name restExecution: "/rest/v1/$fabric/plan/$planId/execution/$id"(controller: 'plan') {
      action = [GET: 'rest_view_execution', HEAD: 'rest_execution_status']
    }
    "/rest/v1/$fabric/system/model"(controller: 'model') {
      action = [PUT: 'rest_upload_model', GET: 'rest_get_model']
    }
    "/rest/v1/$fabric/system/live"(controller: 'system') {
      action = [GET: 'rest_get_live_system']
    }

    "500"(view: '/error')
  }
}
