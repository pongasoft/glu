/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.console.services

import org.linkedin.util.lang.LangUtils

/**
 * @author yan@pongasoft.com */
class TagsService
{
  static transactional = false

  private Map<String, Map> _tagsDefinition = [:]

  private String _tagsCss = ''

  void init(Map<String, Map> tagsDefinition)
  {
    _tagsDefinition = LangUtils.deepClone(tagsDefinition)

    Map defaultTagDefinition =
      _tagsDefinition.remove('__default__') ?: [background: '#005a87', color: '#ffffff']

    StringBuilder css = new StringBuilder()
    css << "ul.tags.with-links a:link, ul.tags.with-links a:visited {background: ${defaultTagDefinition.background};color: ${defaultTagDefinition.color};} ul.tags.with-links a:hover {color: ${defaultTagDefinition.background};background: ${defaultTagDefinition.color};}"

    _tagsDefinition.eachWithIndex { String tag, Map definition, int idx ->
      String tagCssClass = "tag-${idx}".toString()
      definition.cssClass = tagCssClass
      css << " ul.tags.with-links li.${tagCssClass} a:link, ul.tags.with-links li.${tagCssClass} a:visited {background: ${definition.background}; color: ${definition.color};} ul.tags.with-links li.${tagCssClass} a:hover { color: ${definition.background}; background: ${definition.color};}"
    }

    _tagsCss = css
  }

  String getTagsCss()
  {
    return _tagsCss
  }

  String getTagCssClass(String tag)
  {
    _tagsDefinition[tag]?.cssClass
  }
}
