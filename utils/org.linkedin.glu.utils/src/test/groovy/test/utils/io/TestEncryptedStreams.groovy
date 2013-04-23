/*
 * Copyright (c) 2012-2013 Yan Pujante
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

package test.utils.io

import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils

/**
 * @author yan@pongasoft.com */
public class TestEncryptedStreams extends GroovyTestCase
{
  public void testEncryptAndDecryptWithSamePassword()
  {
    String text = "this is my test string"

    ByteArrayOutputStream baos = new ByteArrayOutputStream()

    // in jdk1.7 Blowfish seems to be limited to 128 bits...
    String password = "0o9ijht65tgfrdtg"

    GluGroovyIOUtils.withStreamToEncrypt(password, baos) { OutputStream os ->
      os << text
    }

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())

    baos = new ByteArrayOutputStream()

    GluGroovyIOUtils.withStreamToDecrypt(password, bais) { InputStream is ->
      baos << is
    }

    assertEquals(text, new String(baos.toByteArray(), "UTF-8"))

    baos = new ByteArrayOutputStream()

    GluGroovyIOUtils.withStreamToDecrypt("different", bais) { InputStream is ->
      baos << is
    }

    assertTrue(text != new String(baos.toByteArray(), "UTF-8"))

  }
}