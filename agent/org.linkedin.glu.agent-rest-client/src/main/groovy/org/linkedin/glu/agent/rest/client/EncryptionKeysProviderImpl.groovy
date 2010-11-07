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

import org.linkedin.groovy.util.encryption.EncryptionUtils

/**
 * @author mdubey@linkedin.com
 */
class EncryptionKeysProviderImpl implements EncryptionKeysProvider
{
  public static final String MODULE = EncryptionKeysProviderImpl.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private Map _encryptionKeys
  private final String _secretkeystorePath
  private final String _keystorePassword
  private final String _keyPassword
  private final File _secretKeyStore

  EncryptionKeysProviderImpl(String secretkeystorePath, String keystorePassword, String keyPassword)
  {
    _secretkeystorePath = secretkeystorePath
    _keystorePassword = keystorePassword
    _keyPassword = keyPassword

    // _secretkeystorePath is defined as /dev/null by default (for backward compatibility)
    if (_secretkeystorePath && _secretkeystorePath != '/dev/null') {
      _secretKeyStore = new File(_secretkeystorePath)
      _encryptionKeys = EncryptionUtils.getSecretKeys(_secretKeyStore, _keystorePassword, _keyPassword)
    } else {
      log.warn "secretkeystorePath config parameter not specified. EncryptionKeys not loaded."
    }
  }

  static EncryptionKeysProvider create(def config)
  {
    Map params = getSecretKeyStoreParams(config)
    return new EncryptionKeysProviderImpl(params['secretkeystorePath'], params['keystorePassword'], params['keyPassword'])
  }

  def Map getEncryptionKeys()
  {
    return _encryptionKeys;
  }

  def createNewEncryptionKey(String name)
  {
    if (_secretKeyStore) {
      EncryptionUtils.createSecretKey(_secretKeyStore, name, _keystorePassword, _keyPassword)
      // update cached list of encryption keys
      _encryptionKeys = EncryptionUtils.getSecretKeys(_secretKeyStore, _keystorePassword, _keyPassword)
    } else {
      log.error "secretkeystorePath config parameter not specified. Cannot create new encryption key " + name
    }
  }

  private static Map getSecretKeyStoreParams(def config)
  {
    return HttpsClientHelper.getKeyStoreParams(config, 'secretkeystorePath')
  }

  private static Map getSecretKeys(def config)
  {
    Map params = getSecretKeyStoreParams(config)
    return
  }

}
