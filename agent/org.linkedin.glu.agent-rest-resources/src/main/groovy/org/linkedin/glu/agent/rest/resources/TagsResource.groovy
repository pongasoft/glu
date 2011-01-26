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

package org.linkedin.glu.agent.rest.resources

import org.restlet.Response
import org.restlet.Context
import org.restlet.Request
import org.restlet.representation.Representation
import org.restlet.representation.Variant
import org.linkedin.util.io.PathUtils
import org.linkedin.util.text.StringSplitter
import org.restlet.data.Method
import org.restlet.representation.EmptyRepresentation
import org.restlet.data.Status

/**
 * @author yan@pongasoft.com */
public class TagsResource extends BaseResource
{
  public static final StringSplitter STRING_SPLITTER = new StringSplitter(';' as char)

  TagsResource(Context context, Request request, Response response)
  {
    super(context, request, response);
  }

  public boolean allowPut()
  {
    return true
  }

  public boolean allowPost()
  {
    return true
  }

  public boolean allowDelete()
  {
    return true
  }

  public boolean allowGet()
  {
    return true
  }

  /**
   * GET:  /tags => 200: json array of tags
   * HEAD: /tags => 200 if has tags, 204 otherwise (only match=any makes sense here...)
   * HEAD: /tags/fruit;vegetable?match=all => 200 if matches, 204 if does not match
   * query string can contain: match=any (default if missing) or match=all for matching on all
   * of them
   */
  public Representation represent(Variant variant)
  {
    return noException {
      if(request.getMethod() == Method.HEAD)
      {
        Representation res = new EmptyRepresentation()

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

        return res
      }
      else
      {
        return toRepresentation(agent.getTags())
      }
    }
  }

  /**
   * PUT set tags (ex: PUT /tags/fruit;vegetable)
   */
  public void storeRepresentation(Representation representation)
  {
    noException {
      agent.setTags(tags)
    }
  }

  /**
   * POST add tags (ex: POST /tags/fruit;vegetable)
   */
  public void acceptRepresentation(Representation representation)
  {
    noException {
      response.setEntity(toRepresentation(agent.addTags(tags)))
    }
  }

  /**
   * DELETE remove tags (ex: DELETE /tags/fruit;vegetable)
   */
  public void removeRepresentations()
  {
    noException {
      response.setEntity(toRepresentation(agent.removeTags(tags)))
    }
  }


  private Collection<String> getTags()
  {
    return STRING_SPLITTER.splitAsList(PathUtils.removeLeadingSlash(path) ?: '')
  }
}