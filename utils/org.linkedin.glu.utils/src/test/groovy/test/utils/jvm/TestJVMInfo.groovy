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

package test.utils.jvm

import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.groovy.utils.jvm.JVMInfo

/**
 * @author yan@pongasoft.com  */
public class TestJVMInfo extends GroovyTestCase
{
  public void testGetJVMInfo()
  {
    def map = [
      'java.version': 'jv',
      'java.vm.p0': 'jvp0',
      'java.runtime.p1': 'jrp1',
      'java.specification.p2': 'jsp2',
      'not.a.vm.prop': 'foo'
    ]

    assertEquals(GluGroovyCollectionUtils.xorMap(map, ['not.a.vm.prop']), JVMInfo.getJVMInfo(map))
  }

  public void testGetJVMInfoAsStringCollection()
  {
    def map = [
      'java.version': 'jv',
      'java.runtime.name': 'jrn',
      'java.runtime.version': 'jrv',
      'java.vm.name': 'jvn',
      'java.vm.version': 'jvv',
      'java.vm.info': 'jvi'
    ]

    assertEquals(["java version \"jv\"", "jrn (build jrv)", "jvn (build jvv, jvi)"],
                 JVMInfo.getJVMInfoAsStringCollection(map))
  }

  public void testGetJVMInfoString()
  {
    def map = [
      'java.version': 'jv',
      'java.runtime.name': 'jrn',
      'java.runtime.version': 'jrv',
      'java.vm.name': 'jvn',
      'java.vm.version': 'jvv',
      'java.vm.info': 'jvi'
    ]

    assertEquals("java version \"jv\"\njrn (build jrv)\njvn (build jvv, jvi)",
                 JVMInfo.getJVMInfoString(map))

    assertEquals("java -version".execute().err.text.trim(), JVMInfo.getJVMInfoString())
  }
}