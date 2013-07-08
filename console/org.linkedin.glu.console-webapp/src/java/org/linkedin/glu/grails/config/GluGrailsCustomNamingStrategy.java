/*
 * Copyright (c) 2013 Yan Pujante
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

package org.linkedin.glu.grails.config;

import grails.util.Holders;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yan@pongasoft.com
 */
public class GluGrailsCustomNamingStrategy
{
  public static final String MODULE = GluGrailsCustomNamingStrategy.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static String getTableName(String originalTableName)
  {
    String tableName = originalTableName;

    GrailsApplication grailsApplication = Holders.getGrailsApplication();
    Object optionalTableMapping =
      grailsApplication.getFlatConfig().get("console.datasource.table." + originalTableName + ".mapping");

    if(optionalTableMapping != null)
    {
      tableName = optionalTableMapping.toString();
      log.info("remapping [" + originalTableName + "] table to [" + tableName + "]");
    }

    return tableName;
  }
}
