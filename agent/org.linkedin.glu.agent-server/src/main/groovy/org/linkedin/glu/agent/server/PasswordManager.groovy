/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.agent.server

import org.linkedin.groovy.util.cli.CliUtils
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.util.codec.CodecUtils

/**
 * Password manager
 * @author ypujante@linkedin.com
 */
class PasswordManager
{
  public static void main(String[] args)
  {
    def cli = new CliBuilder(usage: './bin/password-manager.sh [-h] [-e] [-c]')
    cli.h(longOpt: 'help', 'display help')
    cli.e(longOpt: 'encryptPassword', 'encrypt the password', args: 0, required: false)
    cli.c(longOpt: 'computeChecksum', 'compute checksum', args: 1, required: false)

    def options = CliUtils.parseCliOptions(cli: cli, args: args)

    if(!options || options.options.h)
    {
      cli.usage()
      return
    }

    def console = System.console()
    if(!console)
      throw new IllegalStateException("not in console mode...")

    if(options.e || options.c)
    {
      char[] p0
      p0 = console.readPassword("%s:", "Enter p0 (default to gluos2way)")
      if(!p0)
        p0 = 'gluos2way'.toCharArray()

      char[] p1
      p1 = console.readPassword("%s:", "Enter p1 (default to gluos1way1)")
      if(!p1)
        p1 = 'gluos1way1'.toCharArray()

      def twoWayCodec = new Base64Codec(new String(p0))
      def oneWayCodec =
        OneWayMessageDigestCodec.createSHA1Instance(new String(p1), new Base64Codec(new String(p1)))

      if(options.e)
      {
        char[] passwordToEncrypt = console.readPassword("%s:",
                                                        "Enter the password you want to encrypt")

        println CodecUtils.encodeString(oneWayCodec, new String(passwordToEncrypt))
        return
      }
      else
      {
        
      }
    }
    else
    {
      println "Use -e or -c"
      cli.usage()
      return
    }


  }
}
