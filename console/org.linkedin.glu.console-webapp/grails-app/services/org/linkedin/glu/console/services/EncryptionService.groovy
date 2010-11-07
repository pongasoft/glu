/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.console.services

import org.linkedin.glu.agent.rest.client.EncryptionKeysProvider
import org.linkedin.groovy.util.encryption.EncryptionUtils

/**
 * Created by IntelliJ IDEA.
 * User: mdubey
 * Date: Jul 22, 2010
 * Time: 9:08:08 AM
 */
class EncryptionService {
  boolean transactional = false


  // will be dependency injected
  EncryptionKeysProvider encryptionKeysProvider

  def getEncryptionKeys()
  {
    return encryptionKeysProvider.getEncryptionKeys()
  }

  def createNewKey(String name)
  {
    encryptionKeysProvider.createNewEncryptionKey(name)
  }

  def encrypt(String plain, String name)
  {
     def keys = getEncryptionKeys()
     return EncryptionUtils.encrypt(plain, keys, name)
  }

  def decrypt(String plain)
  {
     def keys = getEncryptionKeys()
     return EncryptionUtils.decryptBuffer(plain, keys)
  }

}
