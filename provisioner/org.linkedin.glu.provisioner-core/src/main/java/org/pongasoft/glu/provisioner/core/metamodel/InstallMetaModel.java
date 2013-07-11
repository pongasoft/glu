package org.pongasoft.glu.provisioner.core.metamodel;

import org.linkedin.glu.utils.core.Externable;

/**
 * Represents how to install clis and servers on their target host. Should be expanded in the
 * future.
 *
 * @author yan@pongasoft.com
 */
public interface InstallMetaModel extends Externable, MetaModel
{
  /**
   * Note that a trailing / indicates, it will be considered a directory in which to install
   * the package if it does not exists. Ex: '/opt/glu/'.
   *
   * @return the install path
   */
  String getPath();
}
