/*
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

package org.linkedin.glu.orchestration.engine.plugins.builtin

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class is not a useful plugin per se, and is meant as documentation to describe each
 * plugin hooks.
 *
 * <p>A plugin is simply a class with (groovy) closures, each of them defining a specific hook. You
 * can define as many or as little of those closures as you want and you can 'spread' them accross
 * different classes (for example, you can have a plugin class handling only the 'deployment' related
 * plugin hooks, and another handling the hooks for a different part of the system). If a closure
 * is missing, then it will simply not be called.
 *
 * <p>To make your plugin available to glu, you must add its class name in the configuration file:
 *
 * <pre>
 *     orchestration.engine.plugins = [
 *     'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin',
 *     'org.acme.MyPlugin',
 *     etc...
 *   ]
 * </pre>
 *
 * <p>Make sure that the jar file(s) containing your plugin (and its dependencies) is available to
 * glu. If you are using the (console) server to deploy the glu webapp, then you can follow the
 * steps that explain how to use the MySql driver in the documentation
 * (http://linkedin.github.com/glu/docs/latest/html/console.html#example-for-a-different-database-mysql):
 * you would copy your jar file(s) in the same location.
 *
 * <p>A closure receives arguments provided by the caller which depend on the hook (see individual
 * documentation below). The arguments received is a map.
 *
 * <p>In a "post" hook (which means a hook that is being called after a given call), one of these
 * arguments is always provided:
 * <ul>
 *   <li>serviceResult: the value returned by the original caller</li>
 *   <li>serviceException: if the call threw an exception</li>
 * </ul>
 *
 * <p>Every hook should return <code>null</code> if you do not want to override the value that
 * the original caller returns, otherwise return your own value!
 *
 * <p>In a "pre" plugin, you can generally modify the arguments provided in the map:
 *
 * <pre>
 *   def xxx_pre_yyy = { args ->
 *     args.description = "${args.description} / foo"
 *     return null
 *   }
 * </pre>
 *
 * @author yan@pongasoft.com */
public class DocumentationPlugin
{
  public static final String MODULE = DocumentationPlugin.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  /////////////////////////////////////////////////////////////////////////////////////////////
  // Initialization
  /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * This closure is called once when the application boots up to give a chance to the plugin to
   * initialize itself.
   *
   * @param args.applicationContext the (spring) application context used to start the
   *                                (grails) application: all beans created are available in this
   *                                call
   * @param args.config is a <code>groovy.util.ConfigObject</code> and contains all the values
   *                    defined in the configuration file
   * @return for this closure, the returned value is ignored
   */
  def PluginService_initialize = { args ->
    // thanks to grails/groovy, the application context can be used as a Map:
    // args.applicationContext['authorizationService'] => return the authorizationService

    // args.config.console.defaults => return the Map with all the default configuration values
    // for the console itself

    log.info("PluginService_initialize")
    return null
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  // User management
  /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Called when a user authenticate. This closure allows you for example to implement your entire
   * login mechanism if you wish to! Please be very careful in what you return as a non
   * <code>null</code> value will bypass the authentication entirely!
   *
   * @param args.authToken is a shiro authentication token object (username/password)
   * @return the username if you want to bypass the glu authentication system entirely because you
   * did the check yourself, or <code>null</code> to let glu proceed with authentication
   */
  def UserService_pre_authenticate = { args ->

    // example: use acme proprietary authentication system with args.authToken.username and
    // args.authToken.password to authenticate the user => if authentication succeed then
    // return args.authToken.username

    log.info("UserService_pre_authenticate")
    return null
  }

  /**
   * Called after user authentication.
   *
   * @param args.authToken is a shiro authentication token object (username/password)
   * @param args.serviceResult the username (when authentication worked)
   * @param args.serviceException whenever authentication failed
   * @return the username or <code>null</code> to let the caller return its value
   */
  def UserService_post_authenticate = { args ->
    log.info("UserService_post_authenticate")
    return null
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  // Model
  /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Called before parsing the system model to give you a chance to preprocess it or process
   * it entirely
   *
   * @param args.source can be a <code>String</code>, an <code>InputStream</code> or anything
   * that can be turned into a URI. You can modify this attribute and the caller will pick it up
   * @return if you want to do all the processing yourself, then simply return a fully built
   * <code>SystemModel</code> object, otherwise return <code>null</code>
   */
  def SystemService_pre_parseSystemModel = { args ->

    // typical use case would be to process your own format, for example:

//    if(args.source instanceof URI && args.source.path.endsWith('.xml'))
//    {
//      // read xml and convert to json
//      def xml = args.source.text
//      def json = processXml(xml)
//      // store the json in the source attribute and the caller will process it
//      args.source = json
//    }

    log.info("SystemService_pre_parseSystemModel")
    return null
  }

  /**
   * Called after parsing the system model to give you a chance to tweak the model built by glu.
   *
   * @param args.source same value passed in the "pre" hook
   * @param args.serviceResult the <code>SystemModel</code> object
   * @param args.serviceException if there is an exception (most likely while parsing the model)
   * @return you can replace the model built by glu entirely by returning your own, or simply tweak
   * the one provided in <code>args.serviceResult</code> and returning <code>null</code>
   */
  def SystemService_post_parseSystemModel = { args ->

    // typical use case is to add your own medatada to the model built by glu, for example:

    // if(args.source instanceof URI)
    //   args.serviceResult.metadata.builtFrom = args.source

    log.info("SystemService_post_parseSystemModel")
    return null
  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  // Deployment
  /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Called before executing a deployment plan to allow you to either tweak the plan or prevent
   * entirely its execution (you can throw a <code>java.security.AccessControlException</code>).
   * Note that as with any "pre" hook, you can change and/or replace any of the arguments and the
   * caller will use the new ones.
   *
   * @param args.model the <code>SystemModel</code> object used to generate the plan (note that the
   *                   model may be filtered, and you can call <code>unfilter()</code> on it
   *                   to get the full model)
   * @param args.plan the <code>Plan</code> object representing the plan that will be executed
   * @param args.description the description to use for this plan
   * @return <code>null</code> (the result is unused since executing a plan is asynchronous)
   * @throws java.security.AccessControlException to prevent deployment
   */
  def DeploymentService_pre_executeDeploymentPlan = { args ->

    // typical use case would be to check that the user can actually execute the plan, for example:

    /*
       // using the authorizationService (see StreamFileContentPlugin for how to use it) get the
       // user currently executing the call
       def principal = authorizationService.executingPrincipal

       // assuming that the model was populated with an ldapGroup for each entry as metadata
       def ldapGroups =
         args.model.findEntries().collect { SystemEntry entry -> entry.metadata.ldapGroup }.unique()

       // check against acme proprietary ldap server that the 'principal' belongs to all the
       // 'ldapGroups'

       if(!okToExecutePlan)
        throw new AccessControlException("user ${principal} is not authorized to execute this plan")
    */

    log.info("DeploymentService_pre_executeDeploymentPlan")
    return null
  }

  /**
   * Called when the plan has started executing. The difference between this hook and the "pre" hook
   * is that the plan has actually started, hence a deployment id is available. At this stage of
   * the process you cannot modify the arguments because the deployment is already in process.
   *
   * @param args.model the <code>SystemModel</code> object used to generate the plan (note that the
   *                   model may be filtered, and you can call <code>unfilter()</code> on it
   *                   to get the full model)
   * @param args.plan the <code>Plan</code> object representing the plan that will be executed
   * @param args.description the description to use for this plan
   * @param args.deploymentId the id of the deployment (the key to be able to query the progress
   *        of the deployment)
   * @return <code>null</code> (the result is unused since executing a plan is asynchronous)
   */
  def DeploymentService_onStart_executeDeploymentPlan = { args ->

    // typical use case is to send some sort of notification. Since you have the deploymentId
    // the notification message can contain a link back to check for progress, for example:

    /*
       def emailSubject = "Plan ${args.plan.name} in progress..."

       // config is the config object you get during PluginService_initialize hook and that
       // is assumed you saved as a field...

       def emailBody = """
       Check progress at ${config.grails.serverURL}/plan/deployments/${args.deploymentId}
       """
       // send email
     */

    log.info("DeploymentService_onStart_executeDeploymentPlan")
    return null
  }

  /**
   * Called when the deployment ends to allow you to do any kind of post deployment action (typical
   * use case would be some sort of notification (email, sms, etc...)
   *
   * @param args.model the <code>SystemModel</code> object used to generate the plan (note that the
   *                   model may be filtered, and you can call <code>unfilter()</code> on it
   *                   to get the full model)
   * @param args.plan the <code>Plan</code> object representing the plan
   * @param args.description the description to use for this plan
   * @param args.deploymentId the id of the deployment (same as <code>onStart</code> hook)
   * @param args.serviceResult the <code>IPlanExecution</code> object with all the details about the
   *        execution
   * @return <code>null</code> (the result is unused since executing a plan is asynchronous)
   */
  def DeploymentService_post_executeDeploymentPlan = { args ->

    // typical use case is to send some sort of notification

    /*
       def completionStatus = args.serviceResult.completionStatus
       def emailSubject = "Plan ${args.plan.name} completed with status [${completionStatus.status}]"
       def emailBody = """
       ${completionStatus.status} on ${new Date(completionStatus.endTime)} and lasted ${completionStatus.duration}
       """
       // send email
     */

    log.info("DeploymentService_post_executeDeploymentPlan")
    return null
  }
}