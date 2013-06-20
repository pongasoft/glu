package org.pongasoft.glu.packaging.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.util.io.resource.Resource

/**
 * This class contains the context that will be provided in all templates under the
 * <code>packagerContext</code> name
 *
 * @author yan@pongasoft.com  */
public class PackagerContext
{
  Shell shell
  Resource keysRootDir
}