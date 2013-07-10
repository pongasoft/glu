package org.pongasoft.glu.provisioner.core.metamodel;

import org.linkedin.glu.utils.core.Externable;

import java.net.URI;
import java.util.Collection;

/**
 * Represents a plugin for the console.
 *
 * @author yan@pongasoft.com
 */
public interface ConsolePluginMetaModel extends Externable
{
  /**
   * @return the fully qualified class name of the plugin */
  String getFqcn();

  /**
   * @return the classpath (collection of jars where the class defined in {@link #getFqcn()} is
   *         located as well as dependencies). Only built-in plugins do not require a class path */
  Collection<URI> getClassPath();
}
