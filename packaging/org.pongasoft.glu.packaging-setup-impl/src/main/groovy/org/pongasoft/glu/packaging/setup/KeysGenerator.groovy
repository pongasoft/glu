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
import org.pongasoft.glu.provisioner.core.metamodel.KeyStoreMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.KeysMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.impl.KeyStoreMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.KeysMetaModelImpl
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
  boolean generateRelativeKeyStoreUri = false

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

  KeysMetaModelImpl keys
  def passwords = [:]
  def encryptedPasswords = [:]

  def passwordGenerator = { String name ->
    OneWayCodec codec  = OneWayMessageDigestCodec.createSHA1Instance(masterPassword, base64Codec)
    return CodecUtils.encodeString(codec, name)
  }

  def getPasswords()
  {
    if(!passwords)
    {
      passwords = [
        'agentKeyStore',
        'agentKey',
        'agentTrustStore',
        'consoleKeyStore',
        'consoleKey',
        'consoleTrustStore'
      ].collectEntries { p -> [p, passwordGenerator("${p}.password")] }
    }

    return passwords
  }
  /**
   * Generate all the keys, keystores and truststores
   */
  KeysMetaModel generateKeys()
  {
    if(!keys)
    {
      keys = new KeysMetaModelImpl()
      keys.agentKeyStore = generateAgentKeyStore()
      keys.agentTrustStore = generateAgentTrustStore(keys.agentKeyStore)
      keys.consoleKeyStore = generateConsoleKeyStore()
      keys.consoleTrustStore = generateConsoleTrustStore(keys.consoleKeyStore)
    }

    return keys
  }

  def getEncryptedPasswords()
  {
    if(!encryptedPasswords)
    {
      Base64Codec codec = new Base64Codec(encryptingKey);

      encryptedPasswords = getPasswords().collectEntries { k, v ->
        [k, CodecUtils.encodeString(codec, v.toString())]
      }
    }

    return encryptedPasswords
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
  KeyStoreMetaModel generateAgentKeyStore()
  {
    def resource = createResourceInOutputFolder('agent.keystore') { Resource agentKeyStore ->
      def cmd = [
        'keytool',
        '-noprompt',
        '-genkey',
        '-alias', 'agent',
        '-keystore', agentKeyStore.file.canonicalPath,
        '-storepass', getPasswords().agentKeyStore,
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

      return agentKeyStore
    }

    return new KeyStoreMetaModelImpl(uri: computeKeyStoreUri(resource),
                                     checksum: computeChecksum(resource),
                                     storePassword: getEncryptedPasswords().agentKeyStore,
                                     keyPassword: getEncryptedPasswords().agentKey)
  }

  /**
   * Step 2: extract the certificate from the agent keystore to create the agent truststore
   *
   * @return the resource to the truststore
   */
  KeyStoreMetaModel generateAgentTrustStore(KeyStoreMetaModel agentKeyStore)
  {
    def resource = createResourceInOutputFolder('agent.truststore') { Resource agentTrustStore ->

      createResourceInOutputFolder('agent.cert.temp', false) { Resource tempFile ->
        try
        {
          def cmd = [
            'keytool',
            '-noprompt',
            '-export',
            '-keystore', toCanonicalPath(agentKeyStore),
            '-storepass', getPasswords().agentKeyStore,
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
            '-keystore', agentTrustStore.file.canonicalPath,
            '-storepass', getPasswords().agentTrustStore,
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

      return agentTrustStore
    }

    return new KeyStoreMetaModelImpl(uri: computeKeyStoreUri(resource),
                                     checksum: computeChecksum(resource),
                                     storePassword: getEncryptedPasswords().agentTrustStore)
  }

  /**
   * Step 3: generate console keystore
   *
   * @return the resource to the keystore
   */
  KeyStoreMetaModel generateConsoleKeyStore()
  {
    def resource = createResourceInOutputFolder('console.keystore') { Resource consoleKeyStore ->
      def cmd = [
        'keytool',
        '-noprompt',
        '-genkey',
        '-alias', 'console',
        '-keystore', consoleKeyStore.file.canonicalPath,
        '-storepass', getPasswords().consoleKeyStore,
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

      return consoleKeyStore
    }

    return new KeyStoreMetaModelImpl(uri: computeKeyStoreUri(resource),
                                     checksum: computeChecksum(resource),
                                     storePassword: getEncryptedPasswords().consoleKeyStore,
                                     keyPassword: getEncryptedPasswords().consoleKey)

  }

  /**
   * Step 4: extract the certificate from the console keystore to create the console truststore
   *
   * @return the resource to the truststore
   */
  KeyStoreMetaModel generateConsoleTrustStore(KeyStoreMetaModel consoleKeyStore)
  {
    def resource = createResourceInOutputFolder('console.truststore') { Resource consoleTruststore ->

      createResourceInOutputFolder('console.cert.temp', false) { Resource tempFile ->
        try
        {
          def cmd = [
            'keytool',
            '-noprompt',
            '-export',
            '-keystore', toCanonicalPath(consoleKeyStore),
            '-storepass', getPasswords().consoleKeyStore,
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
            '-storepass', getPasswords().consoleTrustStore,
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

    return new KeyStoreMetaModelImpl(uri: computeKeyStoreUri(resource),
                                     checksum: computeChecksum(resource),
                                     storePassword: getEncryptedPasswords().consoleTrustStore)
  }

  private String toCanonicalPath(KeyStoreMetaModel keyStore)
  {
    if(keyStore.uri.isAbsolute())
      keyStore.uri.path
    else
      outputFolder.createRelative(keyStore.uri.toString()).file.canonicalPath
  }

  private Resource createResourceInOutputFolder(String name,
                                                boolean failOnFileAlreadyExists = true,
                                                Closure<Resource> closure)
  {
    if(!outputFolder.exists())
      shell.mkdirs(outputFolder)
    Resource resource = outputFolder.createRelative(name)
    if(!failOnFileAlreadyExists || !resource.exists())
    {
      shell.rm(resource)
      resource = closure(resource)
    }
    else
      throw new IllegalStateException("${resource} already exists")
    return resource
  }

  private URI computeKeyStoreUri(Resource resource)
  {
    if(generateRelativeKeyStoreUri)
      new URI(resource.filename)
    else
      resource.toURI()
  }
}