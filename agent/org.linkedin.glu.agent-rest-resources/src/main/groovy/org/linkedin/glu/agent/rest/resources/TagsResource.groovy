/*
 * Copyright (c) 2011-2013 Yan Pujante
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

package org.linkedin.glu.agent.rest.resources

import org.linkedin.glu.utils.tags.TagsSerializer
import org.linkedin.util.io.PathUtils
import org.restlet.data.Method
import org.restlet.data.Status
import org.restlet.representation.Representation
import org.restlet.resource.Delete
import org.restlet.resource.Get
import org.restlet.resource.Post
import org.restlet.resource.Put

/**
 * @author yan@pongasoft.com */
public class TagsResource extends BaseResource
{
  public static final TagsSerializer TAGS_SERIALIZER = TagsSerializer.INSTANCE

  /**
   * GET:  /tags => 200: json array of tags
   * HEAD: /tags => 200 if has tags, 204 otherwise (only match=any makes sense here...)
   * HEAD: /tags/fruit;vegetable?match=all => 200 if matches, 204 if does not match
   * query string can contain: match=any (default if missing) or match=all for matching on all
   * of them
   */
  @Get
  public Representation getOrCheckTags()
  {
    noException {
      if(request.getMethod() == Method.HEAD)
      {
        String match = request.originalRef.queryAsForm?.getValues('match') ?: 'any'

        Collection<String> tagsToCheck = tags

        Boolean matchResult = null

        switch(match)
        {
          case 'any':
            if(tagsToCheck)
              matchResult = agent.hasAnyTag(tagsToCheck)
            else
              matchResult = agent.hasTags()
            break

          case 'all':
            matchResult = agent.hasAllTags(tagsToCheck)
            break;

          default:
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "unknown match ${match}")
            break
        }

        if(matchResult != null)
        {
          if(matchResult)
            response.setStatus(Status.SUCCESS_OK)
          else
            response.setStatus(Status.SUCCESS_NO_CONTENT)
        }

        return null
      }
      else
      {
        return toRepresentation(agent.getTags())
      }
    }
  }

  /**
   * set tags (ex: PUT /tags/fruit;vegetable)
   */
  @Put
  public Representation updateTags(Representation representation)
  {
    noException {
      agent.setTags(tags)
      return null
    }
  }

  /**
   * add tags (ex: POST /tags/fruit;vegetable)
   */
  @Post
  public Representation addTags(Representation representation)
  {
    noException {
      toRepresentation(agent.addTags(tags))
    }
  }

  /**
   * DELETE remove tags (ex: DELETE /tags/fruit;vegetable)
   */
  @Delete
  public Representation deleteTags()
  {
    noException {
      toRepresentation(agent.removeTags(tags))
    }
  }

  private Collection<String> getTags()
  {
    return TAGS_SERIALIZER.deserialize(PathUtils.removeLeadingSlash(path) ?: '')
  }
}