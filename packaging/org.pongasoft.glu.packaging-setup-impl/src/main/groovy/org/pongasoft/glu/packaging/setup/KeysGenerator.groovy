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

import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.Codec
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path

/**
 * The purpose of this class is to generate keys and keystore for glu
 *
 * @author yan@pongasoft.com  */
public class KeysGenerator
{
  public static final String MODULE = KeysGenerator.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  Codec base64Codec = new Base64Codec()

  Path outputFolder
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

  String computeChecksum(Path path)
  {
    OneWayCodec oneWayCodec =
      OneWayMessageDigestCodec.createSHA1Instance(new String(sha1Password),
                                                  new Base64Codec(new String(encryptingKey)));

    return oneWayCodec.encode(readFile(path))
  }

  private static byte[] readFile(Path path) throws IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    Files.newInputStream(path).withStream { stream ->
      baos << stream
    }

    return baos.toByteArray();
  }

  /**
   * Step 1: generate agent keystore
   *
   * @return the path to the keystore
   */
  Path generateAgentKeystore()
  {
    Path agentKeystore = createPathInOutputFolder('agent.keystore')

    def cmd = [
      'keytool',
      '-noprompt',
      '-genkey',
      '-alias', 'agent',
      '-keystore', agentKeystore.toString(),
      '-storepass', getPasswords().agentKeystore,
      '-keypass', getPasswords().agentKey,
      '-keyalg', opts.keyalg,
      '-keysize', opts.keysize,
      '-validity', opts.validity,
      '-dname', opts.dname.collect {k, v -> "${k}=${v}"}.join(', ')
    ]

    def res = executeCommand(cmd)

    log.info "Created agent.keystore"
    if(res)
      log.info res

    return agentKeystore
  }

  /**
   * Step 2: extract the certificate from the agent keystore to create the agent truststore
   *
   * @return the path to the truststore
   */
  Path generateAgentTruststore(Path agentKeystore)
  {
    Path agentTruststore = createPathInOutputFolder('agent.truststore')

    Path tempFile = createPathInOutputFolder('agent.cert.temp')
    try
    {
      def cmd = [
        'keytool',
        '-noprompt',
        '-export',
        '-keystore', agentKeystore.toString(),
        '-storepass', getPasswords().agentKeystore,
        '-alias', 'agent',
        '-file', tempFile.toString()
      ]

      def res = executeCommand(cmd)

      log.debug "Created agent.cert.temp"
      if(res)
        log.debug res

      cmd = [
        'keytool',
        '-noprompt',
        '-import',
        '-alias', 'agent',
        '-keystore', agentTruststore.toString(),
        '-storepass', getPasswords().agentTruststore,
        '-file', tempFile.toString()
      ]

      res = executeCommand(cmd)

      log.info "Created agent.truststore"
      if(res)
        log.info res

    }
    finally
    {
      Files.delete(tempFile)
    }

    return agentTruststore
  }

  /**
   * Step 3: generate console keystore
   *
   * @return the path to the keystore
   */
  Path generateConsoleKeystore()
  {
    Path consoleKeystore = createPathInOutputFolder('console.keystore')

    def cmd = [
      'keytool',
      '-noprompt',
      '-genkey',
      '-alias', 'console',
      '-keystore', consoleKeystore.toString(),
      '-storepass', getPasswords().consoleKeystore,
      '-keypass', getPasswords().consoleKey,
      '-keyalg', opts.keyalg,
      '-keysize', opts.keysize,
      '-validity', opts.validity,
      '-dname', opts.dname.collect {k, v -> "${k}=${v}"}.join(', ')
    ]

    def res = executeCommand(cmd)

    log.info "Created console.keystore"
    if(res)
      log.info res

    return consoleKeystore
  }

  /**
   * Step 4: extract the certificate from the console keystore to create the console truststore
   *
   * @return the path to the truststore
   */
  Path generateConsoleTruststore(Path consoleKeystore)
  {
    Path consoleTruststore = createPathInOutputFolder('console.truststore')

    Path tempFile = createPathInOutputFolder('console.cert.temp')
    try
    {
      def cmd = [
        'keytool',
        '-noprompt',
        '-export',
        '-keystore', consoleKeystore.toString(),
        '-storepass', getPasswords().consoleKeystore,
        '-alias', 'console',
        '-file', tempFile.toString()
      ]

      def res = executeCommand(cmd)

      log.debug "Created console.cert.temp"
      if(res)
        log.debug res

      cmd = [
        'keytool',
        '-noprompt',
        '-import',
        '-alias', 'console',
        '-keystore', consoleTruststore.toString(),
        '-storepass', getPasswords().consoleTruststore,
        '-file', tempFile.toString()
      ]

      res = executeCommand(cmd)

      log.info "Created console.truststore"
      if(res)
        log.info res

    }
    finally
    {
      Files.delete(tempFile)
    }

    return consoleTruststore
  }

  private String executeCommand(def cmd)
  {
    Process process = cmd.execute()
    Thread.start {
      System.err << process.errorStream
    }
    Thread.start {
      process.outputStream.close()
    }
    def res = process.text

    if(process.waitFor() != 0)
      throw new Exception("error while executing command")

    return res
  }

  private Path createPathInOutputFolder(String name)
  {
    if(!Files.exists(outputFolder))
      Files.createDirectories(outputFolder)
    outputFolder.resolve(name).toAbsolutePath()
  }

}