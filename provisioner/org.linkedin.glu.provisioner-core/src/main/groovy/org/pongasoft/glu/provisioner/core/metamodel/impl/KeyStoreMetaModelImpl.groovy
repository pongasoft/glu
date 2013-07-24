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

package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.pongasoft.glu.provisioner.core.metamodel.KeyStoreMetaModel

/**
 * @author yan@pongasoft.com  */
public class KeyStoreMetaModelImpl implements KeyStoreMetaModel
{
  URI uri
  String checksum
  String storePassword
  String keyPassword

  @Override
  Object toExternalRepresentation()
  {
    [
      uri: getUri()?.toString(),
      checksum: getChecksum(),
      storePassword: getStorePassword(),
      keyPassword: getKeyPassword()
    ]
  }

  boolean equals(o)
  {
    if(this.is(o)) return true
    if(!(o instanceof KeyStoreMetaModelImpl)) return false

    KeyStoreMetaModelImpl that = (KeyStoreMetaModelImpl) o

    if(checksum != that.checksum) return false
    if(keyPassword != that.keyPassword) return false
    if(storePassword != that.storePassword) return false
    if(uri != that.uri) return false

    return true
  }

  int hashCode()
  {
    int result
    result = (uri != null ? uri.hashCode() : 0)
    result = 31 * result + (checksum != null ? checksum.hashCode() : 0)
    result = 31 * result + (storePassword != null ? storePassword.hashCode() : 0)
    result = 31 * result + (keyPassword != null ? keyPassword.hashCode() : 0)
    return result
  }
}