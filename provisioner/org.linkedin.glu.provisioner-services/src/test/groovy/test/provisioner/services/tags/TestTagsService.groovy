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

package test.provisioner.services.tags

import org.linkedin.glu.provisioner.services.tags.TagsService

/**
 * @author yan@pongasoft.com */
class TestTagsService extends GroovyTestCase
{
  public void testEmpty()
  {
    TagsService tagsService = new TagsService([:])

    assertEquals('ul.tags.with-links a:link, ul.tags.with-links a:visited {background: #005a87;color: #ffffff;} ul.tags.with-links a:hover {color: #005a87;background: #ffffff;}', tagsService.tagsCss)

    assertNull(tagsService.getTagCssClass('foo'))
  }

  public void testDefinition()
  {
    TagsService tagsService = new TagsService(['a:1': [background: 'red', color: 'black']])

    assertEquals('ul.tags.with-links a:link, ul.tags.with-links a:visited {background: #005a87;color: #ffffff;} ul.tags.with-links a:hover {color: #005a87;background: #ffffff;} ul.tags.with-links li.tag-0 a:link, ul.tags.with-links li.tag-0 a:visited {background: red; color: black;} ul.tags.with-links li.tag-0 a:hover { color: red; background: black;}',
                 tagsService.tagsCss)

    assertNull(tagsService.getTagCssClass('foo'))
    assertEquals('tag-0', tagsService.getTagCssClass('a:1'))
  }

  public void testDefinitionWithDefaults()
  {
    TagsService tagsService = new TagsService(['__default__': [background: 'yellow', color: 'blue'],
                                              'a:1': [background: 'red', color: 'black']])

    assertEquals('ul.tags.with-links a:link, ul.tags.with-links a:visited {background: yellow;color: blue;} ul.tags.with-links a:hover {color: yellow;background: blue;} ul.tags.with-links li.tag-0 a:link, ul.tags.with-links li.tag-0 a:visited {background: red; color: black;} ul.tags.with-links li.tag-0 a:hover { color: red; background: black;}',
                 tagsService.tagsCss)

    assertNull(tagsService.getTagCssClass('foo'))
    assertEquals('tag-0', tagsService.getTagCssClass('a:1'))
  }
}
