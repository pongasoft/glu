package org.pongasoft.glu.provisioner.core.metamodel;

import org.linkedin.glu.utils.core.Externable;

/**
 * Represents a cli (a command line interface executable) (base interface)
 *
 * @author yan@pongasoft.com
 */
public interface CliMetaModel extends Externable, Configurable, MetaModel
{
  /**
   * @return the version of this cli... if undefined, defaults to
   * {@link GluMetaModel#getGluVersion()}
   */
  String getVersion();

  /**
   * @return on which host this cli will live
   */
  HostMetaModel getHost();

  /**
   * @return where/how to install this cli
   */
  InstallMetaModel getInstall();

  /**
   * @return reference to the glu meta model this cli belongs to
   */
  GluMetaModel getGluMetaModel();
}