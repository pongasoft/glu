package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.pongasoft.glu.provisioner.core.metamodel.InstallMetaModel

/**
 * @author yan@pongasoft.com  */
public class InstallMetaModelImpl implements InstallMetaModel
{
  String path

  @Override
  Object toExternalRepresentation()
  {
    [
      path: getPath()
    ]
  }
}