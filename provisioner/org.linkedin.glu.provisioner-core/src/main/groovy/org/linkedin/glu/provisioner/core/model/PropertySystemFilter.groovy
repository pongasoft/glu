/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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

package org.linkedin.glu.provisioner.core.model

/**
 * @author ypujante@linkedin.com */
class PropertySystemFilter extends NameEqualsValueSystemFilter
{
  public static final String MODULE = PropertySystemFilter.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private def tokens

  void setName(String n)
  {
    super.setName(n)
    tokens = name.tokenize('.')
  }

  String getKind()
  {
    return 'p';
  }

  def boolean filter(SystemEntry entry)
  {
    if(entry == null)
      return false

    try
    {
      def v = entry
      tokens.each {
        if(v != null)
          v = v[it]
      }
      return v == value
    }
    catch(MissingPropertyException e)
    {
      if(log.isDebugEnabled())
      {
        log.debug("missing property (ignored)", e)
      }
      return false
    }
  }

  @Override
  String toDSL()
  {
    return "${name}='${value}'".toString();
  }

  def String toString()
  {
    return toDSL()
  }

  boolean equals(o)
  {
    // tokens is a 'cached' value so it will be the same if the names are the same!
    if(!(o instanceof PropertySystemFilter)) return false;
    return super.equals(o)
  }
}
