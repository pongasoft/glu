package org.pongasoft.glu.packaging.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.StateMachineMetaModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com  */
public abstract class BasePackager
{
  public static final String MODULE = BasePackager.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final String CONFIG_TOKENS_KEY = 'configTokens'
  public static final String PACKAGER_CONTEXT_KEY = 'packagerContext'

  PackagerContext packagerContext

  Resource outputFolder
  Resource inputPackage
  Collection<Resource> configTemplatesRoots

  boolean dryMode = false

  protected def _sha1s = [:].withDefault { Resource r -> shell.sha1(r) }

  /**
   * Make sure the package name ends with the proper version
   *
   * @return the package name (without the version)
   */
  String ensureVersion(String version)
  {
    String fullPackageName = inputPackage.filename

    if(!fullPackageName.endsWith("-${version}"))
      throw new IllegalArgumentException("input package [${inputPackage.filename}] mismatch version [${version}]")

    fullPackageName - "-${version}"
  }

  abstract PackagedArtifacts createPackages()

  Shell getShell()
  {
    packagerContext.shell
  }

  Resource copyInputPackage(Resource destination)
  {
    Resource parent = shell.mkdirs(destination.parentResource)

    // make sure do not exist...
    shell.delete(destination)
    shell.delete(parent.createRelative(inputPackage.filename))

    Resource location = shell.cp(inputPackage, parent)

    if(location != destination)
      shell.mv(location, destination)

    resolveLinks(destination)

    return destination
  }

  void processConfigs(String fromFolder, Map tokens, Resource toFolder)
  {
    configTemplatesRoots.each { configTemplatesRoot ->
      processConfigs(configTemplatesRoot.createRelative(fromFolder), tokens, toFolder)
    }
  }

  void resolveLinks(Resource destination)
  {
    def out = []
    GroovyIOUtils.eachChildRecurse(inputPackage) { Resource file ->
      if(file.filename.endsWith('.jar.lnk'))
        resolveLink(destination, file, out)
    }

    if(out)
    {
      // optimization => cp forks and makes the process slow => forking once...
      def content = """#!/bin/bash
${out.join('\n')}
"""

      shell.withTempFile { Resource f ->
        shell.saveContent(f, content)
        shell.exec(f)
      }
    }
  }

  void resolveLink(Resource destination, Resource link, def out)
  {
    Resource linkFolder = link.parentResource

    def (linkSourceRelativePath, sha1) = link.file.readLines()

    def linkSource = linkFolder.createRelative(linkSourceRelativePath)

    if(sha1 && sha1 != _sha1s[linkSource])
      throw new IllegalArgumentException("mismatch sha1 for ${linkSource} (expecting ${sha1}, got ${_sha1s[linkSource]})")

    String destinationPath =
      inputPackage.file.toPath().relativize(linkFolder.file.toPath()).toString()

    //shell.cp(linkSource, destination.createRelative(destinationPath))
    out << "cp -R ${linkSource.file.canonicalPath} ${destination.createRelative(destinationPath).file.canonicalPath}"

    //shell.rm(destination.createRelative(destinationPath).createRelative(link.filename))
    out << "rm ${destination.createRelative(destinationPath).createRelative(link.filename).file.canonicalPath}"
  }

  void processConfigs(Resource fromFolder, Map tokens, Resource toFolder)
  {
    processConfigs(shell, fromFolder, tokens, toFolder ?: outputFolder)
  }

  /**
   * Recursively process all the configs in a given folder
   */
  static Collection<Resource> processConfigs(Shell shell,
                                             Resource fromFolder,
                                             Map tokens,
                                             Resource toFolder)
  {
    Collection<Resource> processedTemplates = []

    if(fromFolder?.exists())
    {
      GroovyIOUtils.eachChildRecurse(fromFolder.chroot('.')) { Resource templateOrFile ->
        if(!templateOrFile.isDirectory())
        {
          Resource to = toFolder.createRelative(templateOrFile.path)

          if(to.path.contains('@'))
            to = shell.toResource(shell.replaceTokens(to.path, tokens))

          log.debug("processing config templateOrFile: ${templateOrFile} => ${to}")
          processedTemplates << shell.processTemplate(templateOrFile, to, tokens)
        }
      }
    }

    return processedTemplates
  }

  protected Resource generateStateMachineJarFile(StateMachineMetaModel stateMachineMetaModel,
                                                 Resource toFolder)
  {
    shell.withTempFile { Resource r ->

      def lines = []

      stateMachineMetaModel.defaultTransitions.transitions.each { state, transitions ->
        transitions = transitions
          .collect { transition -> "[to: '${transition.to}', action: '${transition.action}']"}
          .join(', ')
        lines << "  '${state}': [${transitions}]"
      }

      def content = """
defaultTransitions =
[
${lines.join(',\n')}
]

defaultEntryState = '${stateMachineMetaModel.defaultEntryState}'
"""

      shell.saveContent(r.createRelative('glu/DefaultStateMachine.groovy'), content)

      shell.mkdirs(toFolder)

      Resource jarFile = toFolder.createRelative('glu-state-machine.jar')

      shell.ant { ant ->
        ant.jar(destfile: jarFile.file,
                basedir: r.file)
      }

      return jarFile
    }
  }
}