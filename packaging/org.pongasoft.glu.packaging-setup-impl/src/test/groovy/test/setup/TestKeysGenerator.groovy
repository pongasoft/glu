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

package test.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.packaging.setup.KeysGenerator

/**
 * @author yan@pongasoft.com  */
public class TestKeysGenerator extends GroovyTestCase
{
  public void testKeysGenerator()
  {
    ShellImpl.createTempShell { Shell shell ->

      def generator = new KeysGenerator(shell: shell,
                                        outputFolder: shell.toResource('/keys'),
                                        masterPassword: "abcdefgh")

      def keys = generator.generateKeys()
      assertEquals(4, keys.size())
      assertNull(keys.values().find { Resource resource ->
        !resource.exists()
      })

      def expectedPasswords = [
        agentKeystore: "85ZPMe2MD0f36AIISE4wwnlbgWn",
        agentKey: "2aVH_Oql860q6M1aMrMu_xY6yDF",
        agentTruststore: "c1xkQpjD413jQzV8UV3dYtp0R8V",
        consoleKeystore: "3kbzrLeQNG6zU_0VseJZE2l-uCy",
        consoleKey: "cAj9rr8_QyFxJmFMAJC5igVq8KF",
        consoleTruststore: "5KIsE2sjCRhCRsDoI6sDyiLXMM0"
      ]

      assertEquals(expectedPasswords, generator.passwords)

      def expectedEncryptedPasswords = [
        agentKeystore: "x-Mlb8fFMp60W9tw3irTJTbvWr0VEieARdiY",
        agentKey: "W_Wat--Y5p9t6_e7Ji0T6oSTunffoeUj98rV",
        agentTruststore: "9d73JKm0RRAg9-iY9lcWZdJftrWO6kk2Dykd",
        consoleKeystore: "DiteqRvVW9cCE5tRJkU9DdEKo-k-oKwPb81g",
        consoleKey: "W_1Y69ElZo9y3_ktW8eCDrEai9HY6ljaZ_kd",
        consoleTruststore: "JretRrfcDiWgt_-BWKt3AT73ATcgJ_9g3i1e"
      ]

      assertEquals(expectedEncryptedPasswords, generator.encryptedPasswords)
    }
  }

  public void testComputeChecksum()
  {
    ShellImpl.createTempShell { Shell shell ->

      def generator = new KeysGenerator(shell: shell,
                                        outputFolder: shell.toResource('/keys'),
                                        masterPassword: "abcdefgh")

      // since keystores and truststores change every time, we need to create a file for
      // which we control the content so that the checksum is predictable
      assertEquals('kH_rwI1Cii2_Wk8HBcDju9vKbq3',
                   generator.computeChecksum(shell.saveContent('/foo.txt', 'abcdef')))
    }

  }
}