/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.grails.utils

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import org.json.JSONObject
import javax.servlet.http.HttpServletResponse
import org.linkedin.util.codec.Codec
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.CodecUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.clock.Timespan
import org.linkedin.util.io.PathUtils
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.codec.HexaCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.util.codec.OneWayCodec

/**
 * @author ypujante@linkedin.com */
class ConsoleHelper
{
  public static final String MODULE = ConsoleHelper.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  public static final OneWayCodec SHA1 =
    OneWayMessageDigestCodec.createSHA1Instance('', HexaCodec.INSTANCE)

  // we use a codec because somehow the decoding of the cookies when json format is not working :(
  public final static Codec COOKIE_CODEC = new Base64Codec('console')

  /**
   * Locates a value in the request. Precendence order is params first, then cookie
   */
  static def getRequestValue(def params, HttpServletRequest request, String name)
  {
    def value = params[name]

    if(!value)
      value = getCookieValue(request, name)

    return value
  }

  /**
   * Locates a value in the request. Precendence order is params first, then flash, then cookie
   */
  static def getRequestValue(def params, def flash, HttpServletRequest request, String name)
  {
    def value = params[name]

    if(!value)
    {
      value = flash[name]

      if(!value)
      {
        value = getCookieValue(request, name)
      }
    }

    return value
  }

  /**
   * gets the value of a cookie or <code>null</code> if no such cookie
   */
  static def getCookieValue(HttpServletRequest request, String cookieName)
  {
    def cookieValue = request.cookies.find {it.name == cookieName}?.value

    if(cookieValue)
    {
      try
      {
        cookieValue = CodecUtils.decodeString(COOKIE_CODEC, cookieValue)
      }
      catch(Codec.CannotDecodeException e)
      {
        cookieValue = null

        if(log.isDebugEnabled())
          log.debug("invalid cookie value (ingored) ${cookieValue}", e)
      }
    }

    return cookieValue
  }

  /**
   * Returns the cookie value stored as json format
   */
  static def getCookieJSONValue(HttpServletRequest request, String cookieName)
  {
    def value = getCookieValue(request, cookieName)
    if(value)
      return JsonUtils.toValue(new JSONObject(value))
    else
      return null
  }

  /**
   * Saves the cookie for 1y.
   */
  static def saveCookie(HttpServletResponse response, String cookieName, cookieValue)
  {
    saveCookie(response, cookieName, cookieValue, null)
  }

  /**
   * Saves the cookie for the provided duration (<code>null</code> = 1y, and can be any Timespan
   *  format value (String ok)).
   * @param cookieValue <code>null</code> means delete the cookie
   */
  static def saveCookie(HttpServletResponse response, String cookieName, cookieValue, duration)
  {
    if(cookieValue != null)
    {
      cookieValue = JsonUtils.toJSON(cookieValue).toString()
      cookieValue = CodecUtils.encodeString(COOKIE_CODEC, cookieValue)
    }

    def cookie = new Cookie(cookieName, cookieValue)

    if(cookieValue == null)
    {
      cookie.maxAge = 0
    }
    else
    {
      duration = Timespan.parse((duration ?: '1y').toString())
      cookie.maxAge = (int) duration.durationInSeconds
    }
    cookie.path = '/console'
    response.addCookie(cookie)
  }

  static String computeVersion(String release, String uris)
  {
    def out = []

    if(release)
    {
      out << release
    }

    if(uris)
    {
      out << computeUrisVersionString(uris)
    }

    return out.join(' | ')
  }

  static def computeVersionFromURI(URI uri)
  {
    def version
    if(uri.scheme == 'ivy')
    {
      def ivyCoordinates = PathUtils.removeLeadingSlash(uri.path).split('/')
      version = "${ivyCoordinates[1]} - ${ivyCoordinates[2]}"
    }
    else
    {
      version = uri.path.split('/')[-1]
    }
    return version
  }

  static def computeUrisVersion(String uris)
  {
    def versions = []

    if(uris)
    {
      def uriArray = uris.split(',')
      uriArray.each { u ->
        def s = u.split('\\|')
        def contextPath
        def configRef
        if(s.size() >= 2)
        {
          u = s[0]
          contextPath = s[1]
        }
        if (s.size() == 3)
          configRef = s[2]
        
        def uri = new URI(u as String)
        def version = computeVersionFromURI(uri)

        if(contextPath)
          version = "${version}|${contextPath}"

        if (configRef) {
          def configUri = new URI(configRef as String)
          def configVersion = computeVersionFromURI(configUri)
          version = "${version}|${configVersion}"
        }

        versions << version.toString()
      }
    }

    return versions
  }

  static String computeUrisVersionString(String uris)
  {
    String versionString

    def versions = computeUrisVersion(uris)

    if(versions)
    {
      versionString = versions.join(', ')

      if(versions.size() > 1)
      {
        versionString = "[${versionString}]".toString()
      }
    }

    return versionString
  }

  /**
   * returns a file object... handles <code>File</code>, URI, URL, string, <code>null</code>.
   * The difference with GroovyIOUtils.toFile(s) is that it does *not* download the file if not
   * local!
   * TODO MED YP: promote to linkedin-utils
   */
  static File toFileObject(s)
  {
    if(s == null)
      return null

    if(s instanceof File)
      return s

    if(s instanceof Resource)
    {
      return s.getFile()
    }

    if(s instanceof String)
    {
      URI uri = null
      try
      {
        uri = new URI(s)
      }
      catch(URISyntaxException e)
      {
        // ok will handle below
      }

      if(!uri?.scheme)
      {
        return new File(s)
      }
    }

    URI uri = GroovyNetUtils.toURI(s)

    if(uri.scheme == 'file')
      return new File(uri.path)
    else
      return null
  }

  /**
   * Computes the checksum of a string
   *
   * @param s <code>null</code> is ok
   * @return (sha1 of the string as a hexadecimal string)
   */
  static String computeChecksum(String s)
  {
    if(s == null)
      return null

    CodecUtils.encodeString(SHA1, s)
  }

  /**
   * Make sure that the argument is a collection by wrapping it in a collection if necessary
   */
  static Collection toCollection(def o)
  {
    if(o == null)
      return null

    if(o instanceof Collection)
      return o

    return [o]
  }
}
