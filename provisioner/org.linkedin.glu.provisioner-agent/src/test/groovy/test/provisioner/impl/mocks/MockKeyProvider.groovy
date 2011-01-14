/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package test.provisioner.impl.mocks

import org.linkedin.glu.agent.rest.client.EncryptionKeysProvider

/**
 * Created by IntelliJ IDEA.
 * User: mdubey
 * Date: Jul 20, 2010
 * Time: 1:16:11 PM
 */
def class MockKeyProvider implements EncryptionKeysProvider {

  def Map getEncryptionKeys()
  {
    return [s1: 'secretKey1', s2 : 'secretKey2'];
  }

  def createNewEncryptionKey(String name)
  {

  }

}
