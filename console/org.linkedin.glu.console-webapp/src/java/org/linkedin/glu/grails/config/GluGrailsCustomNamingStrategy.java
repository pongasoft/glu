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

import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yan@pongasoft.com
 */
public class GluGrailsCustomNamingStrategy extends ImprovedNamingStrategy
  implements GrailsApplicationAware
{
  public static final String MODULE = GluGrailsCustomNamingStrategy.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private GrailsApplication _grailsApplication;

  @Override
  public void setGrailsApplication(GrailsApplication grailsApplication)
  {
    _grailsApplication = grailsApplication;
  }

  @Override
  public String classToTableName(String className)
  {
    String originalTableName = super.classToTableName(className);
    String tableName = originalTableName;

    Object optionalTableMapping =
      _grailsApplication.getFlatConfig().get("console.datasource.table." + originalTableName + ".mapping");

    if(optionalTableMapping != null)
    {
      tableName = optionalTableMapping.toString();
      log.info("remapping [" + originalTableName + "] table to [" + tableName + "]");
    }

    return tableName;
  }
}
