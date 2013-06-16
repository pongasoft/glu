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
import org.pongasoft.glu.provisioner.core.metamodel.KeysMetaModel

/**
 * @author yan@pongasoft.com  */
public class KeysMetaModelImpl implements KeysMetaModel
{
  KeyStoreMetaModel agentKeyStore
  KeyStoreMetaModel agentTrustStore
  KeyStoreMetaModel consoleKeyStore
  KeyStoreMetaModel consoleTrustStore

  @Override
  Object toExternalRepresentation()
  {
    [
      agentKeyStore: agentKeyStore.toExternalRepresentation(),
      agentTrustStore: agentTrustStore.toExternalRepresentation(),
      consoleKeyStore: consoleKeyStore.toExternalRepresentation(),
      consoleTrustStore: consoleTrustStore.toExternalRepresentation()
    ]
  }
}