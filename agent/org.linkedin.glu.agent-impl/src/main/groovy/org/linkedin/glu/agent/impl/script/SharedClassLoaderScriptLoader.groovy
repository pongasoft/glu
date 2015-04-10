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
package org.linkedin.glu.agent.impl.script

import org.linkedin.util.lang.MemorySize
import org.linkedin.util.reflect.ReflectUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.security.MessageDigest

/**
 * @author yan@pongasoft.com  */
public class SharedClassLoaderScriptLoader implements ScriptLoader<SharedClassLoaderLoadedScript>
{
  public static final String MODULE = SharedClassLoaderScriptLoader.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final MemorySize FILE_BUFFER_SIZE = MemorySize.parse('100k')

  private static class SharedClassLoaderLoadedScript extends LoadedScript
  {
    Object key
  }

  /**
   * We also store the key so that it can be retrieved and reused as the key to
   * the hash map itself.
   */
  private static class ClassLoaderWithKey
  {
    GroovyClassLoader groovyClassLoader = new GroovyClassLoader(getClass().classLoader)
    Class<?> unqualifiedScriptClass
    Set<SharedClassLoaderLoadedScript> scripts = new LinkedHashSet<>()
    Object key

    SharedClassLoaderLoadedScript addScript(SharedClassLoaderLoadedScript script)
    {
      script.key = key
      scripts << script
      return script
    }

    void clear()
    {
      groovyClassLoader.clearCache()
      groovyClassLoader = null
      unqualifiedScriptClass = null
      scripts = null
      key = null
    }
  }

  /**
   * key is the unique key (sha-1)
   */
  private final def Map<Object, ClassLoaderWithKey> _classLoaders = [:]

  /**
   * @return the internal structure... for testing only....
   */
  Map<Object, ClassLoaderWithKey> getClassLoaders()
  {
    return _classLoaders
  }

  @Override
  synchronized SharedClassLoaderLoadedScript loadScript(File scriptFile)
  {
    ClassLoaderWithKey clk = getClassLoader(scriptFile)

    if(!clk.unqualifiedScriptClass)
      clk.unqualifiedScriptClass = clk.groovyClassLoader.parseClass(scriptFile)

    Object scriptInstance = clk.unqualifiedScriptClass.newInstance()

    clk.addScript(new SharedClassLoaderLoadedScript(script: scriptInstance))
  }

  @Override
  synchronized SharedClassLoaderLoadedScript loadScript(String className,
                                                        Collection<File> classPath)
  {
    ClassLoaderWithKey clk = getClassLoader(classPath)

    Class<?> scriptClass =
      ReflectUtils.forName(className, clk.groovyClassLoader)

    Object scriptInstance = scriptClass.newInstance()

    clk.addScript(new SharedClassLoaderLoadedScript(script: scriptInstance))
  }

  @Override
  synchronized void unloadScript(SharedClassLoaderLoadedScript loadedScript)
  {
    ClassLoaderWithKey clk = _classLoaders[loadedScript.key]

    if(clk)
    {
      clk.scripts.remove(loadedScript)

      if(clk.scripts.size() == 0)
      {
        clk.clear()
        _classLoaders.remove(loadedScript.key)
        if(log.isDebugEnabled())
          log.debug("[${loadedScript.key}] discarding class loader")
      }
      else
      {
        if(log.isDebugEnabled())
          log.debug("[${loadedScript.key}] unloaded script from class loader [${clk.scripts.size()}]")
      }
    }
    loadedScript?.script = null
  }

  private Object computeKey(Collection<File> files)
  {
    def md = MessageDigest.getInstance('SHA1')

    files?.each { file -> addToDigest(file, md) }

    return new BigInteger(1, md.digest())
  }

  private MessageDigest addToDigest(File file, MessageDigest md)
  {
    file?.withInputStream { InputStream stream ->
      stream.eachByte(FILE_BUFFER_SIZE.sizeInBytes as int) { byte[] buf, int bytesRead ->
        md.update(buf, 0, bytesRead);
      }
    }

    return md
  }

  private ClassLoaderWithKey getClassLoader(File scriptFile)
  {
    doGetClassLoader(scriptFile, [])
  }

  private ClassLoaderWithKey getClassLoader(Collection<File> classPath)
  {
    doGetClassLoader(null, classPath)
  }

  private ClassLoaderWithKey doGetClassLoader(File scriptFile, Collection<File> classPath)
  {
    def key = computeKey([scriptFile, *classPath])

    def clk = _classLoaders[key]

    if(!clk)
    {
      clk = new ClassLoaderWithKey(key: key)
      classPath?.each { clk.groovyClassLoader.addURL(it.toURI().toURL()) }
      _classLoaders[key] = clk

      if(log.isDebugEnabled())
        log.debug("[${key}] created class loader for [${scriptFile}]/[${classPath}] [0]")
    }
    else
    {
      if(log.isDebugEnabled())
        log.debug("[${key}] reusing class loader for [${scriptFile}]/[${classPath}] [${clk.scripts.size()}]")
    }


    return clk
  }
}