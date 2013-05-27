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

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.Codec
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.util.io.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The purpose of this class is to generate keys and keystore for glu
 *
 * @author yan@pongasoft.com  */
public class KeysGenerator
{
  public static final String MODULE = KeysGenerator.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  Codec base64Codec = new Base64Codec()

  Shell shell
  Resource outputFolder
  String masterPassword
  String sha1Password = 'gluos1way1'
  String encryptingKey = 'gluos2way'

  def opts =
    [
      keyalg: 'RSA',
      keysize: 2048,
      validity: 2000,
      dname: [
        cn: 'glu-cn',
        ou: 'glu-ou',
        o: 'glu-o',
        c: 'glu-c'
      ],
    ]

  def keys = [:]
  def passwords = [:]

  def passwordGenerator = { String name ->
    OneWayCodec codec  = OneWayMessageDigestCodec.createSHA1Instance(masterPassword, base64Codec)
    return CodecUtils.encodeString(codec, name)
  }

  def getPasswords()
  {
    if(!passwords)
    {
      passwords = [
        'agentKeystore',
        'agentKey',
        'agentTruststore',
        'consoleKeystore',
        'consoleKey',
        'consoleTruststore'
      ].collectEntries { p -> [p, passwordGenerator("${p}.password")] }
    }

    return passwords
  }
  /**
   * Generate all the keys, keystores and truststores
   */
  def generateKeys()
  {
    if(!keys)
    {
      keys.agentKeystore = generateAgentKeystore()
      keys.agentTruststore = generateAgentTruststore(keys.agentKeystore)
      keys.consoleKeystore = generateConsoleKeystore()
      keys.consoleTruststore = generateConsoleTruststore(keys.consoleKeystore)
    }

    return keys
  }

  def getEncryptedPasswords()
  {
    Base64Codec codec = new Base64Codec(encryptingKey);

    passwords.collectEntries { k, v ->
      [k, CodecUtils.encodeString(codec, v.toString())]
    }
  }

  String computeChecksum(Resource resource)
  {
    OneWayCodec oneWayCodec =
      OneWayMessageDigestCodec.createSHA1Instance(new String(sha1Password),
                                                  new Base64Codec(new String(encryptingKey)));

    return oneWayCodec.encode(readFile(resource))
  }

  private static byte[] readFile(Resource resource) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    resource.inputStream.withStream { stream ->
      baos << stream
    }

    return baos.toByteArray();
  }

  /**
   * Step 1: generate agent keystore
   *
   * @return the resource to the keystore
   */
  Resource generateAgentKeystore()
  {
    createResourceInOutputFolder('agent.keystore') { Resource agentKeystore ->
      def cmd = [
        'keytool',
        '-noprompt',
        '-genkey',
        '-alias', 'agent',
        '-keystore', agentKeystore.file.canonicalPath,
        '-storepass', getPasswords().agentKeystore,
        '-keypass', getPasswords().agentKey,
        '-keyalg', opts.keyalg,
        '-keysize', opts.keysize,
        '-validity', opts.validity,
        '-dname', opts.dname.collect {k, v -> "${k}=${v}"}.join(', ')
      ]

      def res = shell.exec(command: cmd)

      log.info "Created agent.keystore"
      if(res)
        log.info res.toString()

      return agentKeystore
    }
  }

  /**
   * Step 2: extract the certificate from the agent keystore to create the agent truststore
   *
   * @return the resource to the truststore
   */
  Resource generateAgentTruststore(Resource agentKeystore)
  {
    createResourceInOutputFolder('agent.truststore') { Resource agentTruststore ->

      createResourceInOutputFolder('agent.cert.temp', false) { Resource tempFile ->
        try
        {
          def cmd = [
            'keytool',
            '-noprompt',
            '-export',
            '-keystore', agentKeystore.file.canonicalPath,
            '-storepass', getPasswords().agentKeystore,
            '-alias', 'agent',
            '-file', tempFile.file.canonicalPath
          ]

          def res = shell.exec(command: cmd)

          log.debug "Created agent.cert.temp"
          if(res)
            log.debug res

          cmd = [
            'keytool',
            '-noprompt',
            '-import',
            '-alias', 'agent',
            '-keystore', agentTruststore.file.canonicalPath,
            '-storepass', getPasswords().agentTruststore,
            '-file', tempFile.file.canonicalPath
          ]

          res = shell.exec(command: cmd)

          log.info "Created agent.truststore"
          if(res)
            log.info res

        }
        finally
        {
          shell.rm(tempFile)
        }

        return tempFile
      }

      return agentTruststore
    }
  }

  /**
   * Step 3: generate console keystore
   *
   * @return the resource to the keystore
   */
  Resource generateConsoleKeystore()
  {
    createResourceInOutputFolder('console.keystore') { Resource consoleKeystore ->
      def cmd = [
        'keytool',
        '-noprompt',
        '-genkey',
        '-alias', 'console',
        '-keystore', consoleKeystore.file.canonicalPath,
        '-storepass', getPasswords().consoleKeystore,
        '-keypass', getPasswords().consoleKey,
        '-keyalg', opts.keyalg,
        '-keysize', opts.keysize,
        '-validity', opts.validity,
        '-dname', opts.dname.collect {k, v -> "${k}=${v}"}.join(', ')
      ]

      def res = shell.exec(command: cmd)

      log.info "Created console.keystore"
      if(res)
        log.info res

      return consoleKeystore
    }

  }

  /**
   * Step 4: extract the certificate from the console keystore to create the console truststore
   *
   * @return the resource to the truststore
   */
  Resource generateConsoleTruststore(Resource consoleKeystore)
  {
    createResourceInOutputFolder('console.truststore') { Resource consoleTruststore ->

      createResourceInOutputFolder('console.cert.temp', false) { Resource tempFile ->
        try
        {
          def cmd = [
            'keytool',
            '-noprompt',
            '-export',
            '-keystore', consoleKeystore.file.canonicalPath,
            '-storepass', getPasswords().consoleKeystore,
            '-alias', 'console',
            '-file', tempFile.file.canonicalPath
          ]

          def res = shell.exec(command: cmd)

          log.debug "Created console.cert.temp"
          if(res)
            log.debug res

          cmd = [
            'keytool',
            '-noprompt',
            '-import',
            '-alias', 'console',
            '-keystore', consoleTruststore.file.canonicalPath,
            '-storepass', getPasswords().consoleTruststore,
            '-file', tempFile.file.canonicalPath
          ]

          res = shell.exec(command: cmd)

          log.info "Created console.truststore"
          if(res)
            log.info res

        }
        finally
        {
          shell.rm(tempFile)
        }

        return tempFile
      }

      return consoleTruststore
    }
  }

  private Resource createResourceInOutputFolder(String name,
                                            boolean callClosureOnlyIfFileDoesNotExist = true,
                                            Closure<Resource> closure)
  {
    if(!outputFolder.exists())
      shell.mkdirs(outputFolder)
    Resource resource = outputFolder.createRelative(name)
    if(!callClosureOnlyIfFileDoesNotExist || !resource.exists())
      resource = closure(resource)
    return resource
  }

}