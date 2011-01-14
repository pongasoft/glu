/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.agent.tools;

import org.linkedin.util.codec.Base64Codec;
import org.linkedin.util.codec.CodecUtils;
import org.linkedin.util.codec.OneWayCodec;
import org.linkedin.util.codec.OneWayMessageDigestCodec;
import org.linkedin.util.io.IOUtils;

import java.io.IOException;
import java.io.Console;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

/**
 * @author ypujante@linkedin.com
 */
public class Password
{
  public static void main(String[] args) throws IOException
  {
    Console console = System.console();

    if(args.length == 0)
    {
      char[] password = console.readPassword("[%s]", "Password to encrypt:");
      char[] encryptingKey = console.readPassword("[%s]", "Encrypting key:");

      Base64Codec codec = new Base64Codec(new String(encryptingKey));

      System.out.println(CodecUtils.encodeString(codec, new String(password)));
    }
    else
    {
      char[] sha1Password = console.readPassword("[%s]", "SHA1 password:");
      char[] encryptingKey = console.readPassword("[%s]", "Encrypting key:");
      OneWayCodec oneWayCodec =
        OneWayMessageDigestCodec.createSHA1Instance(new String(sha1Password),
                                                    new Base64Codec(new String(encryptingKey)));

      System.out.println(oneWayCodec.encode(readFile(args[0])));
    }
  }

  private static byte[] readFile(String filename) throws IOException
  {
    File file = new File(filename);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    FileInputStream in = new FileInputStream(file);
    try
    {
      IOUtils.copy(in, baos);
    }
    finally
    {
      in.close();
    }

    return baos.toByteArray();
  }
}
