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
    }

  }

  def String toString()
  {
    return "${name}='${value}'".toString();
  }
}
