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
package org.linkedin.glu.groovy.utils

import org.codehaus.groovy.runtime.HandleMetaClass
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The purpose of this class is to workaround an issue with groovy when running code compiled
 * under jdk1.6 and running it under jdk1.7 (see forum thread: http://groovy.329449.n5.nabble.com/jdk7-and-IncompatibleClassChangeError-for-exception-class-td5714582.html)
 * @author yan@pongasoft.com  */
public class ExceptionJdk17Workaround
{
  public static final String MODULE = ExceptionJdk17Workaround.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  static void installWorkaround()
  {
    if(getJavaVersionAsDouble() >= 1.7)
    {
      def metaClass = Exception.metaClass

      if(metaClass instanceof HandleMetaClass)
        metaClass = metaClass.replaceDelegate()

      def field = MetaClassImpl.class.getDeclaredField('constructors')
      field.setAccessible(true)
      def constructors = field.get(metaClass)

      int jdk7ConstructorIdx = -1

      constructors.array.eachWithIndex { elt, idx ->
        if(elt.cachedConstructor.parameterTypes.size() == 4)
          jdk7ConstructorIdx = idx
      }

      if(jdk7ConstructorIdx != -1)
      {
        constructors.remove(jdk7ConstructorIdx)
        log.info "Running with jdk1.7: installed groovy Exception workaround"
      }
    }
    else
    {
      log.debug "Running with jdk1.6: non workaround necessary"
    }
  }

  static double getJavaVersionAsDouble()
  {
    def version = System.getProperty("java.version")
    try
    {
      Double.parseDouble(version.split('\\.')[0..1].join('.'))
    }
    catch(NumberFormatException e)
    {
      e.printStackTrace()
      return 1.6;
    }
  }
}