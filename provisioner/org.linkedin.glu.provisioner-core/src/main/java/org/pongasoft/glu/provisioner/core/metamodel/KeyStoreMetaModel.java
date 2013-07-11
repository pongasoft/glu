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

package org.pongasoft.glu.provisioner.core.metamodel;

import org.linkedin.glu.utils.core.Externable;

import java.net.URI;

/**
 * Represents a single key store. Note that in glu a keystore only holds one key.
 *
 * @author yan@pongasoft.com
 */
public interface KeyStoreMetaModel extends Externable, MetaModel
{
  /**
   * @return location of the key store (can be relative, use --keys-root to 'resolve' the location)
   */
  URI getUri();

  /**
   * @return the checksum of the key store (obfuscated sha-1)
   */
  String getChecksum();

  /**
   * @return the password for the key store itself
   */
  String getStorePassword();

  /**
   * @return the password to the (unique) key stored in this key store
   */
  String getKeyPassword();
}