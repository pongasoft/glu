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

import org.pongasoft.glu.provisioner.core.metamodel.PhysicalHostMetaModel

/**
 * @author yan@pongasoft.com  */
public class PhysicalHostMetaModelImpl extends HostMetaModelImpl implements PhysicalHostMetaModel
{
  String hostAddress

  @Override
  String resolveHostAddress()
  {
    return hostAddress
  }

  @Override
  Object toExternalRepresentation()
  {
    return hostAddress
  }
}