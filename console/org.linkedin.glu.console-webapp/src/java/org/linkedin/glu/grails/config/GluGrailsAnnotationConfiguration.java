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
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author yan@pongasoft.com
 */
public class GluGrailsAnnotationConfiguration extends GrailsAnnotationConfiguration
{
  public static final String MODULE = GluGrailsAnnotationConfiguration.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private static final long serialVersionUID = 1L;

  @Override
  public void setGrailsApplication(GrailsApplication grailsApplication)
  {
    super.setGrailsApplication(grailsApplication);

    GluGrailsCustomNamingStrategy namingStrategy = new GluGrailsCustomNamingStrategy();
    namingStrategy.setGrailsApplication(grailsApplication);

    // YP implementation note: the naming strategy is set in 2 different locations and
    // unfortunately they behave differently (the one from the superclass does NOT handle objects
    // and handle only class name compared to the one in the Hibernate plugin support which follows
    // what the documentation states). In any case I need to configure it with the grails
    // application in order to access the config object...
    // 1. in the super class (Configuration)
    setNamingStrategy(namingStrategy);

    try
    {
      // 2. in the GrailsDomainBinder (see HibernatePluginSupport)
      GrailsDomainBinder.configureNamingStrategy(namingStrategy);
    }
    catch(ClassNotFoundException e)
    {
      throw new RuntimeException(e);
    }
    catch(InstantiationException e)
    {
      throw new RuntimeException(e);
    }
    catch(IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }

  }
}
