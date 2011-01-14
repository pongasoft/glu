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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.console.services.SystemService
import org.linkedin.glu.console.services.EncryptionService
import org.linkedin.util.codec.Base64Codec

/**
 * Created by IntelliJ IDEA.
 * User: mdubey
 * Date: Jul 22, 2010
 * Time: 9:05:44 AM
 * To change this template use File | Settings | File Templates.
 */
class EncryptionController extends ControllerBase
{
  Base64Codec base64Codec = new Base64Codec()

  // dependency injected
  EncryptionService encryptionService
  SystemService systemService

  def list = {
    // list of available keys
    def keys = [:]
    def encryptionKeys = encryptionService.getEncryptionKeys()
    encryptionKeys.each { k, v ->
      keys.put(k, base64Codec.encode(v))
    }

    return [encryptionKeys: keys, count : keys.size()]
  }

  def create = {
    // Just displays create.gsp page, no data needs to be passed here.
  }

  def encrypt = {
    // This returns keys names for rendering of encrypt/decrypt page
    def keyNames = []
    encryptionService.getEncryptionKeys().each { k, v ->
      keyNames.add(k)
    }

    return [keyNames: keyNames]
  }

  
  // Following closures handle form data and return a rendered fragment for use in Ajax Action.
  def ajaxSave = {
    def name = params.keyName ? params.keyName.trim() : ''

    if (name.length() == 0) {
      render("<span class='error-status'>Please enter a name for the key. Ex: key20100715</span>")
    } else {
      def keys = encryptionService.getEncryptionKeys()
      if (keys && keys[name]) {
        render("<span class='error-status'>Key with name " + name + " already exists in the keystore. Please select another name.</span>")
      } else {
        encryptionService.createNewKey(name)
        audit('encryptionkey.created', name)
        render("New random encryption Key " + name + " created.")
      }
    }
  }

  def ajaxEncrypt = {
    def plain = params.plainText ? params.plainText.trim() : ''

    if (plain.length() == 0) {
      render("<span class='error-status'>Nothing to encrypt.</span>")
    } else {
      def name = params.keyName
      def encryptedText = encryptionService.encrypt(plain, name)
      render("'" + plain + "' encrypted using '" + name + "' = <b>" + encryptedText + "</b>")
    }
  }

  def ajaxDecrypt = {
     def encrypted = params.encrypted ? params.encrypted.trim() : ''

    if (encrypted.length() == 0) {
      render("<span class='error-status'>Nothing to decrypt.</span>")
    } else {
      def plainText = encryptionService.decrypt(encrypted)
      render("'" + encrypted + "' decrypts to = <b>" + plainText + "</b>")
    }
  }
}
