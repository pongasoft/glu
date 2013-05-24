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

import java.nio.file.Files
import java.nio.file.Path

/**
 * The purpose of this class is to generate keys and keystore for glu
 *
 * @author yan@pongasoft.com  */
public class KeysGenerator
{
  Path outputFolder
  char[] masterPassword

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

  Path generateAgentKeystore()
  {
    if(!Files.exists(outputFolder))
      Files.createDirectories(outputFolder)

    Path agentKeystore = outputFolder.resolve('agent.keystore').toAbsolutePath()

    def agentKeystorePassword = "agentKeystorePassword"
    def agentKeyPassword = "agentKeyPassword"

    def cmd = [
      'keytool',
      '-noprompt',
      '-genkey',
      '-alias', 'agent',
      '-keystore', agentKeystore.toString(),
      '-storepass', agentKeystorePassword,
      '-keypass', agentKeyPassword,
      '-keyalg', opts.keyalg,
      '-keysize', opts.keysize,
      '-validity', opts.validity,
      '-dname', opts.dname.collect {k, v -> "${k}=${v}"}.join(', ')
    ]

    def process = cmd.execute()
    Thread.start {
      System.err << process.errorStream
    }
    println process.text

    return agentKeystore
  }
}