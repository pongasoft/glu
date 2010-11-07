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

package org.linkedin.glu.agent.rest.client

/**
 * @author mdubey@linkedin.com
 */
interface EncryptionKeysProvider {

  /**
   * Get a map of all encryption keys that will be used to decrypt enctypted data.
   *
   * This usually come from keystore on console side.
   * 
   * @return Map of encryptionKeys
   */

  def Map getEncryptionKeys()

  /**
   * Creates a new encryption key with the given name, and saves it in secret keystore.
   * 
   * @param name
   * @return
   */
  def createNewEncryptionKey(String name)
}