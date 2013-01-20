/*
 * Copyright (c) 2011-2013 Yan Pujante
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

import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.util.annotations.Initializable
import org.linkedin.groovy.util.io.DataMaskingInputStream
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The role of this plugin is to enforce some policies when streaming file content (ex: checking
 * that the user is authorized to do so or masking passwords, etc...)
 *
 * @author yan@pongasoft.com */
public class StreamFileContentPlugin
{
  public static final String MODULE = StreamFileContentPlugin.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable
  String unrestrictedLocation

  @Initializable
  String unrestrictedRole = "ADMIN"

  @Initializable
  boolean maskFileContent = true

  @Initializable
  AuthorizationService authorizationService

  /**
   * Called on initialization
   */
  def PluginService_initialize = { args ->
    unrestrictedLocation = args.config.plugins.StreamFileContentPlugin.unrestrictedLocation
    unrestrictedRole = args.config.plugins.StreamFileContentPlugin.unrestrictedRole ?: "ADMIN"
    maskFileContent =
      GluGroovyLangUtils.getOptionalBoolean(args.config.plugins.StreamFileContentPlugin.maskFileContent,
                                            true)
    authorizationService = args.applicationContext['authorizationService']
    log.info("Setting unrestrictedLocation to ${unrestrictedLocation}")
  }

  /**
   * Called prior to stream the file content. Enforces unrestricted location when defined and user
   * is not and admin
   */
  def AgentsService_pre_streamFileContent = { args ->
    // do not allow non admin users to access files:
    // -- outside of <nonAdminRootLocation> area
    if(unrestrictedLocation && !args.location.startsWith(unrestrictedLocation))
    {
      authorizationService.checkRole(unrestrictedRole, args.location, null)
    }

    return null
  }

  /**
   * Called with the result of stream file content which can be an input stream in which case
   * it gets decorated with a DataMaskingInputStream to hide the passwords and keys
   */
  def AgentsService_post_streamFileContent = { args ->
    if(args.serviceResult instanceof InputStream && maskFileContent)
    {
      return new DataMaskingInputStream(args.serviceResult)
    }
    return null
  }
}