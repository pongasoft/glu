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
import org.pongasoft.glu.packaging.setup.KeysGenerator
import org.pongasoft.glu.provisioner.core.metamodel.KeysMetaModel

/**
 * @author yan@pongasoft.com  */
public class TestKeysGenerator extends GroovyTestCase
{
  public void testKeysGenerator()
  {
    ShellImpl.createTempShell { Shell shell ->

      def generator = new KeysGenerator(shell: shell,
                                        outputFolder: shell.toResource('/keys'),
                                        masterPassword: "abcdefgh",
                                        generateRelativeKeyStoreUri: true)

      KeysMetaModel keys = generator.generateKeys()

      assertEquals(new URI('agent.keystore'), keys.agentKeyStore.uri)
      assertTrue(shell.toResource("/keys/agent.keystore").exists())
      assertEquals('xR1LJ-60iik2MlmaurcY3--26ykW6_9AtrEG', keys.agentKeyStore.storePassword)
      assertEquals('W_Wat--Y5p9t6_e7Ji0T6oSTunffoeUj98rV', keys.agentKeyStore.keyPassword)

      assertEquals(new URI('agent.truststore'), keys.agentTrustStore.uri)
      assertTrue(shell.toResource("/keys/agent.truststore").exists())
      assertEquals('R_W-DrLAD_fntTE-A_9Z9p-59na_65Nfiet8', keys.agentTrustStore.storePassword)
      assertNull(keys.agentTrustStore.keyPassword)

      assertEquals(new URI('console.keystore'), keys.consoleKeyStore.uri)
      assertTrue(shell.toResource("/keys/console.keystore").exists())
      assertEquals('R5MdZn7bZk70orkl9_EcbR6vZy9YMrE23i7-', keys.consoleKeyStore.storePassword)
      assertEquals('W_1Y69ElZo9y3_ktW8eCDrEai9HY6ljaZ_kd', keys.consoleKeyStore.keyPassword)

      assertEquals(new URI('console.truststore'), keys.consoleTrustStore.uri)
      assertTrue(shell.toResource("/keys/console.truststore").exists())
      assertEquals('3lwjxeUbZgWgb5MBiobaJR-vW0k7WR-j95Np', keys.consoleTrustStore.storePassword)
      assertNull(keys.consoleTrustStore.keyPassword)

      def expectedPasswords = [
        agentKeyStore: 'bF4PErQ1pkYJxJ8ypvkAQ4W2-km',
        agentKey: '2aVH_Oql860q6M1aMrMu_xY6yDF',
        agentTrustStore: 'fSS1xqdnTWyWZEBef7FLzPKxeDZ',
        consoleKeyStore: 'eHIkFtx5h0gaiFVgALtXhXhhcwY',
        consoleKey: 'cAj9rr8_QyFxJmFMAJC5igVq8KF',
        consoleTrustStore: '7xUHiEaAGpi196Qowas4kX_oHrJ'
      ]

      assertEquals(expectedPasswords, generator.passwords)

      def expectedEncryptedPasswords = [
        agentKeyStore: 'xR1LJ-60iik2MlmaurcY3--26ykW6_9AtrEG',
        agentKey: 'W_Wat--Y5p9t6_e7Ji0T6oSTunffoeUj98rV',
        agentTrustStore: 'R_W-DrLAD_fntTE-A_9Z9p-59na_65Nfiet8',
        consoleKeyStore: 'R5MdZn7bZk70orkl9_EcbR6vZy9YMrE23i7-',
        consoleKey: 'W_1Y69ElZo9y3_ktW8eCDrEai9HY6ljaZ_kd',
        consoleTrustStore: '3lwjxeUbZgWgb5MBiobaJR-vW0k7WR-j95Np'
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