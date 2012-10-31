/*
* Copyright (c) 2012 Yan Pujante
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

/**
 * The code is inspired from this thread:
 * http://stackoverflow.com/questions/652916/converting-a-java-keystore-into-pem-format
 */

import java.security.KeyStore
import java.security.cert.Certificate
import java.security.Key

char[] keyStorePassword = System.console().readPassword("Enter keystore password:")
KeyStore ks = KeyStore.getInstance("jks");
/* Load the key store. */
new FileInputStream("console.keystore").withStream {
  ks.load(it, keyStorePassword)
}
char[] keyPassword = System.console().readPassword("Enter key password:")
/* Save the private key. */
ByteArrayOutputStream derKey = new ByteArrayOutputStream()
derKey.withStream { kos ->
  Key pvt = ks.getKey("console", keyPassword);
  kos.write(pvt.getEncoded());
}

/* Save the certificate. */
ByteArrayOutputStream derCert = new ByteArrayOutputStream()
derCert.withStream { cos ->
  Certificate pub = ks.getCertificate("console");
  cos.write(pub.getEncoded());
}

ByteArrayOutputStream consolePem = new ByteArrayOutputStream()

Process p = "openssl pkcs8 -inform der -nocrypt".execute()

Thread.start { p.outputStream << derKey.toByteArray() }
Thread.start { System.err << p.errorStream }
Thread.start { consolePem << p.inputStream }


if(p.waitFor() != 0)
  throw new Exception("Error while running process 1")

p = "openssl x509 -inform der".execute()

Thread.start { p.outputStream << derCert.toByteArray() }
Thread.start { System.err << p.errorStream }
Thread.start { consolePem << p.inputStream }

if(p.waitFor() != 0)
  throw new Exception("Error while running process 2")

new FileOutputStream("console.pem").withStream { fos ->
  fos << consolePem.toByteArray()
}
