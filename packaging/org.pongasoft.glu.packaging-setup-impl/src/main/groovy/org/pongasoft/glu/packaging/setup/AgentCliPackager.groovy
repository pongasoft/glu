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

import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.AgentCliMetaModel

/**
 * @author yan@pongasoft.com  */
public class AgentCliPackager extends BasePackager
{
  AgentCliMetaModel metaModel

  @Override
  PackagedArtifacts createPackages()
  {
    new PackagedArtifacts(createPackage())
  }

  PackagedArtifact createPackage()
  {
    String packageName = ensureVersion(metaModel.version)

    def tokens = [
      agentCliMetaModel: metaModel,
    ]
    tokens[PACKAGER_CONTEXT_KEY] = packagerContext

    def parts = [packageName]
    parts << metaModel.version

    Resource packagePath = outputFolder.createRelative(parts.join('-'))

    if(!dryMode)
    {
      copyInputPackage(packagePath)
      configure(packagePath, tokens)
      if(metaModel.gluMetaModel.stateMachine)
        generateStateMachineJarFile(metaModel.gluMetaModel.stateMachine,
                                    packagePath.createRelative('lib'))
    }

    return new PackagedArtifact(location: packagePath,
                                metaModel: metaModel)
  }

  Resource configure(Resource packagePath, Map tokens)
  {
    processConfigs('agent-cli', tokens, packagePath)
    return packagePath
  }
}