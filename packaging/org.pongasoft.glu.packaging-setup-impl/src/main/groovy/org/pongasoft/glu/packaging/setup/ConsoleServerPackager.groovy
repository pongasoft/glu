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

package org.pongasoft.glu.packaging.setup

import org.linkedin.groovy.util.config.Config
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel

/**
 * @author yan@pongasoft.com  */
public class ConsoleServerPackager extends BasePackager
{
  public static final def ENV_PROPERTY_NAMES = [
    "APP_NAME",
    "APP_VERSION",
    "JAVA_HOME",
    "JAVA_CMD",
    "JAVA_CMD_TYPE",
    "JAVA_OPTIONS",
    "JVM_SIZE",
    "JVM_SIZE_NEW",
    "JVM_SIZE_PERM",
    "JVM_GC_TYPE",
    "JVM_GC_OPTS",
    "JVM_GC_LOG",
    "JVM_APP_INFO",
    "JETTY_CMD",
  ]

  ConsoleMetaModel metaModel

  Map<String, Object> getConfigTokens()
  {
    metaModel.configTokens
  }

  @Override
  PackagedArtifacts createPackages()
  {
    new PackagedArtifacts(createPackage())
  }

  PackagedArtifact createPackage()
  {
    String packageName = ensureVersion(metaModel.version)

    def jettyDistribution =
      shell.ls(inputPackage).find { it.filename.startsWith('jetty-distribution-') }?.filename

    if(!jettyDistribution)
      throw new IllegalArgumentException("${inputPackage} is not a valid console server " +
                                         "distribution as it does not contain a jetty distribution")

    def tokens = [
      consoleMetaModel: metaModel,
      envPropertyNames: ENV_PROPERTY_NAMES,
      'jetty.distribution': jettyDistribution
    ]

    tokens[PACKAGER_CONTEXT_KEY] = packagerContext
    tokens[CONFIG_TOKENS_KEY] = [*:configTokens]

    def parts = [packageName]

    if(metaModel.name != 'default')
      parts << metaModel.name

    parts << metaModel.version

    Resource packagePath = outputFolder.createRelative(parts.join('-'))
    if(!dryMode)
    {
      copyInputPackage(packagePath)
      configure(packagePath, tokens)
      if(metaModel.gluMetaModel.stateMachine)
        generateStateMachineJarFile(metaModel.gluMetaModel.stateMachine,
                                    packagePath.createRelative('glu/repository/plugins'))
      if(Config.getOptionalBoolean(configTokens, 'includeJettyDistribution', true))
      {
        shell.tar(dir: packagePath.createRelative(jettyDistribution),
                  tarFile: packagePath.createRelative("glu/repository/tgzs/${jettyDistribution}.tar.gz"),
                  compression: 'gzip')
      }
    }
    return new PackagedArtifact(location: packagePath,
                                host: metaModel.host.resolveHostAddress(),
                                port: metaModel.mainPort,
                                metaModel: metaModel)
  }

  Resource configure(Resource packagePath, Map tokens)
  {
    processConfigs('console-server', tokens, packagePath)
    return packagePath
  }
}