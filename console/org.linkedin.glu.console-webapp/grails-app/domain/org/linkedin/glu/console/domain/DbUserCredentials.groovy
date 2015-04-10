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

package org.linkedin.glu.console.domain

import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.mindrot.jbcrypt.BCrypt

import java.security.SecureRandom

class DbUserCredentials
{
  public static final SecureRandom SECURE_RANDOM = new SecureRandom()
  public static final int NB_ROUNDS = 12

  static OneWayMessageDigestCodec PASSWORD_CODEC =
    OneWayMessageDigestCodec.createSHA1Instance('glu', new Base64Codec())

  String username
  String oneWayHashPassword // password processed with one way hash
  String x509Pem // Base64 encoded DER certificate
  String salt // a salt for the one way hash function

  boolean validatePassword(String password)
  {
    if(!password)
      return false

    if(salt)
      BCrypt.checkpw(salt + password, oneWayHashPassword)
    else
      // for backward compatibility with non seeded passwords
      computeOneWayHash(password) == oneWayHashPassword
  }

  void setPassword(String password)
  {
    salt = BCrypt.gensalt(NB_ROUNDS, SECURE_RANDOM)
    oneWayHashPassword = BCrypt.hashpw(salt + password, BCrypt.gensalt(NB_ROUNDS, SECURE_RANDOM))
  }
  
  public static String computeOneWayHash(String password)
  {
    CodecUtils.encodeString(PASSWORD_CODEC, password)
  }

  static constraints = {
    username(nullable: false, blank: false, unique: true)
    oneWayHashPassword(nullable: true, blank: false)
    x509Pem(nullable: true, blank: false)
    salt(nullable: true, blank: false)
  }

  static transients = ['password', 'PASSWORD_CODEC', 'SECURE_RANDOM']

}
