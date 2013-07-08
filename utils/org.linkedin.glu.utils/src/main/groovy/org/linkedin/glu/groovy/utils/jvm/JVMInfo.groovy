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

package org.linkedin.glu.groovy.utils.jvm

/**
 * @author yan@pongasoft.com  */
public class JVMInfo
{
  /**
   * @return the jvm information (as a map)
   */
  static Map<String, String> getJVMInfo()
  {
    getJVMInfo(System.properties)
  }

  /**
   * @param properties extract the info from the map
   * @return the jvm information (as a map)
   */
  static Map<String, String> getJVMInfo(def properties)
  {
    def jvmInfo = [:]

    properties.each { k, v ->
      switch(k)
      {
        case 'java.version':
        case { k.startsWith('java.vm.') }:
        case { k.startsWith('java.runtime.') }:
        case { k.startsWith('java.specification.') }:
          jvmInfo[k] = v
          break

        default:
          // do nothing
          break
      }
    }

    return jvmInfo
  }

  /**
   * @return the jvm information as a string (same as running java -version)
   */
  static String getJVMInfoString()
  {
    getJVMInfoString(System.properties)
  }

  /**
   * @return the jvm information as a string (same as running java -version)
   */
  static String getJVMInfoString(def properties)
  {
    getJVMInfoAsStringCollection(properties).join('\n')
  }

  /**
   * @return the jvm information as a string collection
   */
  static Collection<String> getJVMInfoAsStringCollection()
  {
    getJVMInfoAsStringCollection(System.properties)
  }

  /**
   * @return the jvm information as a string collection
   */
  static Collection<String> getJVMInfoAsStringCollection(def properties)
  {
    [
      "java version \"${properties['java.version']}\"".toString(),
      "${properties['java.runtime.name']} (build ${properties['java.runtime.version']})".toString(),
      "${properties['java.vm.name']} (build ${properties['java.vm.version']}, ${properties['java.vm.info']})".toString()
    ]
  }

}