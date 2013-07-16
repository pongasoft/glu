package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.pongasoft.glu.provisioner.core.metamodel.CliMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.HostMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.InstallMetaModel

/**
 * @author yan@pongasoft.com  */
public class CliMetaModelImpl implements CliMetaModel
{
  String version
  Map<String, Object> configTokens
  HostMetaModel host
  InstallMetaModel install
  GluMetaModel gluMetaModel

  String getVersion()
  {
    version ?: gluMetaModel.gluVersion
  }

  @Override
  Object toExternalRepresentation()
  {
    [
      version: getVersion(),
      host: getHost()?.toExternalRepresentation(),
      install: getInstall()?.toExternalRepresentation(),
      configTokens: getConfigTokens() ?: [:]
    ]
  }

}