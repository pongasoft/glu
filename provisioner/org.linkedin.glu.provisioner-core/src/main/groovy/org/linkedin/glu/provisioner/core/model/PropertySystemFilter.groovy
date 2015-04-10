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

  static interface PropertyToken
  {
    public static final Object NO_MATCH = new Object()

    /**
     * @return {@link #NO_MATCH} when there is no match */
    Object matches(Object value)

    void append(StringBuilder sb)
  }

  /**
   * Handles simple property
   */
  static class StringPropertyToken implements PropertyToken
  {
    String token

    @Override
    Object matches(Object value)
    {
      if(value == null)
        return NO_MATCH

      try
      {
        // handles int ranges
        if(value instanceof Collection)
          value[token].findAll { it }
        else
          value[token]
      }
      catch(MissingPropertyException e)
      {
        if(log.isDebugEnabled())
        {
          log.debug("missing property (ignored)", e)
        }
        return NO_MATCH
      }
    }

    @Override
    String toString()
    {
      return token
    }

    @Override
    void append(StringBuilder sb)
    {
      if(sb.size() != 0)
        sb.append('.')
      sb.append(toString())
    }
  }

  /**
   * Handles [xxx] notation... Note that it works for single integers as well as ranges ([0..-1]).
   */
  static class IndexedPropertyToken implements PropertyToken
  {
    def index

    @Override
    Object matches(Object value)
    {
      if(value == null)
        return NO_MATCH

      if(value.metaClass.respondsTo(value, 'getAt', [index.getClass()]))
        value.getAt(index)
      else
        return NO_MATCH
    }

    @Override
    String toString()
    {
      return "[${index}]"
    }

    @Override
    void append(StringBuilder sb)
    {
      sb.append(toString())
    }
  }

  public static PropertyToken createFromToken(String token)
  {
    new StringPropertyToken(token: token)
  }

  public static PropertyToken createFromIndex(def index)
  {
    new IndexedPropertyToken(index: index)
  }

  private Collection<PropertyToken> _tokens

  void setName(String n)
  {
    super.setName(n)
    _tokens = name.tokenize('.').collect { createFromToken(it) }
  }

  void setTokens(Collection<PropertyToken> tokens)
  {
    _tokens = tokens?.collect { it } ?: []
    super.setName(computeName())
  }

  private String computeName()
  {
    StringBuilder sb = new StringBuilder()

    for(PropertyToken token : _tokens)
    {
      token.append(sb)
    }

    return sb.toString()
  }

  PropertySystemFilter appendToken(String token)
  {
    new PropertySystemFilter(tokens: [*_tokens, createFromToken(token)])
  }

  PropertySystemFilter appendIndex(def index)
  {
    new PropertySystemFilter(tokens: [*_tokens, createFromIndex(index)])
  }

  String getKind()
  {
    return 'p';
  }

  def boolean filter(SystemEntry entry)
  {
    if(entry == null)
      return false

    Object computedValue = computeValue(entry)

    if(computedValue instanceof Collection)
      computedValue.find { it == value}
    else
      computedValue == value
  }

  /**
   * @return {@link PropertyToken#NO_MATCH} when there is no match */
  private Object computeValue(def v)
  {
    if(v == null)
      return PropertyToken.NO_MATCH

    for(PropertyToken token : _tokens)
    {
      v = token.matches(v)
      if(v == PropertyToken.NO_MATCH)
        return v
    }

    return v
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
