package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.pongasoft.glu.provisioner.core.metamodel.ConsolePluginMetaModel

/**
 * @author yan@pongasoft.com  */
public class ConsolePluginMetaModelImpl implements ConsolePluginMetaModel
{
  String fqcn
  Collection<URI> classPath

  @Override
  def toExternalRepresentation()
  {
    [
      fqcn: getFqcn(),
      classPath: getClassPath()?.collect { it.toString() } ?: []
    ]
  }
}