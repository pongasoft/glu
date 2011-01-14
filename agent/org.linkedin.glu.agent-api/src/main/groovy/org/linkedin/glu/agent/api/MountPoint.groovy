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


package org.linkedin.glu.agent.api

/**
 * Represents a mount point
 *
 * @author ypujante@linkedin.com
 */
public class MountPoint implements Serializable
{
  private static final long serialVersionUID = 1L;

  public static final ROOT = new MountPoint()

  private final MountPoint parent
  private final String name

  /**
   * Constructor
   */
  def MountPoint(String name, MountPoint parent)
  {
    if(!name || name.contains('/'))
      throw new IllegalArgumentException("${name} is not valid")
    if(parent == null)
      throw new IllegalArgumentException("parent cannot be null")

    this.name = name;
    this.parent = parent;
  }

  /**
   * Constructor
   */
  def MountPoint(name)
  {
    this(name, ROOT)
  }

  /**
   * Constructor use only to create the root
   */
  private MountPoint()
  {
    name = ''
    parent = null
  }

  String getName()
  {
    return name
  }

  MountPoint getParent()
  {
    return parent
  }

  /**
   * Reverse of {@link #getPath()}
   */
  def static fromPath(String path)
  {
    if(path == null)
      return null

    path = path.trim()

    if(path == '/')
      return ROOT

    if(!path.startsWith('/'))
      return new MountPoint(path)

    def mountPoint

    path.split(/\//).each {
      if(it)
      {
        if(mountPoint)
          mountPoint = new MountPoint(it, mountPoint)
        else
          mountPoint = new MountPoint(it)
      }
    }

    return mountPoint
  }

  def static create(MountPoint mountPoint)
  {
    return mountPoint
  }

  def static create(String mountPoint)
  {
    return fromPath(mountPoint)
  }

  def static create(Object mountPoint)
  {
    return fromPath(mountPoint?.toString())
  }

  /**
   * Returns as a path (starting with '/')
   */
  def String getPath()
  {
    if(!parent)
      return '/'

    def path = parent.path
    if(path == '/')
      return "/${name}"
    else
      return "${path}/${name}"
  }

  /**
   * Returns as a collection of path elements (first one is always the empty string)
   */
  def getPathElements()
  {
    def pathElements = []

    def mp = this

    while(mp.parent != null)
    {
      pathElements << mp.name
      mp = mp.parent
    }

    pathElements << ''

    return pathElements.reverse()
  }

  public String toString()
  {
    return path
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!o || getClass() != o.class) return false;

    MountPoint that = (MountPoint) o;

    if(!name.equals(that.name)) return false;
    if(parent ? !parent.equals(that.parent) : that.parent != null) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (parent ? parent.hashCode() : 0);
    result = 31 * result + name.hashCode();
    return result;
  }
}
